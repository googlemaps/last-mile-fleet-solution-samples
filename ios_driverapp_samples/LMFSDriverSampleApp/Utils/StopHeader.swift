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

struct StopHeader: View {
  @ObservedObject var stop: ModelData.Stop

  var body: some View {
    HStack {
      StopStatusImage(stop: stop)
      Text("\(stop.stopInfo.plannedWaypoint.description) (\(stop.stopInfo.tasks.count))")
    }
    .padding()
  }
}

struct StopHeader_Previews: PreviewProvider {
  static var previews: some View {
    let _ = LMFSDriverSampleApp.googleMapsInited
    StopHeader(stop: ModelData(filename: "test_manifest").stops[0])
      .previewLayout(.fixed(width: 300, height: 70))
  }
}
