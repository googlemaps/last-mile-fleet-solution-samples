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

import Foundation
import GoogleRidesharingDriver
import SwiftUI

/// An implementation of the GMTDVehicleReporterListener protocol which accumulates statistics.
class VehicleReporterListener: NSObject, GMTDVehicleReporterListener {
  @Published var successfulUpdates: UInt
  @Published var emptyMaskUpdates: UInt
  @Published var failedUpdates: UInt
  @Published var lastError: Error?

  func vehicleReporter(
    _ vehicleReporter: GMTDVehicleReporter,
    didSucceed vehicleUpdate: GMTDVehicleUpdate
  ) {
    successfulUpdates += 1
  }

  func vehicleReporter(
    _ vehicleReporter: GMTDVehicleReporter,
    didFail vehicleUpdate: GMTDVehicleUpdate,
    withError error: Error
  ) {
    /// We count empty update_mask errors as a separate cateogry, since the first update after
    /// startup typically causes such an error. See the Getting Started guide for the iOS DriverSDK
    /// (https://developers.google.com/maps/documentation/transportation-logistics/on-demand-rides-deliveries-solution/trip-order-progress/driver-sdk/driver_sdk_quickstart_ios)
    /// for more information on this error.
    let fullError = error as NSError
    if let innerError = fullError.userInfo[NSUnderlyingErrorKey] as? NSError {
      let innerFullError = innerError as NSError
      if innerFullError.localizedDescription.contains("update_mask cannot be empty") {
        emptyMaskUpdates += 1
        return
      }
    }
    failedUpdates += 1
    lastError = error
  }

  override init() {
    successfulUpdates = 0
    emptyMaskUpdates = 0
    failedUpdates = 0
  }
}
