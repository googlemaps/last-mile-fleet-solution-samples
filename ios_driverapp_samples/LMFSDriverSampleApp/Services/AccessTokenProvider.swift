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
import GoogleRidesharingDriver

/// A service that can fetch tokens from the backend and test their validity.
///
/// Note that this class needs to inherit from NSObject in order to be able to adopt an Objective-C
/// protocol.
class AccessTokenProvider: NSObject, GMTDAuthorization, ObservableObject {

  /// The data for a token as received from the Backend.
  struct Token: Hashable, Codable {
    let token: String
    let creationTimestampMs: Date
    let expirationTimestampMs: Date

    static func loadFrom(from data: Data) throws -> Token {
      let decoder = JSONDecoder()
      decoder.dateDecodingStrategy = .millisecondsSince1970
      decoder.keyDecodingStrategy = .convertFromSnakeCase
      return try decoder.decode(self, from: data)
    }
  }

  /// Special-purpose error to indicate an uninitialized enum.
  enum Errors: Error {
    case uninitialized
  }

  /// Type for the most recent result.
  typealias TokenOrError = Result<Token, Error>

  /// Type for the callback from fetch().
  typealias TokenCompletion = (TokenOrError) -> Void

  /// The vehicleId for which we fetch tokens.
  var vehicleId: String? {
    didSet {
      synchronize {
        result = TokenOrError.failure(Errors.uninitialized)
        fetch(nil)
      }
    }
  }

  /// This Error indicates that the vehicleId property was not set before fetch() was called.
  enum TokenServiceError: Error {
    case vehicleIdUninitialized
  }

  /// The most recent result.
  ///
  /// Any token in this result may not be valid, so this shouldn't be
  /// used for transactions; those should always call fetchToken().
  /// This property is intended for debugging UIs that display the last result.
  @Published private(set) var result = TokenOrError.failure(Errors.uninitialized)

  /// The cancelable for any token request in-flight.
  private var inFlightFetch: AnyCancellable?

  /// The set of completions to notify when a fetch completes.
  private var completions: [TokenCompletion] = [TokenCompletion]()

  /// Fetches an up-to-date token or reports an error.
  ///
  /// This fetches a token, taking advantage of an internally cached token if it is still valid.
  /// An error will be passed to the callback if a token cannot be fetched or if the vehicleId
  /// property has not been initialized.
  ///
  /// Since this is called by the GMTDAuthorization entry point below, this method
  /// may be invoked on an arbitrary queue.
  func fetch(_ callback: TokenCompletion?) {
    synchronize {
      switch result {
      case .success(let token):
        if token.expirationTimestampMs >= Date() {
          callback?(result)
          return
        }
        fallthrough
      case .failure:
        if let vehicleId = vehicleId {
          if let callback = callback {
            completions.append(callback)
          }
          if inFlightFetch != nil {
            return
          }
          let backendBaseURL = URL(string: ApplicationDefaults.backendBaseURLString.value)!
          var components = URLComponents(url: backendBaseURL, resolvingAgainstBaseURL: false)!
          components.path = "/token/delivery_driver/\(vehicleId)"
          let request = URLRequest(url: components.url!)
          inFlightFetch =
            URLSession
            .DataTaskPublisher(request: request, session: .shared)
            .receive(on: RunLoop.main)
            .sink(
              receiveCompletion: { [weak self] completion in
                self?.handleReceiveCompletion(completion: completion)
              },
              receiveValue: { [weak self] output in
                self?.handleReceiveData(data: output.data)
              }
            )
          result = TokenOrError.failure(Errors.uninitialized)
        } else {
          /// vehicleId == nil
          if let callback = callback {
            callback(TokenOrError.failure(TokenServiceError.vehicleIdUninitialized))
          }
          return
        }
      }
    }
  }

  /// Handler for the completion of the URLSession DataTask.
  private func handleReceiveCompletion(
    completion: Subscribers.Completion<URLSession.DataTaskPublisher.Failure>
  ) {
    self.synchronize {
      switch completion {
      case .finished:
        break
      case .failure(let error):
        self.result = TokenOrError.failure(error)
        self.invokeCallbacks()
      }
      /// switch(completion)
      self.inFlightFetch = nil
    }
  }

  /// Handler for the data callback of the URLSession DataTask.
  private func handleReceiveData(data: Data) {
    var newResult = TokenOrError.failure(Errors.uninitialized)
    do {
      try newResult = TokenOrError.success(Token.loadFrom(from: data))
    } catch {
      newResult = TokenOrError.failure(error)
    }
    self.synchronize {
      self.result = newResult
      self.invokeCallbacks()
    }
  }

  /// Entry point for GMTDAuthorization protocol called by DriverSDK.
  ///
  /// The GMTDAuthorization header file notes that this method may be invoked on an arbitrary
  /// queue.
  func fetchToken(
    with authorizationContext: GMTDAuthorizationContext?,
    completion: @escaping GMTDAuthTokenFetchCompletionHandler
  ) {
    /// Enforce the function signature.
    assert(authorizationContext?.vehicleID != nil)
    /// We expect DriverSDK to only be invoked on the single manifest supported by this app.
    assert(authorizationContext?.vehicleID == vehicleId)

    /// Fetch the token from the token service, which already handles caching.
    fetch { tokenOrError in
      switch tokenOrError {
      case .failure(let error):
        completion(nil, error)
      case .success(let token):
        completion(token.token, nil)
      }
    }
  }

  /// Re-entrant, lock-based synchronization primitive.
  ///
  /// Equivalent to Objective-C @synchronized. See
  /// https://www.cocoawithlove.com/blog/2016/06/02/threads-and-mutexes.html for more commentary
  /// on synchronization in Swift. That also explains why this is in the individual class and not
  /// placed in a helper function.
  private func synchronize<T>(execute: () throws -> T) rethrows -> T {
    objc_sync_enter(self)
    defer { objc_sync_exit(self) }
    return try execute()
  }

  /// Invoke all pending callbacks with the current result and clear the completions array.
  ///
  /// This must be called from inside a synchronize block.
  private func invokeCallbacks() {
    for completion in completions {
      completion(self.result)
    }
    completions.removeAll()
  }
}
