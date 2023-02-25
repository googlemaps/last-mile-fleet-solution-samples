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
import GoogleMaps

/// Possible configuration errors for the iOS Sample app.
enum ConfigurationError: Error {
  /// This error indicates that the apiKey for the application has not been correctly configured.
  case apiKeyConfigurationError(String)
}

@main
struct LMFSDriverSampleApp: App {
  /// Initialize the Google Maps SDK, returning an error if initialization cannot be completed.
  static let googleMapsInited: ConfigurationError? = {
    let apiKey = ApplicationDefaults.apiKey.value
    guard apiKey.range(of: "^AIza[0-9A-Za-z-_]{35}$", options: .regularExpression) != nil else {
      return .apiKeyConfigurationError(
        "The API Key has not been correctly configured. Please set your project API key " +
        "either in ApplicationDefaults.swift or LocalOverrides/ApplicationDefaults.json.")
    }
    GMSServices.provideAPIKey(apiKey)
    GMSServices.setMetalRendererEnabled(true)
    return nil
  }()

  var body: some Scene {
    WindowGroup {
      if let error = LMFSDriverSampleApp.googleMapsInited {
        Text(String(describing: error))
      } else {
        let modelData = ModelData()
        ContentView()
          .environmentObject(modelData)
          .onAppear {
            modelData.assignVehicle(vehicleId: nil)
          }
      }
    }
  }
}
