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

/// This view provides the set of task buttons and actions for a task, based on the task status.
struct TaskButtons: View {
  @EnvironmentObject var modelData: ModelData
  @ObservedObject var task: ModelData.Task

  var body: some View {
    HStack {
      switch task.outcome {

      case .pending:
        Button(
          action: {
            modelData.setTaskStatus(task: task, newStatus: .completed)
          },
          label: {
            Text(task.actionName)
              .foregroundColor(.white)
              .padding(EdgeInsets(top: 0, leading: 10, bottom: 0, trailing: 10))
          }
        )
        .background(Color.blue)
        .cornerRadius(3)
        .buttonStyle(.bordered)

        Button(
          action: {
            modelData.setTaskStatus(task: task, newStatus: .couldNotComplete)
          },
          label: {
            Text(task.unsuccessfulActionName)
              .foregroundColor(.blue)
          }
        )
        .buttonStyle(.borderless)

      case .completed:
        Text("Completed")
          .foregroundColor(.gray)
          .padding(EdgeInsets(top: 5, leading: 0, bottom: 0, trailing: 0))

      case .couldNotComplete:
        Text("Could not complete")
          .foregroundColor(.gray)

      }

      Spacer()
    }
  }
}

struct TaskButtons_Previews: PreviewProvider {
  private static func completedTask(
    modelData: ModelData,
    task: ModelData.Task
  ) -> ModelData.Task {
    modelData.setTaskStatus(task: task, newStatus: .completed)
    return task
  }

  static var previews: some View {
    let _ = LMFSDriverSampleApp.googleMapsInited
    let modelData = ModelData(filename: "test_manifest")
    let stop1 = modelData.stops[0]
    let task1 = modelData.tasks(stop: stop1)[0]
    TaskButtons(task: task1)
      .environmentObject(modelData)
      .previewLayout(.sizeThatFits)

    let stop2 = modelData.stops[1]
    let task2 = modelData.tasks(stop: stop2)[0]
    TaskButtons(task: task2)
      .environmentObject(modelData)
      .previewLayout(.sizeThatFits)

    let stop3 = modelData.stops[0]
    let task3 = modelData.tasks(stop: stop3)[2]
    TaskButtons(
      task: completedTask(
        modelData: modelData,
        task: task3)
    )
    .environmentObject(modelData)
    .previewLayout(.sizeThatFits)
  }
}
