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

/// This view is a row which displays the address of a stop.
struct StopListRow: View {
  @ObservedObject var stop: ModelData.Stop
  @EnvironmentObject var modelData: ModelData
  @State private var expanded = false
  @State private var openDetails: Bool = false
  @State private var openNav: Bool = false

  var body: some View {
    DisclosureGroup(isExpanded: $expanded) {
      let content = VStack(alignment: .leading) {
        HStack {
          // The details button
          Button {
            openDetails = true
          } label: {
            Text("Details")
              .foregroundColor(Color.blue)
              .padding(EdgeInsets(top: 0, leading: 10, bottom: 0, trailing: 10))
              .font(.system(size: 14))
          }
          .background(Color.white)
          .cornerRadius(2)

          if stop == modelData.navigationState?.upcomingStop {
            // The navigation button
            Button {
              modelData.navigationStatus = .inProgress
            } label: {
              Text("Navigate")
                .foregroundColor(.white)
                .padding(EdgeInsets(top: 0, leading: 15, bottom: 0, trailing: 10))
                .font(.system(size: 14))
            }
            .background(Color.blue)
            .cornerRadius(3)
          }

          Spacer()

          // Use an invisble NavigationLink so we don't automatically get the disclosure chevron.
          // These are at the end so they don't otherwise affect layout.

          NavigationLink(
            destination: StopDetailsPage(stop: stop),
            isActive: $openDetails
          ) {
            EmptyView()
          }
          .frame(width: 0)
          .opacity(0)
          .hidden()
        }

        ForEach(modelData.tasks(stop: stop)) { task in
          StopDetailsPageTask(stop: stop, task: task)
        }
      }
      .padding(EdgeInsets(top: -5, leading: 0, bottom: 0, trailing: 0))

      content.listRowSeparator(.hidden, edges: VerticalEdge.Set.top)
        .buttonStyle(.bordered)
    } label: {
      StopHeader(stop: stop)
        .font(.system(size: 20))
    }
    .foregroundColor(.primary)
  }
}

struct TaskRow_Previews: PreviewProvider {
  static var previews: some View {
    let _ = LMFSDriverSampleApp.googleMapsInited
    let modelData = ModelData(filename: "test_manifest")
    Group {
      StopListRow(stop: modelData.stops[0])
      StopListRow(stop: modelData.stops[1])
    }
    .environmentObject(modelData)
    .previewLayout(.fixed(width: 300, height: 70))
  }
}
