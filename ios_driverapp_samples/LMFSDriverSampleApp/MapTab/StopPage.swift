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

/// A view that displays the information of a stop and can be swiped from StopMap to change stops.
struct StopPage: View {
  @EnvironmentObject var modelData: ModelData
  @ObservedObject var stop: ModelData.Stop
  @State private var openDetails = false

  var body: some View {
    VStack {
      HStack {
        StopHeader(stop: stop)

        Spacer()
      }
      HStack {
        Spacer()

        // Use an invisible navigation link so the disclosure chevron is not automatically
        // added. It's placed here next to the spacer so that it doesn't affect layout.
        NavigationLink(
          destination: StopDetailsPage(stop: stop),
          isActive: $openDetails
        ) {
          // Use an empty navigation link to prevent the expansion chevron from showing.
          EmptyView()
        }

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
        .buttonStyle(.bordered)

        if stop == modelData.navigationState?.upcomingStop {
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
          .buttonStyle(.bordered)
        }
      }
    }
    .frame(width: 280, height: 100)
    .padding(EdgeInsets(top: 0, leading: 0, bottom: 10, trailing: 20))
    .background(
      RoundedRectangle(cornerRadius: 10)
        .fill(Color.white)
        .shadow(color: .gray, radius: 2, x: 1, y: 2))
  }
}

struct StopPage_Previews: PreviewProvider {
  static var previews: some View {
    let _ = LMFSDriverSampleApp.googleMapsInited
    let modelData = ModelData(filename: "test_manifest")
    StopPage(stop: modelData.stops[0])
      .previewLayout(.sizeThatFits)
      .environmentObject(modelData)
    StopPage(stop: modelData.stops[1])
      .previewLayout(.sizeThatFits)
      .environmentObject(modelData)
  }
}
