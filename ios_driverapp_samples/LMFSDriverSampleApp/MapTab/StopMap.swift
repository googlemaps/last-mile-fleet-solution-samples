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
import SwiftUI
import UIKit

/// This view that the user sees when tapping on the map tab.
struct StopMap: View {
  @EnvironmentObject var modelData: ModelData
  @State var selectedMarker: GMSMarker?
  @State var markers: [GMSMarker] = []
  @State private var zoom: Float = 15
  @State var currentPage: Int = 0

  var body: some View {
    NavigationView {
      ZStack(alignment: .bottom) {
        MapViewControllerBridge(
          markers: $markers,
          selectedMarker: $selectedMarker,
          zoom: $zoom
        )
        .frame(maxWidth: .infinity, alignment: .leading)
        .onAppear {
          markers = StopMap.buildMarkers(modelData: modelData)
        }

        PageViewController(
          pages: modelData.stops.map({
            StopPage(stop: $0).environmentObject(modelData)
          }),
          currentPage: $currentPage
        )
        .frame(height: 120)
        .padding(EdgeInsets(top: 0, leading: 0, bottom: 20, trailing: 0))
      }
      .navigationTitle(Text("Your itinerary"))
      .onChange(of: selectedMarker) { newSelectedMarker in
        if let stopId = newSelectedMarker?.userData as? String {
          if let stopIndex = modelData.stops.firstIndex(where: { $0.stopInfo.stopId == stopId }) {
            currentPage = stopIndex
          }
        }
      }
    }
  }

  func mapViewControllerBridge(
    _ bridge: MapViewControllerBridge,
    didTapOnMarker marker: GMSMarker
  ) {
    if let stopId = marker.userData as? String,
      let stopIndex = modelData.stops.firstIndex(where: { $0.stopInfo.stopId == stopId })
    {
      currentPage = stopIndex
    }
  }

  private static func buildMarkers(modelData: ModelData) -> [GMSMarker] {
    return modelData.stops.map({ CustomMarker.makeMarker(stop: $0) })
  }

  init() {
    // Ensure Google Maps SDK is initialized.
    let _ = LMFSDriverSampleApp.googleMapsInited
  }
}

struct StopMap_Previews: PreviewProvider {
  static var previews: some View {
    let _ = LMFSDriverSampleApp.googleMapsInited
    StopMap()
      .environmentObject(ModelData(filename: "test_manifest"))
  }
}
