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

/// This view defines the details of a task, and will show up when the user taps on a stop on the
/// stop list page.
struct StopDetailsPageTask: View {
  @ObservedObject var stop: ModelData.Stop
  @ObservedObject var task: ModelData.Task
  @State private var openDetails = false

  var body: some View {
    HStack(alignment: .top) {
      TaskStatusImage(task: task)

      VStack(alignment: .leading) {
        HStack {
          TaskHeader(taskInfo: task.taskInfo)

          /// Use an empty navigation link to avoid showing the disclosure chevron.
          NavigationLink(
            destination: TaskDetailsPage(task: task, stop: stop),
            isActive: $openDetails
          ) {
            EmptyView()
          }
          .frame(width: 0)
          .opacity(0)
          .hidden()

          Button(
            action: {
              openDetails = true
            },
            label: {
              Image(systemName: "arrow.forward.circle")
            }
          )
          .background(Color.clear)
          .buttonStyle(.plain)
        }

        TaskButtons(task: task)
      }
      .padding(EdgeInsets(top: 5, leading: 0, bottom: 0, trailing: 0))
    }
  }
}

struct StopDetailsPageTask_Previews: PreviewProvider {
  static var previews: some View {
    let _ = LMFSDriverSampleApp.googleMapsInited
    let modelData = ModelData(filename: "test_manifest")
    let stop = modelData.stops[0]
    StopDetailsPageTask(stop: stop, task: stop.tasks[0])
      .previewLayout(.sizeThatFits)
  }
}
