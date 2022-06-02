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

struct MapViewControllerBridge: UIViewControllerRepresentable {
  @Binding var markers: [GMSMarker]
  @Binding var selectedMarker: GMSMarker?
  @Binding var zoom: Float
  @EnvironmentObject private var modelData: ModelData

  func makeUIViewController(context: Context) -> MapViewController {
    let uiViewController = MapViewController()
    uiViewController.map.delegate = context.coordinator
    return uiViewController
  }

  func updateUIViewController(_ uiViewController: MapViewController, context: Context) {
    markers.forEach { $0.map = uiViewController.map }
    selectedMarker?.map = uiViewController.map
    animateToUpdatedMarkers(in: uiViewController.map)
  }

  func makeCoordinator() -> MapViewCoordinator {
    return MapViewCoordinator(self)
  }

  private func animateToUpdatedMarkers(in map: GMSMapView) {
    if let selectedMarker = selectedMarker, map.selectedMarker != selectedMarker {
      map.selectedMarker = selectedMarker
      let camera = GMSCameraPosition(
        latitude: selectedMarker.position.latitude,
        longitude: selectedMarker.position.longitude,
        zoom: zoom)
      DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
        CATransaction.begin()
        CATransaction.setAnimationDuration(0.1)
        map.animate(with: GMSCameraUpdate.setCamera(camera))
        CATransaction.commit()
      }
    } else if markers.count > 0 {
      let bounds = markers.reduce(
        GMSCoordinateBounds()
      ) { partialResult, marker in
        partialResult.includingCoordinate(marker.position)
      }
      DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
        CATransaction.begin()
        CATransaction.setAnimationDuration(0.1)
        /// Later increase the bottom edge inset for the paging overlay view.
        let insets = UIEdgeInsets(top: 100, left: 100, bottom: 100, right: 100)
        map.animate(with: GMSCameraUpdate.fit(bounds, with: insets))
        CATransaction.commit()
      }
    }
  }

  final class MapViewCoordinator: NSObject, GMSMapViewDelegate {
    let mapViewControllerBridge: MapViewControllerBridge

    init(_ mapViewControllerBridge: MapViewControllerBridge) {
      self.mapViewControllerBridge = mapViewControllerBridge
    }

    func mapView(_ mapView: GMSMapView, didTap marker: GMSMarker) -> Bool {
      let bridge = mapViewControllerBridge
      bridge.selectedMarker = marker
      /// Although we did handle this tap, we also want the map to carry out its default behavior
      /// to center the map on the tapped pin, so return false.
      return false
    }
  }
}
