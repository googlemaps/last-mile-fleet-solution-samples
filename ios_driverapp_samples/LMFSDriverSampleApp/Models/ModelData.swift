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
import CoreLocation
import Foundation

/// This class represents the state of the currently loaded trip for this client (if any).
///
/// The trip can be modified by the user's actions during the course of usage.
///
/// The itinerary data is initialized from an immutable Manifest object representing the itinerary,
/// which is read from the backend service. This object and its subobjects refer to the
/// corresponding Manifest objects for all immutable properties.
final class ModelData: ObservableObject {
  /// The list of mutable Stop objects in the current itinerary.
  @Published var stops: [Stop]

  /// This struct contains information to initialize navigation from the current app state.
  struct NavigationState {
    /// The first stop in the itinerary which is not complete.
    let upcomingStop: Stop

    /// The location from which the driver will depart for the upcoming stop.
    let startingLocation: CLLocationCoordinate2D
  }

  /// This property will be non-nil iff navigation is currently possible.
  ///
  /// When navigation is possible, the parameters for navigation can be found here.
  @Published var navigationState: NavigationState? {
    willSet(nextNavigationState) {
      if navigationState?.upcomingStop != nextNavigationState?.upcomingStop {
        currentStopState = .new
      }
    }
  }

  /// The navigation status that the app currently falls in.
  ///
  /// Setting this property will change currentStopState, and might eventually cause requests
  /// to be sent under some circumstances.
  @Published var navigationStatus: NavigationStatus = .notStarted {
    willSet(newNavigationStatus) {
      switch newNavigationStatus {
      case .inProgress:
        currentStopState = .enroute
      case .notStarted:
        currentStopState = .arrived
      default:
        /// Don't need to change currentStopState if the navigation is aborted.
        break
      }
    }
  }

  /// An enum value indicates the state of the driver proceeding to the current stop.
  ///
  /// Note that this is different from navigationStatus. For example, aborting a navigation changes
  /// navigationStatus from .inProgress to .aborted, but currentStopState remains at .enroute
  /// because the driver is still on the way to the upcoming stop.
  private var currentStopState: StopState = .new
  {
    willSet(newStopState) {
      if newStopState == currentStopState || newStopState == .new {
        return
      }
      networkServiceManager.updateStops(
        vehicleId: manifest.vehicle.vehicleId,
        newStopState: newStopState, stops: stops)
    }
  }

  /// The lazily-initialized GMTDVehicleReporterListener implementation for this application.
  ///
  /// This object should be added as a listener to any GMTDVehicleReporters created.
  lazy var vehicleReporterListener = VehicleReporterListener()

  /// The read-only manifest from which the current itinerary was derived.
  ///
  /// In general, the properties of the manifest are not duplicated in the mutable objects. Instead,
  /// the mutable objects refer to the read-only objects.
  @Published var manifest: Manifest

  /// The service for access to tokens fetched from the Backend.
  lazy var accessTokenProvider = AccessTokenProvider()

  /// The service manager which deals with network requests.
  lazy var networkServiceManager: NetworkServiceManager = NetworkServiceManager()

  /// A map of tasks based on their taskId.
  ///
  /// This is needed to resolve the taskIds in a stop to the actual Task object.
  private var tasks: [String: Task]

  enum NavigationStatus: String, Hashable, Codable {
    case notStarted
    /// By default a stop's status is initialized to .notStarted. After the
    /// navigation is completed, it should also be reset to .notStarted in preparation for
    /// the next navigation.
    case inProgress
    /// Navigation is in progress.
    case aborted/// The user has manually aborted the navigation.
  }

  enum StopState: String, Hashable, Codable {
    case arrived = "ARRIVED"
    /// Arrived at the stop.
    case enroute = "ENROUTE"
    /// The navigation to this stop has started and not yet arrived.
    case new = "NEW"/// The StopState is initialized to .new.
  }

  /// Returns the Task objects associated with the given Stop object.
  ///
  /// - Parameter stop: The stop.
  /// - Returns: An array of Task objects associated with the Stop.
  func tasks(stop: Stop) -> [Task] {
    return stop.stopInfo.tasks.compactMap { tasks[$0] }
  }

  /// Moves the stops given to a new location in the stop list and notifies the backend.
  ///
  /// - Parameters:
  ///   - source: An IndexSet giving the indices in the current stops array of the stops to be
  ///   moved.
  ///   - destination: The place in the stops array to move the designated stops to.
  func moveStops(source: IndexSet, destination: Int) {
    stops.move(fromOffsets: source, toOffset: destination)
    updateStopOrderAndStatus()

    networkServiceManager.updateStops(vehicleId: manifest.vehicle.vehicleId, stops: stops)
  }

  /// Sets the status of the given task and reports the update to the backend.
  ///
  /// - Parameters:
  ///   - task: Task to be updated.
  ///   - newStatus: New status for that task.
  func setTaskStatus(task: Task, newStatus: Task.Outcome) {
    if newStatus == task.outcome {
      return
    }
    task.outcome = newStatus
    updateStopOrderAndStatus()
    networkServiceManager.updateTask(task: task)
  }

  /// Initializer for an empty ModelData.
  init() {
    manifest = Manifest()
    stops = []
    tasks = [:]
    updateStopOrderAndStatus()
  }

  /// Initializer for reading ModelData from a file.
  ///
  /// - Parameter filename: Filename from which a JSON manifest should be read.
  ///                       See the LMFS samples apps documentation for format details.
  init(filename: String) {
    do {
      let url = Bundle(for: Self.self).url(forResource: filename, withExtension: "json")!
      let newManifest = try Manifest.loadFrom(url: url)
      manifest = newManifest
      /// Wrap a Task around each Manifest.Task.
      var taskMap = [String: Task]()
      newManifest.tasks.enumerated().forEach { index, manifestTask in
        taskMap[manifestTask.id] = Task(manifestTask, sequence: index)
      }
      tasks = taskMap
      /// Wrap a Stop around each Manifest.Stop according to the order of
      /// remainingStopIdList.
      stops = newManifest.remainingStopIdList.compactMap { stopId in
        newManifest.stops.first(where: { $0.id == stopId })
      }.map { Stop($0, newManifest.vehicle.vehicleId, taskMap: taskMap) }
    } catch {
      fatalError("ModelData cannot be successfully initialized using a filename; " +
                 "\(String(describing: error)).")
    }
    updateStopOrderAndStatus()
  }

  /// Attempts to fetch a manifest from the backend and if successful updates the contents of this
  /// object with it.
  ///
  /// Callers of this function should be observing relevant properties of this object in order to
  /// see changes.
  func assignVehicle(vehicleId: String?) {
    networkServiceManager.fetchManifest(
      clientId: ApplicationDefaults.clientId.value,
      vehicleId: vehicleId
    ) { maybeManifest in
      if let newManifest = maybeManifest {
        self.update(manifest: newManifest)
      }
    }
  }

  /// Replaces the current contents of this object with a new manifest read from the given data.
  ///
  /// - Parameter data: A data object containing a JSON manifest in UTF-8 encoding.
  ///                   See the documentation for the sample backend for details of the JSON format.
  private func update(manifest newManifest: Manifest) {
    manifest = newManifest
    accessTokenProvider.vehicleId = manifest.vehicle.vehicleId
    // Wrap a Task around each Manifest.Task.
    var taskMap = [String: Task]()
    newManifest.tasks.enumerated().forEach { index, manifestTask in
      taskMap[manifestTask.id] = Task(manifestTask, sequence: index)
    }
    tasks = taskMap
    // Wrap a Stop around each Manifest.Stop according to the order of
    // remainingStopIdList.
    stops = newManifest.remainingStopIdList.compactMap { order in
      newManifest.stops.first(where: { $0.id == order })
    }.map { Stop($0, newManifest.vehicle.vehicleId, taskMap: taskMap) }
    updateStopOrderAndStatus()
    // Pre-fetch a token for the new vehicle.
    accessTokenProvider.fetch(nil)
  }

  /// Updates the status property of all stops.
  ///
  /// This updates the taskStatus property of each stop, the order property of each stop, and the
  /// navigationState property of self. It must be called whenever the status of
  /// a task or the order of stops changes.
  private func updateStopOrderAndStatus() {
    var upcomingStop: Stop? = nil
    var startingLocation = self.manifest.vehicle.startLocation.coordinate

    var order: UInt = 1

    var needUpdateStopsRequest = false
    stops.forEach { stop in
      stop.updateTaskStatus()
      if stop.taskStatus != .pending {
        /// Has couldNotComplete or completed stop(s), need to send stops request.
        needUpdateStopsRequest = true
      }

      if upcomingStop == nil {
        if stop.taskStatus == .pending {
          upcomingStop = stop
        } else {
          startingLocation = stop.stopInfo.plannedWaypoint.coordinate
        }
      }
      stop.order = order
      order = order + 1
    }
    if needUpdateStopsRequest {
      networkServiceManager.updateStops(vehicleId: manifest.vehicle.vehicleId, stops: stops)
    }

    if let nonNilUpcomingStop = upcomingStop {
      self.navigationState = NavigationState(
        upcomingStop: nonNilUpcomingStop,
        startingLocation: startingLocation)
    } else {
      self.navigationState = nil
    }
  }

  /// Instances of this class represent a single Stop in the current itinerary.
  class Stop: Identifiable, Hashable, ObservableObject, CustomStringConvertible {
    /// The immutable information about the stop.
    let stopInfo: Manifest.Stop
    /// Identifies the manifest this stop was loaded from.
    let vehicleId: String

    /// The array of task objects associated with this stop.
    let tasks: [Task]

    /// Status of the tasks at this stop.
    ///
    /// This will be .completed iff all tasks were successful.
    /// This will be .pending if any of the tasks was pending.
    /// This will be .couldNotComplete if there was no pending task and any of the tasks could not
    /// complete.
    @Published var taskStatus: TaskStatus

    /// Sequence number of this stop within the trip (1-based). 0 is used to indicate the order
    /// has not been calculated yet.
    @Published var order: UInt

    /// The possible status values for a Stop.
    enum TaskStatus: Int {
      case completed
      /// All tasks completed
      case couldNotComplete
      /// Some tasks could not be completed; all other tasks are complete.
      case pending
      /// One or more tasks are pending.

      static func forTasks(_ tasks: [Task]) -> TaskStatus {
        switch tasks.reduce(Task.Outcome.completed, { $0.combine($1.outcome) }) {
        case .completed:
          return TaskStatus.completed
        case .couldNotComplete:
          return TaskStatus.couldNotComplete
        default:
          return TaskStatus.pending
        }
      }
    }

    /// Recalculates the status of this Stop from the status of its Tasks.
    func updateTaskStatus() {
      taskStatus = TaskStatus.forTasks(tasks)
    }

    /// Return the count of tasks of a given type.
    func taskCount(taskType: Manifest.TaskType) -> Int {
      return tasks.filter { $0.taskInfo.taskType == taskType }.count
    }

    static func == (lhs: ModelData.Stop, rhs: ModelData.Stop) -> Bool {
      lhs.stopInfo.id == rhs.stopInfo.id
    }

    init(_ manifestStop: Manifest.Stop, _ vid: String, taskMap: [String: Task]) {
      stopInfo = manifestStop
      vehicleId = vid
      let taskArray: [Task] = stopInfo.tasks.enumerated().compactMap({ sequence, taskId in
        taskMap[taskId]!.sequence = sequence
        return taskMap[taskId]
      })
      tasks = taskArray
      taskStatus = TaskStatus.forTasks(taskArray)
      order = 0
    }

    /// Returns a globally unique hashable id for this stop. To be globally unique the
    /// vehicle ID needs to be included.
    var id: String {
      return "\(vehicleId)-\(stopInfo.id)"
    }

    func hash(into hasher: inout Hasher) {
      hasher.combine(id)
    }

    var description: String {
      return "ModelData.Stop(\(id))"
    }
  }

  class Task: Identifiable, ObservableObject {
    let taskInfo: Manifest.Task
    /// An arbitrary integer which is assigned consecutively to the Tasks in a Stop.
    var sequence: Int
    @Published var outcome: Outcome

    enum Outcome: Int {
      case pending
      /// This task still needs to be completed.
      case couldNotComplete
      /// This task could not be completed.
      case completed
      /// This task has already been completed.

      /// How to combine the outcome of two Tasks; can be applied iteratively to find the outcome
      /// of a list of tasks.
      func combine(_ other: Outcome) -> Outcome {
        /// If either task is pending, combined status is pending.
        if self == .pending || other == .pending {
          return .pending
        }
        /// If a non-pending tasks couldn't be completed, the combined status is could not complete.
        if self == .couldNotComplete || other == .couldNotComplete {
          return .couldNotComplete
        }
        /// Only if both are completed do we return completed.
        return .completed
      }
    }

    /// Returns whether or not all tasks in the given list are deliveries.
    static func allDeliveries(_ tasks: [Task]) -> Bool {
      tasks.allSatisfy({ $0.taskInfo.taskType == .DELIVERY })
    }

    var actionName: String {
      switch taskInfo.taskType {
      case .DELIVERY:
        return "Mark delivered"
      case .PICKUP:
        return "Mark picked up"
      case .SCHEDULED_STOP:
        return "Mark stop completed"
      case .UNAVAIALBLE:
        return "Invalid"
      }
    }

    var unsuccessfulActionName: String {
      switch taskInfo.taskType {
      case .DELIVERY:
        return "Couldn't deliver"
      case .PICKUP:
        return "Couldn't pick up"
      case .SCHEDULED_STOP:
        return "Couldn't stop"
      case .UNAVAIALBLE:
        return "Invalid"
      }
    }

    /// Sometimes there is a need to designate the tasks within a stop using letter (to avoid
    /// confusion with numbers for stops). We arbitrarily map them to alphabetic characters.
    static private let sequenceOptions = Array("ABCDEFGHIJKLMNOIPQRSTUVWXYZ")
    var sequenceString: String {
      let safeIndex = min(sequence, Task.sequenceOptions.count - 1)
      return String(Task.sequenceOptions[safeIndex])
    }

    init(_ manifestTask: Manifest.Task, sequence: Int) {
      taskInfo = manifestTask
      self.sequence = sequence
      outcome = .pending
    }

    var id: String {
      return taskInfo.id
    }
  }
}
