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

import SwiftUI

/// This view shows a list of stops that the vehicle will navigate to in order. Each stop is
/// represented by a `StopListRow`.
struct StopList: View {
  @EnvironmentObject var modelData: ModelData
  @State private var editMode = EditMode.inactive

  var body: some View {
    NavigationView {
      VStack(alignment: .leading) {
        List {
          ForEach(modelData.stops) { stop in
            StopListRow(stop: stop)
              .moveDisabled(stop.taskStatus != .pending)
          }
          .onMove { source, destination in
            modelData.moveStops(source: source, destination: destination)
          }
        }
        .environment(\.editMode, $editMode)

        VStack(alignment: .leading) {
          HStack {
            Text("ID: \(modelData.manifest.vehicle.vehicleId)")
            Spacer()
            Button(action: {
                        UIPasteboard.general.string = modelData.manifest.vehicle.vehicleId
                    }) {
                        Image(systemName: "doc.on.doc")
                    }
          }
          Text("Total tasks: \(modelData.manifest.tasks.count)")
        }
        .foregroundColor(.gray)
        .padding(EdgeInsets(top: 0, leading: 20, bottom: 10, trailing: 20))
      }
      .navigationTitle("Your itinerary")
      .toolbar {
        ToolbarItem(placement: .navigationBarTrailing) {
          Button(editMode == .inactive ? "Reorder Stops" : "Done") {
            editMode = (editMode == .inactive ? .active : .inactive)
          }
        }
      }
    }
    .navigationViewStyle(StackNavigationViewStyle())
    .navigationBarTitleDisplayMode(.inline)
    .ignoresSafeArea(edges: .top)
  }
}

struct StopList_Previews: PreviewProvider {
  static var previews: some View {
    let _ = LMFSDriverSampleApp.googleMapsInited
    StopList()
      .environmentObject(ModelData(filename: "test_manifest"))

  }
}
