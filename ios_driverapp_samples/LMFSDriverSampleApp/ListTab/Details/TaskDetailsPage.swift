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

/// This view defines the page we push to when the user taps on the arrow button
/// for a task in the stop list.
struct TaskDetailsPage: View {
  @ObservedObject var task: ModelData.Task
  @State var selectedMarker: GMSMarker?
  @State var markers: [GMSMarker]
  @State private var zoom: Float = 15

  var body: some View {
    VStack {
      MapViewControllerBridge(
        markers: $markers,
        selectedMarker: $selectedMarker,
        zoom: $zoom
      )
      .frame(height: 250)

      HStack {
        Spacer(minLength: 60)

        TaskDetails(task: task)
      }

      Spacer()
    }
    .navigationTitle(task.taskInfo.plannedWaypoint.description)
    .navigationBarTitleDisplayMode(.inline)
  }

  init(task: ModelData.Task, stop: ModelData.Stop) {
    // Ensure Google Maps SDK is initialized.
    let _ = LMFSDriverSampleApp.googleMapsInited
    self.task = task
    self.markers = [
      CustomMarker.makeMarker(task: task, showLabel: false),
      CustomMarker.makeMarker(stop: stop, showLabel: false),
    ]
    self.selectedMarker = nil
  }
}

struct TaskDetailsPage_Previews: PreviewProvider {
  static var previews: some View {
    let _ = LMFSDriverSampleApp.googleMapsInited
    let modelData = ModelData(filename: "test_manifest")
    let stop = modelData.stops[0]
    TaskDetailsPage(task: modelData.tasks(stop: stop)[0], stop: stop)
  }
}
