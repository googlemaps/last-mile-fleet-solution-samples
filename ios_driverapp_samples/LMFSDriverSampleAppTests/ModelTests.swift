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

import XCTest

@testable import LMFSDriverSampleApp

class ModelsTests: XCTestCase {

  private var modelData = ModelData()

  override func setUpWithError() throws {
    modelData = ModelData(filename: "test_manifest")
  }

  func testSetTaskStatus() throws {
    let stop1 = modelData.stops[0]
    let tasks = modelData.tasks(stop: stop1)
    XCTAssertEqual(tasks.count, 3)

    XCTAssertEqual(tasks[0].outcome, .pending)
    XCTAssertEqual(tasks[1].outcome, .pending)
    XCTAssertEqual(tasks[2].outcome, .pending)
    XCTAssertEqual(stop1.taskStatus, .pending)
    let navigationState1 = try XCTUnwrap(modelData.navigationState)
    XCTAssertEqual(stop1, navigationState1.upcomingStop)

    modelData.setTaskStatus(task: tasks[0], newStatus: .completed)
    XCTAssertEqual(tasks[0].outcome, .completed)
    modelData.setTaskStatus(task: tasks[1], newStatus: .completed)
    XCTAssertEqual(tasks[1].outcome, .completed)
    modelData.setTaskStatus(task: tasks[2], newStatus: .completed)
    XCTAssertEqual(tasks[2].outcome, .completed)
    XCTAssertEqual(stop1.taskStatus, .completed)
    let navigationState2 = try XCTUnwrap(modelData.navigationState)
    XCTAssertNotEqual(navigationState2.upcomingStop, stop1)

    modelData.setTaskStatus(task: tasks[1], newStatus: .couldNotComplete)
    XCTAssertEqual(tasks[1].outcome, .couldNotComplete)
    XCTAssertEqual(stop1.taskStatus, .couldNotComplete)
  }

  /// Flip the first 2 stops.
  func testMoveStops() throws {
    let stopBeforeMove = modelData.stops[0]
    let indexSet = IndexSet(integer: 0)

    modelData.moveStops(source: indexSet, destination: 2)
    let stopAfterMove = modelData.stops[1]
    XCTAssertEqual(stopBeforeMove, stopAfterMove)
    XCTAssertEqual(stopBeforeMove.order, 2)
    let navigationState = try XCTUnwrap(modelData.navigationState)
    XCTAssertNotEqual(stopBeforeMove, navigationState.upcomingStop)
  }

  // Tests the bug that caused b/221304932: Make sure the `id` property of a ModelData.Stop is
  // globally unique, not just unique within the Manifest/vehicle.
  func testGloballyUniqueStopId() throws {
    let firstStops = Set(modelData.stops.map { $0.id })
    // test_manifest_2.json is identical to test_manifest.json except for vehicle id.
    let modelData2 = ModelData(filename: "test_manifest_2")
    let secondStops = Set(modelData2.stops.map { $0.id })
    XCTAssertTrue(firstStops.intersection(secondStops).isEmpty)
  }
}
