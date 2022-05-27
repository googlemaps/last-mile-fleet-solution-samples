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

/// This view defines the details of a task, and buttons which let the driver to update the task
/// status.
struct TaskDetails: View {
  @ObservedObject var task: ModelData.Task
  @EnvironmentObject var modelData: ModelData

  var body: some View {
    let taskInfo = task.taskInfo
    VStack(alignment: HorizontalAlignment.leading) {
      if let contactName = taskInfo.contactName {
        Text(contactName)
          .fontWeight(.semibold)
          .padding(EdgeInsets(top: 0, leading: 0, bottom: 10, trailing: 0))
      }

      switch task.taskInfo.taskType {
      case .PICKUP:
        Text("Pick up")
          .fontWeight(.semibold)
      case .SCHEDULED_STOP:
        Text("Scheduled stop")
          .fontWeight(.semibold)
      case .DELIVERY, .UNAVAIALBLE:
        EmptyView()
      }

      Text("ID: \(taskInfo.taskId)")

      TaskButtons(task: task)
    }
  }
}

struct TaskDetails_Previews: PreviewProvider {
  static var previews: some View {
    let _ = LMFSDriverSampleApp.googleMapsInited
    let modelData = ModelData(filename: "test_manifest")
    let stop = modelData.stops[0]
    let task = modelData.tasks(stop: stop)[0]
    TaskDetails(task: task)
      .environmentObject(modelData)
      .previewLayout(.sizeThatFits)
  }
}
