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
import XCTest

@testable import LMFSDriverSampleApp

/// An implementation of NetworkManagerSession which returns publishers that publish a constant.
class ConstantValueSession: NetworkManagerSession {
  typealias Publisher = NetworkManagerSession.Publisher

  /// Captures the value of the request for which a publisher was most recently created.
  var request: URLRequest?

  func publisher(for request: URLRequest) -> Publisher {
    self.request = request
    return Just<Publisher.Output>(self.value)
      .setFailureType(to: Publisher.Failure.self)
      .eraseToAnyPublisher()
  }

  private let value: Publisher.Output

  /// - Parameters:
  /// value The constant value all publishers created by this session will publish.
  init(value: Publisher.Output) {
    self.value = value
  }
}

/// An implementation of NetworkManagerSession which returns publishers that throw an error.
class ConstantFailureSession: NetworkManagerSession {
  typealias Publisher = NetworkManagerSession.Publisher

  /// Captures the value of the request for which a publisher was most recently created.
  var request: URLRequest?

  func publisher(for request: URLRequest) -> Publisher {
    self.request = request
    return Fail<Publisher.Output, Publisher.Failure>(error: self.failure)
      .eraseToAnyPublisher()
  }

  private let failure: Publisher.Failure

  /// - Parameters:
  /// error The error that the publisher will throw.
  init(failure: Publisher.Failure) {
    self.failure = failure
  }
}

class NetworkServiceManagerTests: XCTestCase {
  private var modelData: ModelData = ModelData()

  override func setUp() {
    super.setUp()
    modelData = ModelData(filename: "test_manifest")
  }

  /// Tests the updateTask() method with a successful response.
  ///
  /// For this method in particular, an unsuccessful response doesn't actually have a different
  /// outcome, so only this test is done.
  func testUpdateTaskSuccess() throws {
    // Create the expected response (which is empty).
    let firstStop = try XCTUnwrap(modelData.stops.first)
    let task = try XCTUnwrap(modelData.tasks(stop: firstStop).first)
    let taskPath = "/task/\(task.id)"
    let expectedURL = NetworkServiceManager.backendBaseURL()
        .appendingPathComponent(taskPath, isDirectory: false)
    let response = HTTPURLResponse(url: expectedURL, mimeType: nil, expectedContentLength: 0,
                                   textEncodingName: nil)

    // Create the fake session which will return that response, and the network service manager.
    let session = ConstantValueSession(value:(data: Data(), response: response))
    let networkServiceManager = NetworkServiceManager(session: session)

    // Send update, wait for completion.
    task.outcome = .completed
    let expectation = self.expectation(description: "Wait for update task")
    networkServiceManager.updateTask(task: task) {
      expectation.fulfill()
    }
    self.wait(for: [ expectation ], timeout: 2.0)

    // Verify the request which was sent.
    let request = try XCTUnwrap(session.request)
    XCTAssertEqual(request.httpMethod, "POST")
    XCTAssertEqual(request.url?.path, taskPath)
    let bodyData = try XCTUnwrap(request.httpBody)
    let bodyDict = try XCTUnwrap(
        JSONDecoder().decode(Dictionary<String, String>.self, from: bodyData))
    XCTAssertEqual(bodyDict["task_outcome"], "SUCCEEDED")
  }

  /// Tests the updateStops() method with a successful response.
  ///
  /// For this method in particular, an unsuccessful response doesn't actually have a different
  /// outcome, so only this test is done.
  func testUpdateStopsSuccess() throws {
    // The expected response to updateTask is empty.
    let expectedPath = "/manifest/\(modelData.manifest.vehicle.vehicleId)"
    let expectedURL = NetworkServiceManager.backendBaseURL()
        .appendingPathComponent(expectedPath, isDirectory: false)
    let response = HTTPURLResponse(url: expectedURL, mimeType: nil, expectedContentLength: 0,
                                   textEncodingName: nil)

    // Create the fake session which will return that response, and the network service manager.
    let session = ConstantValueSession(value:(data: Data(), response: response))
    let networkServiceManager = NetworkServiceManager(session: session)

    // Send update and wait for completion.
    let firstStop = try XCTUnwrap(modelData.stops.first)
    for task in modelData.tasks(stop: firstStop) {
      task.outcome = .completed
    }
    let expectation = self.expectation(description: "Wait for update stop")
    networkServiceManager.updateStops(vehicleId: modelData.manifest.vehicle.vehicleId,
                                      newStopState: .enroute,
                                      stops: modelData.stops.suffix(modelData.stops.count - 1)) {
      expectation.fulfill()
    }
    self.wait(for: [ expectation ], timeout: 2.0)

    // Verify the expected request which was sent.
    let request = try XCTUnwrap(session.request)
    XCTAssertEqual(request.httpMethod, "POST")
    XCTAssertEqual(request.url, expectedURL)
    let bodyData = try XCTUnwrap(request.httpBody)
    let bodyDict = try XCTUnwrap(JSONSerialization.jsonObject(with:bodyData) as? [String: Any])
    XCTAssertEqual(bodyDict["current_stop_state"] as? String, "ENROUTE")
    let stopIDs = try XCTUnwrap(bodyDict["remaining_stop_id_list"] as? [String])
    XCTAssertEqual(Set(stopIDs), Set(arrayLiteral: "2", "3", "4"))
  }

  /// Tests the fetchManifest() method in the case of a successful fetch.
  func testFetchManifestSuccess() throws {
    // Build the expected response.
    let testDataFileURL = try XCTUnwrap(Bundle(for: Self.self).url(forResource: "test_manifest",
                                                                   withExtension: "json"))
    let manifestData = try Data(contentsOf: testDataFileURL)
    let response = HTTPURLResponse(url: expectedManifestURL, mimeType: "application/json",
                                   expectedContentLength: manifestData.count,
                                   textEncodingName: "utf-8")

    // Create the fake session which will return the data and response.
    let session = ConstantValueSession(value:(data: manifestData, response: response))

    // Create the networkServiceManager to test.
    let networkServiceManager = NetworkServiceManager(session: session)

    // Fetch the manifest and wait for the response.
    var manifest: Manifest? = nil
    let expectation = self.expectation(description: "Waiting for fetchManifest.")
    networkServiceManager.fetchManifest(clientId: "TestyClient", vehicleId: nil) { maybeManifest in
      expectation.fulfill()
      manifest = maybeManifest
    }
    self.wait(for: [ expectation ], timeout: 0.1)

    // Assert the expected request was made.
    try assertManifestRequest(request: try XCTUnwrap(session.request), url: expectedManifestURL,
                              clientID: "TestyClient")

    // Assert the expected Manifest was returned.
    let expectedManifest = try XCTUnwrap(Manifest.loadFrom(data: manifestData))
    XCTAssertEqual(manifest, expectedManifest)
  }

  /// Tests the fetchManifest() method in the case of a network timeout.
  func testFetchManifestTimedOut() throws {
    // Create the error the URLSession will throw.
    let error = URLError(.timedOut)

    // Create the fake session which will throw that error, and the network service manager.
    let session = ConstantFailureSession(failure: error)
    let networkServiceManager = NetworkServiceManager(session: session)

    // Fetch the manifest and wait for the response.
    var manifest: Manifest? = nil
    let expectation = self.expectation(description: "Waiting for fetchManifest.")
    networkServiceManager.fetchManifest(clientId: "TestyClient", vehicleId: nil) { maybeManifest in
      expectation.fulfill()
      manifest = maybeManifest
    }
    self.wait(for: [ expectation ], timeout: 0.1)

    // Assert the expected request was made.
    try assertManifestRequest(request: try XCTUnwrap(session.request), url: expectedManifestURL,
                              clientID: "TestyClient")

    // Assert the expected Manifest was returned.
    XCTAssertNil(manifest)
  }

  private var expectedManifestURL = NetworkServiceManager.backendBaseURL()
    .appendingPathComponent("/manifest/", isDirectory: false)

  private func assertManifestRequest(request: URLRequest, url: URL, clientID: String,
                                     file: StaticString = #file, line: UInt = #line) throws {
    XCTAssertEqual(request.httpMethod, "POST", file:file, line:line)
    XCTAssertEqual(request.url, expectedManifestURL, file:file, line:line)
    let bodyData = try XCTUnwrap(request.httpBody, file:file, line:line)
    let bodyDict = try XCTUnwrap(JSONSerialization.jsonObject(with:bodyData) as? [String: Any],
                                 file:file, line:line)
    XCTAssertEqual(bodyDict["client_id"] as? String, clientID, file:file, line:line)
  }
}
