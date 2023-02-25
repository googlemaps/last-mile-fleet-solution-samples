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
import SwiftUI

let addressBarBlue = Color(.sRGB, red: 232 / 255, green: 240 / 255, blue: 252.0 / 255)

struct ContentView: View {
  @State private var selection: Tab = .settings
  @EnvironmentObject var modelData: ModelData

  enum Tab {
    case list
    case map
    case settings
  }

  var body: some View {
    // Since we want navigation to be full-screen, we have to make a switch here at the top
    // level of the application based on whether we're navigating or not.
    if modelData.navigationStatus == .inProgress {
      VStack {
        NavViewControllerBridge(
          locationManager: CLLocationManager())

        // Status bar indicating address and number of tasks associated with the upcoming stop.
        VStack {
          Text(
            modelData.navigationState?.upcomingStop.stopInfo.plannedWaypoint.description
              ?? "Navigation in progress"
          )
          .frame(maxWidth: .infinity)
          .padding(.bottom, 5)
          Text(tasksMessage())
            .frame(maxWidth: .infinity)
        }
        .padding(EdgeInsets(top: 20, leading: 0, bottom: 20, trailing: 0))
        .background(addressBarBlue)
      }
    } else {
      TabView(selection: $selection) {

        StopList()
          .tabItem {
            Label("List", systemImage: "list.bullet")
          }
          .tag(Tab.list)
        StopMap()
          .tabItem {
            Label("Map", systemImage: "map")
          }
          .tag(Tab.map)

        Settings()
          .tabItem {
            Label("Settings", systemImage: "gearshape")
              .tag(Tab.settings)
          }
      }
      .edgesIgnoringSafeArea(.all)
      .environmentObject(modelData)
      .environmentObject(modelData.networkServiceManager)
      .environmentObject(modelData.accessTokenProvider)
    }
  }

  private func tasksMessage() -> String {
    let taskTypes = [Manifest.TaskType.DELIVERY, Manifest.TaskType.PICKUP]
    let nonZeroTaskTypes = taskTypes.filter { modelData.upcomingTaskCount(type: $0) > 0 }
    let taskTypeMessages = nonZeroTaskTypes.map {
      String.localizedStringWithFormat(
        NSLocalizedString($0.rawValue, comment: ""),
        modelData.upcomingTaskCount(type: $0) as CVarArg)
    }
    let tasksMessage = taskTypeMessages.joined(separator: ", ")
    return tasksMessage
  }
}

extension ModelData {
  fileprivate func upcomingTaskCount(type: Manifest.TaskType) -> Int {
    return self.navigationState?.upcomingStop.taskCount(taskType: type) ?? 0
  }
}

struct ContentView_Previews: PreviewProvider {
  static var previews: some View {
    let _ = LMFSDriverSampleApp.googleMapsInited
    ContentView()
      .environmentObject(ModelData(filename: "test_manifest"))
  }
}
