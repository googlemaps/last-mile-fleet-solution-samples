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

/// The error returned in a callback from a `RequestTracking` implementation.
enum RequestTrackingError: Error, Equatable, CustomStringConvertible {
  /// This error indicates two requests with the same identifier have been started but not
  /// completed. The tracker requires all requests to have a unique identifier. If it is valid to
  /// have multiple identical requests, consider adding a timestamp to the request identifier.
  ///
  /// The first associated value is a human-readable representation of the request identifier.
  /// The second associated value is the cancellable for the second call to `.started()`.
  case duplicateRequestIdentifier(String, AnyCancellable)

  /// This error indicates a `.completed()` was called on a request which was never passed to
  /// `.started()`. This indicates some kind of control flow issue in the calling application.
  ///
  /// The associated value is a human-readable representation of the request identifier.
  case unknownIdentifier(String)

  var description: String {
    switch self {
    case .duplicateRequestIdentifier(let identifier, _):
      return "Duplicate task update \(identifier) started."
    case .unknownIdentifier(let identifier):
      return "Unknown task update \(identifier) completed."
    }
  }

}

/// This protocol defines the generic implementation of an object which can track pending requests.
///
/// All implementations of this protocol are expected to be thread-safe.
protocol RequestTracking {
  /// The request identifier should represent the request uniquely across all requests that
  /// may exist at the same time. For instance, for GET requests, the resource identifier string
  /// is typically a good choice.
  associatedtype RequestIdentifier: Hashable

  /// The type for the error handler.
  typealias ErrorHandler = (RequestTrackingError) -> Void

  /// The callback for errors. Called asynchronously from an arbitrary queue.
  ///
  /// It is recommended to always set a custom error handler, since errors indicates a programming
  /// error in the caller.
  ///
  /// Errors passed to this handler are errors in tracking the request; they are unrelated to any
  /// errors in sending or handling the request, which will be returned from Combine rather than
  /// from this object.
  var errorHandler: ErrorHandler { get set }

  /// Returns the number of currently pending requests.
  var count: Int { get }

  /// This function should be called once when a request has been started.
  ///
  /// Implementations of this method may be asynchronous, such that side effects
  /// such as changes to the value of `count` or callbacks to `errorHandler` may occur after
  /// this function has returned.
  ///
  /// - Parameters:
  ///   - request: The object defining the request to be tracked.
  ///   - cancellable: The cancellable for this request.
  ///
  ///   After this call succeeds, a strong reference to  `cancellable` is held by the request
  ///   tracker until `.completed()` is called with the same request identifier. This frees the
  ///   caller from needing to keep a reference to the cancellable.
  func started(identifier: RequestIdentifier, cancellable: AnyCancellable)

  /// This function should be called once when the request has completed or failed.
  ///
  /// Once both `.started()` and `.completed()` have been called for a given request, the tracker
  /// will no longer hold a reference to the cancellable passed to `.started()`.
  ///
  /// Implementations of this method may be asynchronous, such that side effects such as changes
  /// to the value of `count`, releasing of the cancellable, or calls to `errorHandler` may occur
  /// after this call has returned.
  func completed(identifier: RequestIdentifier)
}

/// Implementation of RequestTracking that tracks requests in memory.
class InMemoryRequestTracker<RequestIdentifier: Hashable>: RequestTracking {
  /// The type for error handlers for this class.
  typealias ErrorHandler = (RequestTrackingError) -> Void

  /// A handler which will be called if errors are detected.
  var errorHandler: ErrorHandler = { _ in }

  /// Optional block to be notified when a new request is about to be tracked.
  var willInsertBlock: ((RequestIdentifier) -> Void)?

  /// Optional block to be notified when a tracked request has been completed or cancelled.
  var didRemoveBlock: ((RequestIdentifier) -> Void)?

  /// The serial dispatch queue on which the request tracker performs work. All calls
  /// to `.started()` or `.completed()` may enqueue activity on this queue.
  /// Exposed for the sake of testing.
  let serialQueue: DispatchQueue = DispatchQueue.main

  /// Dictionary mapping the request identifier to the cancellable for the request.
  private var requests: [RequestIdentifier: AnyCancellable] = [:]

  var count: Int {
    return requests.count
  }

  /// The maximum allowable number of pending requests. See `init`.
  let requestLimit: Int

  func started(identifier: RequestIdentifier, cancellable: AnyCancellable) {
    serialQueue.async {
      if self.requests[identifier] != nil {
        // If a request with the same identifier has already been passed to `.started()`, this is
        // a programmer error of passing duplicate request identifiers. Call the error handler.
        self.errorHandler(
          RequestTrackingError.duplicateRequestIdentifier("\(identifier)", cancellable))
      } else {
        if self.requestLimit > 0 {
          // Reduce the number of pending requests to less than the limit.
          while (self.requests.count >= self.requestLimit) {
            if let (removedIdentifier, cancellable) = self.requests.popFirst() {
              cancellable.cancel()
              if let didRemoveBlock = self.didRemoveBlock {
                didRemoveBlock(removedIdentifier)
              }
            }
          }
        }
        if let willInsertBlock = self.willInsertBlock {
          willInsertBlock(identifier)
        }
        self.requests[identifier] = cancellable
      }
    }
  }

  func completed(identifier: RequestIdentifier) {
    serialQueue.async {
      if self.requests.removeValue(forKey: identifier) == nil {
        // No tracked request was found, so call the error handler.
        self.errorHandler(RequestTrackingError.unknownIdentifier("\(identifier)"))
      } else {
        if let didRemoveBlock = self.didRemoveBlock {
          didRemoveBlock(identifier)
        }
      }
    }
  }

  /// Initializes a new InMemoryRequestTracker object.
  /// - Parameter requestLimit: The maximum allowable number of requests which have been started
  ///                           but not completed. If another request is started when this limit
  ///                           has been reached, a random pending request will be cancelled.
  ///                           By default, there is no limit.
  init(requestLimit limit: Int = 0) {
    requestLimit = limit
  }
}
