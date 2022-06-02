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

/// Swift equivalent of google.type.LatLng.
struct LatLng: Hashable, Codable {
  let latitude: Double
  let longitude: Double
}

/// Swift equivalent of maps.fleetengine.delivery.v1.LocationInfo.
struct LocationInfo: Hashable, Codable {
  let point: LatLng
}

/// Swift equivalent of maps.fleetengine.delivery.v1.Task.
struct Task: Hashable, Codable, Identifiable {
  enum CodingKeys: String, CodingKey {
    case name, type, state, trackingId, deliveryVehicleId, plannedLocation, taskDuration
  }

  let name: String
  let type: TaskType
  let state: TaskState
  let trackingId: String
  let deliveryVehicleId: String?
  let plannedLocation: LocationInfo
  let taskDuration: Duration

  var id: String {
    return trackingId
  }

  init(from decoder: Decoder) throws {
    let deliveryVehicleIdKey = CodingUserInfoKey(rawValue: "deliveryVehicleId")!
    let defaultDeliveryVehicleId = String(describing: decoder.userInfo[deliveryVehicleIdKey])

    let container = try decoder.container(keyedBy: CodingKeys.self)
    self.name = try container.decode(String.self, forKey: .name)
    self.type = try container.decode(TaskType.self, forKey: .type)
    self.state = try container.decode(TaskState.self, forKey: .state)
    self.trackingId = try container.decode(String.self, forKey: .trackingId)
    self.deliveryVehicleId =
      try container.decodeIfPresent(String.self, forKey: .deliveryVehicleId)
      ?? defaultDeliveryVehicleId
    self.plannedLocation = try container.decode(LocationInfo.self, forKey: .plannedLocation)
    self.taskDuration = Duration(
      fromJSONString: try container.decode(String.self, forKey: .taskDuration))
  }

  func encode(to encoder: Encoder) throws {
    var container = encoder.container(keyedBy: CodingKeys.self)
    try container.encode(self.name, forKey: .name)
    try container.encode(self.type, forKey: .type)
    try container.encode(self.state, forKey: .state)
    try container.encode(self.trackingId, forKey: .trackingId)
    try container.encode(self.deliveryVehicleId, forKey: .deliveryVehicleId)
    try container.encode(self.plannedLocation, forKey: .plannedLocation)
    try container.encode(self.taskDuration.JSONString, forKey: .taskDuration)
  }

  enum TaskType: String, CaseIterable, Codable {
    case unspecified = "TYPE_UNSPECIFIED"
    case pickup = "PICKUP"
    case delivery = "DELIVERY"
    case scheduled_stop = "SCHEDULED_STOP"
    case unavailable = "UNAVAILABLE"
  }

  enum TaskState: String, CaseIterable, Codable {
    case state_unspecified = "STATE_UNSPECIFIED"
    case open = "OPEN"
    case closed = "CLOSED"
  }

  struct Duration: Hashable, Codable {
    let seconds: Int64
    let nanos: Int32

    init(fromJSONString: String) {
      let components = fromJSONString.split(separator: "s")
      self.seconds = Int64(components[0]) ?? 0
      self.nanos = components.count > 1 ? Int32(components[1])! : 0
    }

    var JSONString: String {
      return "\(seconds)s\((nanos != 0) ? String(nanos) : "")"
    }
  }

}
