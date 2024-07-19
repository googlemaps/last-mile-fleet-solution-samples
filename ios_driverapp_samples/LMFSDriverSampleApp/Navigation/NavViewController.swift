/*
 * Copyright 2022 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

import CoreLocation
import GoogleMaps
import GoogleNavigation
import GoogleRidesharingDriver
import SwiftUI
import UIKit

class NavViewController: UIViewController {
  /// The instance of locationManager we should use for requesting always permission.
  private let locationManager: CLLocationManager

  /// The main repository of state for the app.
  ///
  /// Most classes in this app get access to this as a @EnvironmentObject, but since this is a UIKit
  /// class rather than a SwiftUI class, it needs to have the ModelData object explicitly passed to
  /// in.
  @ObservedObject var modelData: ModelData

  /// Array of speed multipliers to offer when simulating progress along a route.
  private static let speedOptions: [Int] = [1, 2, 4, 8, 16]

  /// Wrapper around the application preference for location simulation.
  private let disableLocationSimulation = ApplicationDefaults.disableLocationSimulation

  /// The delivery driver API object for this navigation session.
  ///
  /// This should exist once navigation has started.
  private var deliveryDriverAPI: GMTDDeliveryDriverAPI?

  /// Returns this view controller's map view, if it has been initialized already.
  var mapView: GMSMapView? {
    return self.view as? GMSMapView
  }

  /// Initializes a NavViewController.
  init(modelData: ModelData, locationManager: CLLocationManager) {
    self.modelData = modelData
    self.locationManager = locationManager
    super.init(nibName: nil, bundle: nil)
  }

  /// Not implemented yet.
  required init?(coder: NSCoder) {
    return nil
  }

  /// Standard lifecycle method for a UIViewController.
  override func loadView() {
    locationManager.requestAlwaysAuthorization()

    let simulatingLocation = !disableLocationSimulation.value

    // Try and get the camera to the right initial location. This doesn't work perfectly because
    // we're initializing the map view with zero rect, but it will at least get the camera close
    // to the point navigation will start from.
    guard let startingLocation = modelData.navigationState?.startingLocation else {
      fatalError("navigationState.startingLocation is nil in loadView()!")
    }
    let camera = GMSCameraPosition.camera(
      withLatitude: startingLocation.latitude,
      longitude: startingLocation.longitude,
      zoom: 14)
    let uiView = GMSMapView.map(withFrame: CGRect.zero, camera: camera)

    // Show the terms and conditions.
    let companyName = "LMFS Sample Co."
    GMSNavigationServices.showTermsAndConditionsDialogIfNeeded(
      withCompanyName: companyName
    ) { termsAccepted in
      if termsAccepted {
        // Now we can turn on navigation.
        uiView.isNavigationEnabled = true
        uiView.settings.compassButton = true

        // Request authorization for alert notifications which deliver guidance instructions
        // in the background.
        UNUserNotificationCenter.current().requestAuthorization(
          options: [.alert]) { granted, error in
            NSLog(
              "Notification authorization granted \(granted) ",
              "error \(String(describing: error)).")
          }

        // Set the destination to the upcoming stop.
        guard
          let plannedWaypoint = self.modelData.navigationState?.upcomingStop.stopInfo
            .plannedWaypoint
        else {
          fatalError("navigationState.upcomingStop is nil in loadView()!")
        }

        guard
          let destination =
            GMSNavigationWaypoint(
              location: plannedWaypoint.coordinate,
              title: plannedWaypoint.description)
        else {
          fatalError("destination in loadView() cannot be successfully generated!")
        }
        let destinations = [destination]
        if simulatingLocation {
          // This is important to get the initial location correct when simulating location.
          uiView.locationSimulator?.simulateLocation(at: startingLocation)
        }
        uiView.navigator?.setDestinations(
          destinations,
          callback: { routeStatus in
            uiView.navigator?.isGuidanceActive = true
            uiView.navigator?.sendsBackgroundNotifications = true
            self.initializeDeliveryDriverAPI(uiView)
            if simulatingLocation {
              uiView.locationSimulator?.simulateLocationsAlongExistingRoute()
            }
            uiView.cameraMode = .following

            if simulatingLocation {
              // Create and add a simulation speed control.
              let speedOptionLabels = NavViewController.speedOptions.map { i in "\(i)x" }
              let simulationSpeedController = UISegmentedControl(items: speedOptionLabels)
              simulationSpeedController.addTarget(
                self,
                action: #selector(self.simulatedSpeedChanged(_:)),
                for: .valueChanged)
              uiView.addSubview(simulationSpeedController)
              simulationSpeedController.translatesAutoresizingMaskIntoConstraints = false
              simulationSpeedController.bottomAnchor.constraint(
                equalTo: uiView.navigationFooterLayoutGuide.topAnchor, constant: -10
              ).isActive = true
              simulationSpeedController.centerXAnchor.constraint(
                equalTo: uiView.centerXAnchor
              ).isActive = true
            }

            uiView.roadSnappedLocationProvider?.allowsBackgroundLocationUpdates = true
            uiView.roadSnappedLocationProvider?.startUpdatingLocation()
            if let vehicleReporter = self.deliveryDriverAPI?.vehicleReporter {
              uiView.roadSnappedLocationProvider?.add(vehicleReporter)
            }

            // Configure and add an exit button (there's no way out of navigation by default).
            var configuration = UIButton.Configuration.filled()
            configuration.title = "Exit"
            configuration.contentInsets =
                NSDirectionalEdgeInsets(top: 10, leading: 15, bottom: 10, trailing: 15)
            configuration.baseBackgroundColor = .white
            // This references the default tintColor in order to get Apple's default button blue,
            // which is not otherwise available programmatically.
            configuration.baseForegroundColor = uiView.tintColor
            configuration.background.cornerRadius = 10
            configuration.titleTextAttributesTransformer =
                UIConfigurationTextAttributesTransformer { incoming in
                  var outgoing = incoming
                  outgoing.font = .systemFont(ofSize: 18)
                  return incoming
                }
            let exitButton = UIButton(configuration: configuration)
            // Drop shadows cannot be controlled via UIButton.Configuration.
            exitButton.layer.shadowOffset = CGSize(width: 2, height: 2)
            exitButton.layer.shadowColor = CGColor(gray: 0, alpha: 0.8)
            exitButton.layer.shadowRadius = 5
            exitButton.layer.shadowOpacity = 0.6
            exitButton.addTarget(
              self, action: #selector(self.exitNavigation),
              for: .primaryActionTriggered)
            uiView.addSubview(exitButton)
            exitButton.translatesAutoresizingMaskIntoConstraints = false
            exitButton.bottomAnchor.constraint(
              equalTo: uiView.navigationFooterLayoutGuide.topAnchor, constant: -15
            ).isActive = true
            exitButton.trailingAnchor.constraint(
              equalTo: uiView.trailingAnchor, constant: -15
            ).isActive = true
          })
      } else {
        // We need to kick out of navigation if the user refused the disclosure, since otherwise
        // the user will be trapped in the navigation screen.
        self.modelData.navigationStatus = .aborted
      }
    }

    self.view = uiView
  }

  /// The callback from the segmented control for simulated driving.
  @objc
  func simulatedSpeedChanged(_ sender: UISegmentedControl) {
    let selectedSegmentIndex = sender.selectedSegmentIndex
    if selectedSegmentIndex != UISegmentedControl.noSegment {
      let newSpeedMultiplier = NavViewController.speedOptions[selectedSegmentIndex]
      mapView?.locationSimulator?.speedMultiplier = Float(newSpeedMultiplier)
    }
  }

  /// The callback for the exit-navigation button.
  @objc
  func exitNavigation() {
    modelData.navigationStatus = .aborted
  }

  /// Helper method to create the delivery driver API.
  ///
  /// After this method returns, the private var deliveryDriverAPI will be non-nil.
  private func initializeDeliveryDriverAPI(_ mapView: GMSMapView) {
    let context = GMTDDriverContext(
      accessTokenProvider: modelData.accessTokenProvider,
      providerID: modelData.manifest.vehicle.providerId,
      vehicleID: modelData.manifest.vehicle.vehicleId,
      navigator: mapView.navigator!)
    deliveryDriverAPI = GMTDDeliveryDriverAPI(driverContext: context)
    deliveryDriverAPI?.vehicleReporter.add(modelData.vehicleReporterListener)

    deliveryDriverAPI?.vehicleReporter.locationTrackingEnabled =
      !ApplicationDefaults.disableLocationReporting.value
  }
}
