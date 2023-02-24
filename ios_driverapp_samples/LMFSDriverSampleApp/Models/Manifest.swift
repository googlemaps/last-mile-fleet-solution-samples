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

import CoreLocation
import Foundation

/// Method on DateFormatter to support reading full ISO8601 dates.
///
/// This is essentially a workaround for the fact that the .iso8601 method of DateFormatter
/// does not support fractional seconds.
extension DateFormatter {
  fileprivate static let iso8601Full: DateFormatter = {
    let formatter = DateFormatter()
    formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZZZZZ"
    formatter.calendar = Calendar(identifier: .iso8601)
    formatter.timeZone = TimeZone(secondsFromGMT: 0)
    formatter.locale = Locale(identifier: "en_US_POSIX")
    return formatter
  }()
}

/// Swift equivalents of the items in the Manifest returned from the Backend sample application.
///
/// These are readonly objects reflecting a snapshot of the day's itinerary from the backend.
struct Manifest: Hashable, Codable {
  /// Definition of the Vehicle this manifest was generated for.
  var vehicle: Vehicle
  /// The list of stops for the day. Order in this list is not meaningful.
  var stops: [Stop]
  /// The tasks themselves.
  var tasks: [Task]
  /// The client ID.
  var clientId: String
  /// The order of the stops at the time this manifest was generated.
  var remainingStopIdList: [String]

  func tasksForStop(stop: Stop) -> [Task] {
    return stop.tasks.compactMap { targetTaskId in
      tasks.first(where: {
        $0.taskId == targetTaskId
      })
    }
  }

  /// Empty Manifest.
  init() {
    let waypoint = Waypoint(description: "Bad Stop", lat: 0.0, lng: 0.0)
    vehicle = Vehicle(vehicleId: "Unknown", providerId: "Unknown", startLocation: waypoint)
    tasks = [Task]()
    stops = [Stop]()
    clientId = ApplicationDefaults.clientId.value
    remainingStopIdList = [String]()
  }

  /// Initializer to read a manifest from a url
  static func loadFrom(url: URL) throws -> Manifest {
    return try loadFrom(data: Data(contentsOf: url))
  }

  /// Initializer to read a manifest from a Data object.
  static func loadFrom(data dataObject: Data) throws -> Manifest {
    let decoder = JSONDecoder()
    decoder.dateDecodingStrategy = .formatted(DateFormatter.iso8601Full)
    decoder.keyDecodingStrategy = .convertFromSnakeCase
    return try decoder.decode(self, from: dataObject)
  }

  struct Vehicle: Hashable, Codable {
    var vehicleId: String
    var providerId: String
    var startLocation: Waypoint
  }

  enum TaskType: String, Hashable, Codable {
    case PICKUP
    case DELIVERY
    case SCHEDULED_STOP
    case UNAVAIALBLE
  }

  struct Task: Hashable, Codable, Identifiable {
    var taskId: String
    var trackingId: String
    var plannedWaypoint: Waypoint
    var contactName: String?
    var plannedCompletionTime: Date?
    var plannedCompletionTimeRangeSeconds: Int64?
    var durationSeconds: Int64
    var taskType: TaskType
    var description: String?

    var id: String {
      return taskId
    }
  }

  struct Stop: Hashable, Codable, Identifiable {
    var plannedWaypoint: Waypoint
    /// Array of task IDs.
    var tasks: [String]
    /// The Id of the stop.
    var stopId: String

    var id: String {
      return stopId
    }
  }

  struct Waypoint: Hashable, Codable {
    var description: String
    var lat: Double
    var lng: Double

    var coordinate: CLLocationCoordinate2D {
      return CLLocationCoordinate2DMake(lat, lng)
    }
  }

}
