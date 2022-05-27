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

import Combine
import SwiftUI

extension AccessTokenProvider.TokenOrError {
  // Returns whether the result represents success or not.
  var valid: Bool {
    switch self {
    case .success:
      return true
    case .failure:
      return false
    }
  }

  // Returns a label for whether the result is success or failure.
  var label: String {
    return self.valid ? "Success" : "Failure"
  }

  // Returns a potentially long description of the result.
  var fullDescription: String {
    switch self {
    case .success(let token):
      return token.token
    case .failure(let error):
      return String(describing: error)
    }
  }
}

/// This view that the user sees when tapping on the settings tab.
struct Settings: View {
  @EnvironmentObject var modelData: ModelData
  @EnvironmentObject var accessTokenProvider: AccessTokenProvider
  @EnvironmentObject var networkServiceManager: NetworkServiceManager
  @State private var vehicleIdToLoad: String = ""
  @State private var connectivityStatus = "unverified"
  // Copy a reference to these objects into a var so we can construct bindings to it.
  @State private var backendBaseURLString = ApplicationDefaults.backendBaseURLString
  @State private var disableLocationReporting = ApplicationDefaults.disableLocationReporting
  @State private var disableLocationSimulation = ApplicationDefaults.disableLocationSimulation

  var body: some View {
    NavigationView {
      Form {
        Section(header: Text("Vehicle")) {
          Text("Current: \(modelData.manifest.vehicle.vehicleId)")

          Button("Assign Next Available") {
            modelData.assignVehicle(vehicleId: nil)
          }
          .disabled(modelData.stops.count > 0)

          Text("Load arbitrary vehicle ID:")
          TextField(
            "Vehicle ID", text: $vehicleIdToLoad,
            onCommit: { modelData.assignVehicle(vehicleId: vehicleIdToLoad) }
          )
          .disableAutocorrection(true)
          .textInputAutocapitalization(.never)

          Text("Vehicle loading status: \(networkServiceManager.manifestFetchStatus)")
            .foregroundColor(.gray)
        }

        Section(header: Text("Backend app URL")) {
          TextField("Base URL for backend", text: $backendBaseURLString.value)
            .disableAutocorrection(true)
            .textInputAutocapitalization(.never)

          Button("Test") {
            testConnectivity()
          }
          Text("Status: \(connectivityStatus)")
            .foregroundColor(.gray)
        }

        Section(header: Text("Navigation")) {
          Toggle("Disable simulated location", isOn: $disableLocationSimulation.value)
          Toggle("Disable location reporting", isOn: $disableLocationReporting.value)
        }

        Section(header: Text("Token")) {
          let result = modelData.accessTokenProvider.result
          let fullDescription = result.fullDescription
          let shortDescription = String(fullDescription.prefix(12))
          HStack {
            Text("Status: \(result.label): \(shortDescription)...")
            Spacer()
            Button(action: {
              UIPasteboard.general.string = fullDescription
            }) {
              Image(systemName: "doc.on.doc")
            }
          }
          Button(
            "Attempt token fetch",
            action: {
              modelData.accessTokenProvider.fetch(nil)
            }
          )
          .disabled(result.valid)
        }

        Section(header: Text("Vehicle Reporter Status")) {
          let listener = modelData.vehicleReporterListener
          Text("Updates completed: \(listener.successfulUpdates)")
          Text("Updates with empty mask: \(listener.emptyMaskUpdates)")
          Text("Updates failed: \(listener.failedUpdates)")
          let errorText =
            (listener.lastError == nil) ? "None" : String(describing: listener.lastError)
          Text("Last error: \(errorText)")
        }

        Section(header: Text("Stop Update Status")) {
          Text(networkServiceManager.stopUpdateStatus)
        }

        Section(header: Text("Task Update Status")) {
          let errorText =
            (networkServiceManager.taskUpdateLatestError == nil)
            ? "None" : String(describing: networkServiceManager.taskUpdateLatestError)
          Text("Latest Error: \(errorText)")
          Text("Updates completed: \(networkServiceManager.taskUpdatesCompleted)")
          Text("Updates failed: \(networkServiceManager.taskUpdatesFailed)")
        }
      }
    }
  }

  private func testConnectivity() {
    let pingURLString = "\(ApplicationDefaults.backendBaseURLString.value)/"
    let pingURL = URL(string: pingURLString)!
    let task = URLSession.shared.dataTask(
      with: pingURL,
      completionHandler: testConnectivityCallback
    )
    task.resume()
    connectivityStatus = "waiting for response"
  }

  private func testConnectivityCallback(data: Data?, response: URLResponse?, error: Error?) {
    if data == nil && response == nil {
      connectivityStatus = "Failed: \(String(describing:error))"
    } else {
      connectivityStatus = "verified"
    }
  }
}

struct Settings_Previews: PreviewProvider {
  static var previews: some View {
    let _ = LMFSDriverSampleApp.googleMapsInited
    let modelData = ModelData(filename: "test_manifest")
    Settings()
      .environmentObject(modelData)
      .environmentObject(modelData.networkServiceManager)
      .environmentObject(modelData.accessTokenProvider)
  }
}

/// Swift substring manipulation needs more helpers.
extension String {
  fileprivate func index(at: Int) -> String.Index {
    return self.index(self.startIndex, offsetBy: at)
  }

  fileprivate func substring(start: Int, length: Int) -> Substring {
    return self[self.index(at: start)..<self.index(at: start + length)]
  }
}
