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

/// This view shows the textual header information for a ModelData.Task.
struct TaskHeader: View {
  let taskInfo: Manifest.Task

  var body: some View {
    // If the Task has a contact name and description, first line will be contact name and second
    // line is the waypoint description (aka address). If there's no contact name, the description
    // is the first line and second line is nil.
    let firstLine = taskInfo.contactName ?? taskInfo.plannedWaypoint.description
    let secondLine = (taskInfo.contactName != nil) ? taskInfo.plannedWaypoint.description : nil

    VStack(alignment: .leading) {
      Text(firstLine)
        .font(.system(size: 14))
        .fontWeight(.semibold)
        .padding(EdgeInsets(top: 0, leading: 0, bottom: 5, trailing: 0))
        .lineLimit(nil)
        // Workaround for text-wrapping issue in SwiftUI. See
          // https://developer.apple.com/documentation/swiftui/view/fixedsize(horizontal:vertical:)
        .fixedSize(horizontal: false, vertical: true)
      if let secondLine = secondLine {
        Text(secondLine)
          .font(.system(size: 14))
          .lineLimit(nil)
          // Workaround for text-wrapping issue in SwiftUI. See
          // https://developer.apple.com/documentation/swiftui/view/fixedsize(horizontal:vertical:)
          .fixedSize(horizontal: false, vertical: true)
      }
      HStack(spacing: 1) {
        Text("ID: ")
          .fontWeight(.semibold)
        Text(taskInfo.taskId)
        Spacer()
      }
      .font(.system(size: 14))
    }
  }
}

struct TaskHeader_Previews: PreviewProvider {
  static var previews: some View {
    let _ = LMFSDriverSampleApp.googleMapsInited
    let modelData = ModelData(filename: "test_manifest")
    let stop = modelData.stops[0]
    let task = modelData.tasks(stop: stop)[0]
    TaskHeader(taskInfo: task.taskInfo)
      .environmentObject(modelData)
      .previewLayout(.fixed(width: 320, height: 100))
  }
}
