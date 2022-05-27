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

struct TaskStatusImage: View {
  @ObservedObject var task: ModelData.Task

  var body: some View {
    let isDelivery = task.taskInfo.taskType == .DELIVERY
    let taskLetter = task.sequenceString.lowercased()
    let foregroundColor =
      isDelivery ? .red : Color(.sRGB, red: 1.0, green: 0.7, blue: 0.7, opacity: 1)
    Image(systemName: "\(taskLetter).circle.fill")
      .foregroundColor(foregroundColor)
      .frame(width: 30, height: 30)
  }
}

struct TaskStatusImage_Previews: PreviewProvider {
  static var previews: some View {
    let _ = LMFSDriverSampleApp.googleMapsInited
    let modelData = ModelData(filename: "test_manifest")
    TaskStatusImage(task: modelData.tasks(stop: modelData.stops[0])[0])
      .previewLayout(.fixed(width: 70, height: 70))
    TaskStatusImage(task: modelData.tasks(stop: modelData.stops[0])[1])
      .previewLayout(.fixed(width: 70, height: 70))
    TaskStatusImage(task: modelData.tasks(stop: modelData.stops[0])[2])
      .previewLayout(.fixed(width: 70, height: 70))
    TaskStatusImage(task: modelData.tasks(stop: modelData.stops[1])[0])
      .previewLayout(.fixed(width: 70, height: 70))
    TaskStatusImage(task: modelData.tasks(stop: modelData.stops[1])[1])
      .previewLayout(.fixed(width: 70, height: 70))
    TaskStatusImage(task: modelData.tasks(stop: modelData.stops[1])[2])
      .previewLayout(.fixed(width: 70, height: 70))
  }
}
