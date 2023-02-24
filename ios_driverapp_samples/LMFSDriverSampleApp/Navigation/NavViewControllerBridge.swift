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

import GoogleMaps
import GoogleNavigation
import SwiftUI

/// The SwiftUI view which wraps the NavViewController UIViewController.
struct NavViewControllerBridge: UIViewControllerRepresentable {
  /// The instance of location manager to use for request Always permission.
  private let locationManager: CLLocationManager

  /// The ModelData containing the primary state of the application.
  @EnvironmentObject var modelData: ModelData

  /// Initializes this SwiftUI view.
  init(locationManager: CLLocationManager) {
    self.locationManager = locationManager
  }

  /// Lifecycle method for UIViewControllerRepresentable which creates the UIViewController.
  func makeUIViewController(context: Context) -> NavViewController {
    let uiViewController = NavViewController(
      modelData: modelData,
      locationManager: locationManager)
    uiViewController.mapView?.delegate = context.coordinator
    uiViewController.mapView?.navigator?.add(context.coordinator)
    return uiViewController
  }

  /// Lifecycle method for UIViewControllerRepresentable which updates the UIViewController.
  ///
  /// This particular implementation doesn't need any logic here.
  func updateUIViewController(_ uiViewController: NavViewController, context: Context) {
    NSLog("In updateUIViewController")
  }

  /// Lifecycle method for UIViewControllerRepresentable which creates the coordinate object that
  /// allows changes from the contained UIViewController to flow back into the SwiftUI application.
  func makeCoordinator() -> NavViewCoordinator {
    return NavViewCoordinator(self)
  }

  /// Contained class to define the coordinator.
  final class NavViewCoordinator: NSObject, GMSMapViewDelegate, GMSNavigatorListener {
    /// Points to the containing NavViewControllerBridge instance.
    var navViewControllerBridge: NavViewControllerBridge

    /// Iniitalizes the coordinator.
    init(_ navViewControllerBridge: NavViewControllerBridge) {
      self.navViewControllerBridge = navViewControllerBridge
    }

    /// Callback method from GMSNavigator for arriving at the destination.
    func navigator(_ navigator: GMSNavigator, didArriveAt waypoint: GMSNavigationWaypoint) {
      navViewControllerBridge.modelData.navigationStatus = .notStarted
    }
  }
}
