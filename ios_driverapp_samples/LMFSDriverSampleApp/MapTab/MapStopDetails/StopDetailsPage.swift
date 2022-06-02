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

/// This view defines the page we push to when the user taps on the
/// Details button for a stop in the map view.
struct StopDetailsPage: View {
  @EnvironmentObject var modelData: ModelData
  var stop: ModelData.Stop
  @State var markers: [GMSMarker]
  @State private var zoom: Float = 15

  var body: some View {
    VStack {
      MapViewControllerBridge(
        markers: $markers,
        selectedMarker: .constant(nil),
        zoom: $zoom
      )
      .frame(height: 250)

      ForEach(modelData.tasks(stop: stop)) { task in
        StopDetailsPageTask(stop: stop, task: task)
      }
      .padding()

      Spacer()
    }
    .navigationTitle(stop.stopInfo.plannedWaypoint.description)
    .navigationBarTitleDisplayMode(.inline)
  }

  init(stop: ModelData.Stop) {
    // Ensure Google Maps SDK is initialized.
    let _ = LMFSDriverSampleApp.googleMapsInited

    self.stop = stop
    self.markers =
      stop.tasks.map({ CustomMarker.makeMarker(task: $0) }) + [
        CustomMarker.makeMarker(stop: stop, showLabel: false)
      ]
  }
}

struct StopDetailsPage_Previews: PreviewProvider {
  static var previews: some View {
    let _ = LMFSDriverSampleApp.googleMapsInited
    let modelData = ModelData(filename: "test_manifest")
    StopDetailsPage(stop: modelData.stops[0])
      .environmentObject(modelData)
  }
}
