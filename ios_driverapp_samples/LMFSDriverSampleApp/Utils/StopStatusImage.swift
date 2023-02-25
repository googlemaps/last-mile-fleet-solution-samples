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

extension Color {
  /// Colors for the UI. Written in designer-friendly 255 notation.
  static let stopStatusGreen = Color(.sRGB, red: 52.0 / 255, green: 168.0 / 255, blue: 83.0 / 255)
  static let stopStatusYellow = Color(.sRGB, red: 242.0 / 255, green: 153.0 / 255, blue: 0.0 / 255)
  static let stopStatusBlue = Color(.sRGB, red: 69.0 / 255, green: 151.0 / 255, blue: 255.0 / 255)
}

struct StopStatusImage: View {
  @ObservedObject var stop: ModelData.Stop

  var body: some View {
    switch stop.taskStatus {
    case .completed:
      Image(systemName: "checkmark.circle.fill")
        .foregroundColor(.stopStatusGreen)
        .frame(width: 30, height: 30)
    case .couldNotComplete:
      Image(systemName: "exclamationmark.circle.fill")
        .foregroundColor(.stopStatusYellow)

    case .pending:
      Image(systemName: "\(stop.order).circle.fill")
        .foregroundColor(.stopStatusBlue)
    }
  }
}

struct StopStatusImage_Previews: PreviewProvider {
  static var previews: some View {
    let _ = LMFSDriverSampleApp.googleMapsInited
    let modelData = ModelData(filename: "test_manifest")
    ForEach(modelData.stops) {
      StopStatusImage(stop: $0)
        .previewLayout(.fixed(width: 70, height: 70))
    }
  }
}
