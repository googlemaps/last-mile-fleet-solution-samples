/* Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.backend.utils;

import com.example.backend.json.BackendConfig;
import com.google.protobuf.Duration;
import com.google.type.LatLng;
import google.maps.fleetengine.delivery.v1.LocationInfo;
import google.maps.fleetengine.delivery.v1.Task;
import google.maps.fleetengine.delivery.v1.VehicleJourneySegment;
import google.maps.fleetengine.delivery.v1.VehicleStop;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A collection of utilities used by the backend config endpoint. Includes support for
 * creating/updating Task and DeliveryVehicle protos from BackendConfig equivalents. These protos
 * are used to communicate with Fleet Engine.
 */
public final class BackendConfigUtils {

  private static final String PROVIDER_ID = SampleBackendUtils.backendProperties.providerId();
  public static final String PARENT = "providers/" + PROVIDER_ID;

  private static long timestamp = 0;

  public static final void setTimestamp(long ts) {
    timestamp = ts;
  }

  public static final Task createTask(BackendConfig.Task t) {
    // Create the task.
    return Task.newBuilder()
        .setName(getTaskName(t.taskId))
        .setType(getTaskType(t.type))
        .setState(Task.State.OPEN)
        .setTaskOutcome(Task.TaskOutcome.TASK_OUTCOME_UNSPECIFIED)
        .setTrackingId(t.trackingId)
        .setPlannedLocation(createLocationInfo(t.plannedWaypoint))
        .setTaskDuration(createDuration(t.duration))
        .build();
  }

  public static final List<VehicleJourneySegment> createVehicleJourneySegments(
      BackendConfig.Manifest m) throws BackendConfigException {

    ArrayList<VehicleJourneySegment> vehicleJourneySegments = new ArrayList<>();
    HashMap<String, BackendConfig.Task> tasksMap = new HashMap<>();
    for (BackendConfig.Task task : m.tasks) {
      tasksMap.put(task.taskId, task);
    }

    for (BackendConfig.Stop stop : m.stops) {
      VehicleStop.Builder stopBuilder =
          VehicleStop.newBuilder()
              .setPlannedLocation(createLocationInfo(stop.plannedWaypoint))
              .setState(VehicleStop.State.NEW);
      for (String taskId : stop.tasks) {
        if (tasksMap.containsKey(taskId)) {
          stopBuilder.addTasks(
              VehicleStop.TaskInfo.newBuilder()
                  .setTaskId(taskId)
                  .setTaskDuration(createDuration(tasksMap.get(taskId).duration)));
        } else {
          throw new BackendConfigException(
              String.format("Task ID %s cannot be found in the list of tasks.", taskId));
        }
      }
      vehicleJourneySegments.add(VehicleJourneySegment.newBuilder().setStop(stopBuilder).build());
    }
    return vehicleJourneySegments;
  }

  public static final String getTimestampedId(String id) {
    return id + (timestamp > 0 ? "_" + String.valueOf(timestamp) : "");
  }

  public static final String getTaskName(String taskId) {
    return PARENT + "/tasks/" + taskId;
  }

  public static final String getDeliveryVehicleName(String deliveryVehicleId) {
    return PARENT + "/deliveryVehicles/" + deliveryVehicleId;
  }

  private static final LocationInfo createLocationInfo(BackendConfig.Waypoint w) {
    return LocationInfo.newBuilder()
        .setPoint(LatLng.newBuilder().setLatitude(w.lat).setLongitude(w.lng))
        .build();
  }

  private static final Duration createDuration(java.time.Duration d) {
    return Duration.newBuilder().setSeconds(d.getSeconds()).build();
  }

  private static final Task.Type getTaskType(BackendConfig.TaskType type) {
    switch (type) {
      case PICKUP:
        return Task.Type.PICKUP;
      case DELIVERY:
        return Task.Type.DELIVERY;
      default:
        return Task.Type.TYPE_UNSPECIFIED;
    }
  }

  private BackendConfigUtils() {}
}
