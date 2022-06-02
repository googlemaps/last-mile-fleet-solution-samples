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
import XCTest

@testable import LMFSDriverSampleApp

class RequestTrackingTests: XCTestCase {
  // Test timeout when waiting for expectations.
  let timeout = 1.0

  func testNormalUsage() {
    let inMemoryRequestTracker = InMemoryRequestTracker<String>()
    XCTAssertEqual(inMemoryRequestTracker.count, 0)
    XCTAssertEqual(inMemoryRequestTracker.requestLimit, 0)

    var cancellableCancelled = false
    var errorOccurred = false
    inMemoryRequestTracker.errorHandler = { error in
      errorOccurred = true
    }
    var insertedIdentifier: String? = nil
    inMemoryRequestTracker.willInsertBlock = { identifier in
      insertedIdentifier = identifier
    }
    var removedIdentifier: String? = nil
    inMemoryRequestTracker.didRemoveBlock = { identifier in
      removedIdentifier = identifier
    }

    let expectation1 = self.expectation(description: "Wait for started")
    inMemoryRequestTracker.started(
      identifier: "rq1",
      cancellable: AnyCancellable({
        // Note: be careful not to store a cancellable in a variable; that would make the
        // assertion that this object is cancelled (due to being released) unpredictable.
        // See the longer explanation in testDuplicateRequestIdentifierError() for more context.
        cancellableCancelled = true
      })
    )
    inMemoryRequestTracker.serialQueue.async { expectation1.fulfill() }
    self.wait(for: [expectation1], timeout: timeout)
    XCTAssertEqual(inMemoryRequestTracker.count, 1)
    XCTAssertFalse(cancellableCancelled)
    XCTAssertEqual(insertedIdentifier, "rq1")
    XCTAssertNil(removedIdentifier)
    XCTAssertFalse(errorOccurred)

    let expectation2 = self.expectation(description: "Wait for completed")
    inMemoryRequestTracker.completed(identifier: "rq1")
    inMemoryRequestTracker.serialQueue.async { expectation2.fulfill() }
    self.wait(for: [expectation2], timeout: timeout)
    XCTAssertEqual(inMemoryRequestTracker.count, 0)
    XCTAssertEqual(removedIdentifier, "rq1")

    // When the tracker is no longer retaining the cancellation, it's .cancel() routine should be
    // invoked.
    XCTAssertTrue(cancellableCancelled)
    XCTAssertFalse(errorOccurred)
  }

  /// Tests the normal use case, but making sure we handle nil callback blocks.
  func testNormalUsageNoInsertionRemovedBlocks() {
    let inMemoryRequestTracker = InMemoryRequestTracker<String>()
    XCTAssertEqual(inMemoryRequestTracker.count, 0)
    XCTAssertEqual(inMemoryRequestTracker.requestLimit, 0)

    var cancellableCancelled = false

    let expectation1 = self.expectation(description: "Wait for started")
    inMemoryRequestTracker.started(
      identifier: "rq1",
      cancellable: AnyCancellable({
        // Note: be careful not to store a cancellable in a variable; that would make the
        // assertion that this object is cancelled (due to being released) unpredictable.
        // See the longer explanation in testDuplicateRequestIdentifierError() for more context.
        cancellableCancelled = true
      })
    )
    inMemoryRequestTracker.serialQueue.async { expectation1.fulfill() }
    self.wait(for: [expectation1], timeout: timeout)
    XCTAssertEqual(inMemoryRequestTracker.count, 1)

    let expectation2 = self.expectation(description: "Wait for completed")
    inMemoryRequestTracker.completed(identifier: "rq1")
    inMemoryRequestTracker.serialQueue.async { expectation2.fulfill() }
    self.wait(for: [expectation2], timeout: timeout)
    XCTAssertEqual(inMemoryRequestTracker.count, 0)

    // When the tracker is no longer retaining the cancellation, it's .cancel() routine should be
    // invoked.
    XCTAssertTrue(cancellableCancelled)
  }

  /// Tests that starting then completing two requests in sequence works correctly.
  func testSerialDispatches() {
    let inMemoryRequestTracker = InMemoryRequestTracker<String>()
    XCTAssertEqual(inMemoryRequestTracker.count, 0)
    XCTAssertEqual(inMemoryRequestTracker.requestLimit, 0)

    var cancellableACancelled = false
    var errorOccurred = false
    inMemoryRequestTracker.errorHandler = { error in
      errorOccurred = true
    }
    var insertedIdentifier: String? = nil
    inMemoryRequestTracker.willInsertBlock = { identifier in
      insertedIdentifier = identifier
    }
    var removedIdentifier: String? = nil
    inMemoryRequestTracker.didRemoveBlock = { identifier in
      removedIdentifier = identifier
    }

    let expectationA1 = self.expectation(description: "Wait for A started")
    inMemoryRequestTracker.started(
      identifier: "rqA",
      cancellable: AnyCancellable({
        // Note: be careful not to store a cancellable in a variable; that would make the
        // assertion that this object is cancelled (due to being released) unpredictable.
        // See the longer explanation in testDuplicateRequestIdentifierError() for more context.
        cancellableACancelled = true
      })
    )
    inMemoryRequestTracker.serialQueue.async { expectationA1.fulfill() }
    self.wait(for: [expectationA1], timeout: timeout)
    XCTAssertEqual(inMemoryRequestTracker.count, 1)
    XCTAssertFalse(cancellableACancelled)
    XCTAssertEqual(insertedIdentifier, "rqA")
    XCTAssertNil(removedIdentifier)
    XCTAssertFalse(errorOccurred)

    let expectationA2 = self.expectation(description: "Wait for A completed")
    inMemoryRequestTracker.completed(identifier: "rqA")
    inMemoryRequestTracker.serialQueue.async { expectationA2.fulfill() }
    self.wait(for: [expectationA2], timeout: timeout)
    XCTAssertEqual(inMemoryRequestTracker.count, 0)

    // When the tracker is no longer retaining the cancellation, it's .cancel() routine should be
    // invoked.
    XCTAssertTrue(cancellableACancelled)
    XCTAssertEqual(removedIdentifier, "rqA")
    XCTAssertFalse(errorOccurred)

    // Dispatch a second request.
    var cancellableBCancelled = false
    let expectationB1 = self.expectation(description: "Wait for B started")
    inMemoryRequestTracker.started(
      identifier: "rqB",
      cancellable: AnyCancellable({
        // Note: be careful not to store a cancellable in a variable; that would make the
        // assertion that this object is cancelled (due to being released) unpredictable.
        // See the longer explanation in testDuplicateRequestIdentifierError() for more context.
        cancellableBCancelled = true
      })
    )
    inMemoryRequestTracker.serialQueue.async { expectationB1.fulfill() }
    self.wait(for: [expectationB1], timeout: timeout)
    XCTAssertEqual(inMemoryRequestTracker.count, 1)
    XCTAssertFalse(cancellableBCancelled)
    XCTAssertEqual(insertedIdentifier, "rqB")
    XCTAssertEqual(removedIdentifier, "rqA")
    XCTAssertFalse(errorOccurred)

    let expectationB2 = self.expectation(description: "Wait for B completed")
    inMemoryRequestTracker.completed(identifier: "rqB")
    inMemoryRequestTracker.serialQueue.async { expectationB2.fulfill() }
    self.wait(for: [expectationB2], timeout: timeout)
    XCTAssertEqual(inMemoryRequestTracker.count, 0)

    // When the tracker is no longer retaining the cancellation, it's .cancel() routine should be
    // invoked.
    XCTAssertTrue(cancellableBCancelled)
    XCTAssertEqual(removedIdentifier, "rqB")
    XCTAssertFalse(errorOccurred)
  }

  /// Tests that request limits work correctly.
  func testRequestLimits() {
    let inMemoryRequestTracker = InMemoryRequestTracker<String>(requestLimit: 1)
    XCTAssertEqual(inMemoryRequestTracker.count, 0)
    XCTAssertEqual(inMemoryRequestTracker.requestLimit, 1)

    var cancellableACancelled = false
    var errorOccurred = false
    inMemoryRequestTracker.errorHandler = { error in
      errorOccurred = true
    }
    var insertedIdentifier: String? = nil
    inMemoryRequestTracker.willInsertBlock = { identifier in
      insertedIdentifier = identifier
    }
    var removedIdentifier: String? = nil
    inMemoryRequestTracker.didRemoveBlock = { identifier in
      removedIdentifier = identifier
    }

    let expectationA1 = self.expectation(description: "Wait for A started")
    inMemoryRequestTracker.started(
      identifier: "rqA",
      cancellable: AnyCancellable({
        // Note: be careful not to store a cancellable in a variable; that would make the
        // assertion that this object is cancelled (due to being released) unpredictable.
        // See the longer explanation in testDuplicateRequestIdentifierError() for more context.
        cancellableACancelled = true
      })
    )
    inMemoryRequestTracker.serialQueue.async { expectationA1.fulfill() }
    self.wait(for: [expectationA1], timeout: timeout)
    XCTAssertEqual(inMemoryRequestTracker.count, 1)
    XCTAssertFalse(cancellableACancelled)
    XCTAssertEqual(insertedIdentifier, "rqA")
    XCTAssertNil(removedIdentifier)
    XCTAssertFalse(errorOccurred)

    // Dispatch a second request, which should cause the first request to be cancelled.
    var cancellableBCancelled = false
    let expectationB1 = self.expectation(description: "Wait for B started")
    inMemoryRequestTracker.started(
      identifier: "rqB",
      cancellable: AnyCancellable({
        // Note: be careful not to store a cancellable in a variable; that would make the
        // assertion that this object is cancelled (due to being released) unpredictable.
        // See the longer explanation in testDuplicateRequestIdentifierError() for more context.
        cancellableBCancelled = true
      })
    )
    inMemoryRequestTracker.serialQueue.async { expectationB1.fulfill() }
    self.wait(for: [expectationB1], timeout: timeout)
    XCTAssertEqual(inMemoryRequestTracker.count, 1)
    XCTAssertTrue(cancellableACancelled)
    XCTAssertFalse(cancellableBCancelled)
    XCTAssertEqual(insertedIdentifier, "rqB")
    // The didRemoveBlock should have been called on the first request that got cancelled.
    XCTAssertEqual(removedIdentifier, "rqA")
    XCTAssertFalse(errorOccurred)

    let expectationB2 = self.expectation(description: "Wait for B completed")
    inMemoryRequestTracker.completed(identifier: "rqB")
    inMemoryRequestTracker.serialQueue.async { expectationB2.fulfill() }
    self.wait(for: [expectationB2], timeout: timeout)
    XCTAssertEqual(inMemoryRequestTracker.count, 0)

    // When the tracker is no longer retaining the cancellation, it's .cancel() routine should be
    // invoked.
    XCTAssertTrue(cancellableBCancelled)
    XCTAssertEqual(removedIdentifier, "rqB")
    XCTAssertFalse(errorOccurred)
  }

  /// Tests that two independent requests are tracked correctly.
  func testMultipleDispatches() {
    let inMemoryRequestTracker = InMemoryRequestTracker<String>()
    XCTAssertEqual(inMemoryRequestTracker.count, 0)
    XCTAssertEqual(inMemoryRequestTracker.requestLimit, 0)

    var cancellableACancelled = false
    var cancellableBCancelled = false
    var errorOccurred = false
    inMemoryRequestTracker.errorHandler = { error in
      errorOccurred = true
    }
    var insertedIdentifier: String? = nil
    inMemoryRequestTracker.willInsertBlock = { identifier in
      insertedIdentifier = identifier
    }
    var removedIdentifier: String? = nil
    inMemoryRequestTracker.didRemoveBlock = { identifier in
      removedIdentifier = identifier
    }

    let expectation1a = self.expectation(description: "Wait for started request a")
    inMemoryRequestTracker.started(
      identifier: "ra",
      cancellable: AnyCancellable({
        cancellableACancelled = true
      })
    )
    inMemoryRequestTracker.serialQueue.async { expectation1a.fulfill() }
    self.wait(for: [expectation1a], timeout: timeout)
    XCTAssertEqual(inMemoryRequestTracker.count, 1)
    XCTAssertFalse(cancellableACancelled)
    XCTAssertEqual(insertedIdentifier, "ra")
    XCTAssertNil(removedIdentifier)
    XCTAssertFalse(errorOccurred)

    let expectation1b = self.expectation(description: "Wait for started request b")
    inMemoryRequestTracker.started(
      identifier: "rb",
      cancellable: AnyCancellable({
        cancellableBCancelled = true
      })
    )
    inMemoryRequestTracker.serialQueue.async { expectation1b.fulfill() }
    self.wait(for: [expectation1b], timeout: timeout)
    XCTAssertEqual(inMemoryRequestTracker.count, 2)
    XCTAssertFalse(cancellableBCancelled)
    XCTAssertEqual(insertedIdentifier, "rb")
    XCTAssertNil(removedIdentifier)
    XCTAssertFalse(errorOccurred)

    // Complete the requests in the opposite order.
    let expectation2b = self.expectation(description: "Wait for completed request b")
    inMemoryRequestTracker.completed(identifier: "rb")
    inMemoryRequestTracker.serialQueue.async { expectation2b.fulfill() }
    self.wait(for: [expectation2b], timeout: timeout)
    XCTAssertEqual(inMemoryRequestTracker.count, 1)
    XCTAssertEqual(removedIdentifier, "rb")

    let expectation2a = self.expectation(description: "Wait for completed request a")
    inMemoryRequestTracker.completed(identifier: "ra")
    inMemoryRequestTracker.serialQueue.async { expectation2a.fulfill() }
    self.wait(for: [expectation2a], timeout: timeout)
    XCTAssertEqual(inMemoryRequestTracker.count, 0)
    XCTAssertEqual(removedIdentifier, "ra")

    // When the tracker is no longer retaining the cancellation, it's .cancel() routine should be
    // invoked.
    XCTAssertTrue(cancellableBCancelled)
    XCTAssertTrue(cancellableACancelled)
    XCTAssertFalse(errorOccurred)
  }

  // Tests that the requestTracker can be called from arbitrary queues, and in particular that
  // .started() and .completed() can be called from different queues.
  func testQueueSafety() {
    let inMemoryRequestTracker = InMemoryRequestTracker<String>()
    XCTAssertEqual(inMemoryRequestTracker.count, 0)
    XCTAssertEqual(inMemoryRequestTracker.requestLimit, 0)

    var cancellableCancelled = false
    var errorOccurred = false
    inMemoryRequestTracker.errorHandler = { error in
      errorOccurred = true
    }
    var insertedIdentifier: String? = nil
    inMemoryRequestTracker.willInsertBlock = { identifier in
      insertedIdentifier = identifier
    }
    var removedIdentifier: String? = nil
    inMemoryRequestTracker.didRemoveBlock = { identifier in
      removedIdentifier = identifier
    }

    let expection1 = self.expectation(description: "Wait for call to started")
    let startedQueue = DispatchQueue(label: "com.example.lmfs.driver.ios.tests.started")
    startedQueue.async {
      inMemoryRequestTracker.started(
        identifier: "rq1",
        cancellable: AnyCancellable({
          cancellableCancelled = true
        })
      )
      expection1.fulfill()
    }
    self.wait(for: [expection1], timeout: timeout)

    let expectation2 = self.expectation(description: "Wait for started")
    inMemoryRequestTracker.serialQueue.async { expectation2.fulfill() }
    self.wait(for: [expectation2], timeout: timeout)

    XCTAssertEqual(inMemoryRequestTracker.count, 1)
    XCTAssertFalse(cancellableCancelled)
    XCTAssertEqual(insertedIdentifier, "rq1")
    XCTAssertNil(removedIdentifier)
    XCTAssertFalse(errorOccurred)

    let expectation3 = self.expectation(description: "Wait for dispatch of completed")
    let completedQueue = DispatchQueue(label: "com.example.lmfs.driver.ios.tests.completed")
    completedQueue.async {
      inMemoryRequestTracker.completed(identifier: "rq1")
      expectation3.fulfill()
    }
    self.wait(for: [expectation3], timeout: timeout)

    let expectation4 = self.expectation(description: "Wait for completed")
    inMemoryRequestTracker.serialQueue.async { expectation4.fulfill() }
    self.wait(for: [expectation4], timeout: timeout)

    XCTAssertEqual(inMemoryRequestTracker.count, 0)
    XCTAssertTrue(cancellableCancelled)
    XCTAssertEqual(removedIdentifier, "rq1")
    XCTAssertFalse(errorOccurred)
  }

  func testDuplicateRequestIdentifierError() {
    let inMemoryRequestTracker = InMemoryRequestTracker<String>()
    XCTAssertEqual(inMemoryRequestTracker.count, 0)
    XCTAssertEqual(inMemoryRequestTracker.requestLimit, 0)

    var request1Cancelled = false
    var request2Cancelled = false
    var errorResult: RequestTrackingError?
    var insertedIdentifier: String? = nil
    inMemoryRequestTracker.willInsertBlock = { identifier in
      insertedIdentifier = identifier
    }
    var removedIdentifier: String? = nil
    inMemoryRequestTracker.didRemoveBlock = { identifier in
      removedIdentifier = identifier
    }

    // This test wants to verify that the tracker does not hold a reference to
    // `cancellableForRequest2`. To make sure the compiler will free that variable before we make
    // the assertion, we construct an inner scope to declare that variable in.
    // Without such a structure, the compiler would be free to release the value anywhere between
    // the last reference to the value and the end of the function, which would make the assertion
    // that it was unreleased unpredictable.
    func innerTest() {
      inMemoryRequestTracker.errorHandler = { error in
        errorResult = error
      }

      let expectation1 = self.expectation(description: "Wait for started1")
      inMemoryRequestTracker.started(
        identifier: "rq1",
        cancellable: AnyCancellable({
          request1Cancelled = true
        })
      )
      inMemoryRequestTracker.serialQueue.async { expectation1.fulfill() }
      self.wait(for: [expectation1], timeout: timeout)
      XCTAssertEqual(inMemoryRequestTracker.count, 1)
      XCTAssertEqual(insertedIdentifier, "rq1")
      XCTAssertNil(removedIdentifier)
      XCTAssertNil(errorResult)

      let expectation2 = self.expectation(description: "Wait for started2")
      let cancellableForRequest2 = AnyCancellable({
        request2Cancelled = true
      })
      inMemoryRequestTracker.started(identifier: "rq1", cancellable: cancellableForRequest2)
      inMemoryRequestTracker.serialQueue.async { expectation2.fulfill() }
      self.wait(for: [expectation2], timeout: timeout)
      XCTAssertEqual(inMemoryRequestTracker.count, 1)
      XCTAssertEqual(errorResult, .duplicateRequestIdentifier("rq1", cancellableForRequest2))
      // Clear our local variable so we don't hold a reference to cancellable2.
      errorResult = nil
      // The error case should not have called either block, so they should have the same values
      // as before the second .started() call.
      XCTAssertEqual(insertedIdentifier, "rq1")
      XCTAssertNil(removedIdentifier)
    }
    innerTest()

    XCTAssertFalse(request1Cancelled)
    XCTAssertTrue(request2Cancelled)
  }

  func testUnknownIdentifierError() {
    let inMemoryRequestTracker = InMemoryRequestTracker<String>()
    XCTAssertEqual(inMemoryRequestTracker.count, 0)
    XCTAssertEqual(inMemoryRequestTracker.requestLimit, 0)

    var errorResult: RequestTrackingError?
    inMemoryRequestTracker.errorHandler = { error in
      errorResult = error
    }
    var insertedIdentifier: String? = nil
    inMemoryRequestTracker.willInsertBlock = { identifier in
      insertedIdentifier = identifier
    }
    var removedIdentifier: String? = nil
    inMemoryRequestTracker.didRemoveBlock = { identifier in
      removedIdentifier = identifier
    }

    let expectation1 = self.expectation(description: "Wait for completed")
    inMemoryRequestTracker.completed(identifier: "rq1")
    inMemoryRequestTracker.serialQueue.async { expectation1.fulfill() }
    self.wait(for: [expectation1], timeout: timeout)
    XCTAssertEqual(inMemoryRequestTracker.count, 0)
    XCTAssertEqual(errorResult, .unknownIdentifier("rq1"))
    XCTAssertNil(insertedIdentifier)
    XCTAssertNil(removedIdentifier)
  }
}
