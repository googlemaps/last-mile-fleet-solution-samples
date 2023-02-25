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
import Foundation


/// This protocol defines an interface for obtaining publishers for network data operations.
protocol NetworkManagerSession {
  /// The type of the objects that the publisher(for:) method returns. This is aligned with
  /// URLSession.DataTaskPublisher since that's a common production implementation.
  typealias Publisher = AnyPublisher<URLSession.DataTaskPublisher.Output,
                                     URLSession.DataTaskPublisher.Failure>

  /// Returns an publisher for the given URLRequest.
  func publisher(for: URLRequest) -> Publisher
}

/// The production implementation of NetworkManagerSession is created by extending URLSession to
/// implement the protocol.
extension URLSession: NetworkManagerSession {
  func publisher(for request: URLRequest) -> NetworkManagerSession.Publisher {
    return self.dataTaskPublisher(for: request).eraseToAnyPublisher()
  }
}

/// The class that manages communication with the backend.
class NetworkServiceManager: ObservableObject {
  /// Pending manifest get requests.
  private var pendingManifestFetches = InMemoryRequestTracker<String>()

  /// The identifier for a task update request is the final state of the task after the update.
  private struct TaskUpdateIdentifier: Hashable {
    let taskId: String
    let outcome: ModelData.Task.Outcome
  }

  /// Pending task update requests.
  private var pendingTaskUpdates = InMemoryRequestTracker<TaskUpdateIdentifier>()

  /// The identifier for a stop update request is the final state of the stop after the update.
  private struct StopUpdateIdentifier: Hashable {
    let newState: ModelData.StopState?
    let stopIds: [String]
  }

  /// Pending stop update requests.
  ///
  /// We limit the number of outstanding stop requests to one, because subsequent stop requests
  /// should always incorporate the result of preceding requests: Stop states can only go in a
  /// certain order, and the remaining stop IDs list should only ever get shorter. By only keeping
  /// a single outstanding stop update, we don't have to worry about stop updates being applied
  /// out-of-order on the backend.
  private var pendingStopUpdates = InMemoryRequestTracker<StopUpdateIdentifier>(requestLimit: 1)

  /// The properties below are published for the sake of diagnostic/debugging views.

  /// The error, if any, from the most recent attempt to update tasks.
  @Published var taskUpdateLatestError: Error?
  @Published var taskUpdatesCompleted = 0
  @Published var taskUpdatesFailed = 0

  /// A status string for the most recent attempt to fetch a manifest.
  @Published var manifestFetchStatus = "Uninitialized"

  /// A status string for the most recent attempt to update tasks.
  @Published var taskUpdateStatus = "Uninitialized"

  /// A status string for the most recent attempt to update stops.
  @Published var stopUpdateStatus = "None"

  /// Sends a request to update the outcome of the given task.
  /// - Parameter task: The task whose outcome should be updated.
  /// - Parameter updateCompletion: A block which will be invoked when the update completes or fails.
  func updateTask(task: ModelData.Task, updateCompletion: os_block_t? = nil) {
    let taskId = task.taskInfo.taskId
    let outcomeValue: String
    switch task.outcome {
    case .pending:
      return
    case .completed:
      outcomeValue = "SUCCEEDED"
    case .couldNotComplete:
      outcomeValue = "FAILED"
    }

    var components = URLComponents(url: NetworkServiceManager.backendBaseURL(),
                                   resolvingAgainstBaseURL: false)!
    components.path = "/task/\(taskId)"
    var request = URLRequest(url: components.url!)
    request.httpMethod = "POST"
    let requestBody = ["task_outcome": outcomeValue]
    do {
      request.httpBody = try JSONEncoder().encode(requestBody)
    } catch {
      taskUpdateLatestError = error
      let errorDesc = "Error encoding json: \(error)"
      self.taskUpdateStatus = errorDesc
      updateCompletion?()
      return
    }
    let taskUpdateIdentifier = TaskUpdateIdentifier(taskId: taskId, outcome: task.outcome)
    let taskUpdateCancellable = session
      .publisher(for: request)
      .receive(on: RunLoop.main)
      .sink(
        receiveCompletion: { completion in
          switch completion {
          case .finished:
            self.taskUpdateLatestError = nil
          case .failure(let error):
            self.taskUpdateLatestError = error
            self.taskUpdatesFailed += 1
          }
          self.taskUpdatesCompleted += 1
          self.pendingTaskUpdates.completed(identifier: taskUpdateIdentifier)
          updateCompletion?()
        },
        receiveValue: { _ in
        })
    self.pendingTaskUpdates.started(
      identifier: taskUpdateIdentifier,
      cancellable: taskUpdateCancellable)
  }

  /// Attempts to fetch a new manifest from the backend.
  ///
  /// - Parameters:
  ///   - clientId: This client's persistent ID string.
  ///   - vehicleId: Specifies the ID of the vehicle to fetch. If nil, the next available vehicle
  ///                  will be fetched.
  ///   - completion: This will be called exactly once with the new manifest or nil.
  func fetchManifest(
    clientId: String,
    vehicleId: String?,
    fetchCompletion: @escaping (Manifest?) -> Void
  ) {
    manifestFetchStatus = "waiting for response"
    var components = URLComponents(url: NetworkServiceManager.backendBaseURL(),
                                   resolvingAgainstBaseURL: false)!
    components.path = "/manifest/\(vehicleId ?? "")"
    var request = URLRequest(url: components.url!)
    request.httpMethod = "POST"
    let requestBody = ["client_id": clientId]
    do {
      request.httpBody = try JSONEncoder().encode(requestBody)
    } catch {
      self.manifestFetchStatus = "Error encoding json: \(error)"
      fetchCompletion(nil)
      return
    }

    let manifestFetchCancellable = session
      .publisher(for: request)
      .receive(on: RunLoop.main)
      .sink(
        receiveCompletion: { completion in
          switch completion {
          case .failure(let error):
            self.manifestFetchStatus = "Error: \(String(describing: error))"
            fetchCompletion(nil)
          case .finished:
            self.manifestFetchStatus = "Success"
          }
          self.pendingManifestFetches.completed(identifier: clientId)
        },
        receiveValue: { urlResponse in
          if let httpResponse = urlResponse.response as? HTTPURLResponse {
            if httpResponse.statusCode != 200 {
              let responseCodeString =
                HTTPURLResponse.localizedString(forStatusCode: httpResponse.statusCode)
              let dataString = String(decoding: urlResponse.data, as: UTF8.self)
              self.manifestFetchStatus = "Status \(responseCodeString) Response: \(dataString)"
              fetchCompletion(nil)
              return
            }
          }
          do {
            let newManifest = try Manifest.loadFrom(data: urlResponse.data)
            self.manifestFetchStatus = "Success"
            fetchCompletion(newManifest)
          } catch {
            let dataString = String(decoding: urlResponse.data, as: UTF8.self)
            let errorDesc = "Error parsing output: \(error) data: \(dataString)"
            self.manifestFetchStatus = errorDesc
            fetchCompletion(nil)
          }
        }
      )
    self.pendingManifestFetches.started(
      identifier: clientId,
      cancellable: manifestFetchCancellable)

  }

  /// Sends a request to update the current stop's state and/or the list of remaining stops.
  /// - Parameters:
  ///   - vehicleId: The vehicle ID for the currently loaded manifest.
  ///   - newStopState: The new state of the current stop.
  ///   - stops: The new list of remaining stops in this manifest.
  func updateStops(
    vehicleId: String,
    newStopState: ModelData.StopState? = nil,
    stops: [ModelData.Stop],
    updateCompletion: os_block_t? = nil
  ) {
    let newStopIds = stops.filter { $0.taskStatus == .pending }.map { $0.stopInfo.id }
    let stopUpdateIdentifier = StopUpdateIdentifier(newState: newStopState, stopIds: newStopIds)

    var components = URLComponents(url: NetworkServiceManager.backendBaseURL(),
                                   resolvingAgainstBaseURL: false)!
    components.path = "/manifest/\(vehicleId)"
    var request = URLRequest(url: components.url!)
    request.httpMethod = "POST"
    var requestBody: [String: Any] = [:]
    if let newStopStateValue = newStopState {
      requestBody["current_stop_state"] = newStopStateValue.rawValue
    }
    requestBody["remaining_stop_id_list"] = newStopIds
    do {
      request.httpBody =
        try JSONSerialization.data(
          withJSONObject: requestBody,
          options: JSONSerialization.WritingOptions()
        ) as Data
    } catch {
      self.stopUpdateStatus = "Error encoding json: \(error)"
      updateCompletion?()
      return
    }
    self.stopUpdateStatus = "Pending"
    let stopUpdateCancellable = session
      .publisher(for: request)
      .receive(on: RunLoop.main)
      .sink(
        receiveCompletion: { completion in
          switch completion {
          case .failure(let error):
            self.stopUpdateStatus = "Error: \(String(describing: error))"
          case .finished:
            self.stopUpdateStatus = "Success"
          }
          self.pendingStopUpdates.completed(identifier: stopUpdateIdentifier)
          updateCompletion?()
        },
        receiveValue: { _ in })
    self.pendingStopUpdates.started(
      identifier: stopUpdateIdentifier,
      cancellable: stopUpdateCancellable)
  }

  private var session: NetworkManagerSession

  /// Creates a new NetworkServiceManager.
  /// - Parameter session: The NetworkManagerSession to use for HTTP requests.
  init(session: NetworkManagerSession = URLSession.shared) {
    self.session = session

    // This can't be done in the property initialization because self must be available.
    pendingManifestFetches.errorHandler = self.manifestTrackingError
    pendingTaskUpdates.errorHandler = self.taskUpdateTrackingError
    pendingStopUpdates.errorHandler = self.stopUpdateTrackingError
  }
  /// The base URL for the backend from defaults.
  static func backendBaseURL() -> URL {
    // The backend URL may be updated while the app is running, so always re-read from defaults.
    return URL(string: ApplicationDefaults.backendBaseURLString.value)!
  }

  private func manifestTrackingError(error: RequestTrackingError) {
    // The tracker calls back on an arbitrary queue, so dispatch to main queue before updating UI.
    DispatchQueue.main.async {
      self.manifestFetchStatus = String(describing: error)
    }
  }

  private func taskUpdateTrackingError(error: RequestTrackingError) {
    DispatchQueue.main.async {
      self.taskUpdateStatus = String(describing: error)
    }
  }

  private func stopUpdateTrackingError(error: RequestTrackingError) {
    DispatchQueue.main.async {
      self.stopUpdateStatus = String(describing: error)
    }
  }
}
