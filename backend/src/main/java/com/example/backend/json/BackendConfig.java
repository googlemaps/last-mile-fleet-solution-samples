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
package com.example.backend.json;

import com.google.gson.annotations.SerializedName;
import java.time.Duration;
import java.time.ZonedDateTime;

public final class BackendConfig {

  public String description;
  public Manifest[] manifests;

  BackendConfig() {}

  public enum StopState {
    @SerializedName("STATE_UNSPECIFIED")
    STATE_UNSPECIFIED("STATE_UNSPECIFIED"),

    @SerializedName("NEW")
    NEW("NEW"),

    @SerializedName("ENROUTE")
    ENROUTE("ENROUTE"),

    @SerializedName("ARRIVED")
    ARRIVED("ARRIVED");

    private final String state;

    public String getValue() {
      return this.state;
    }

    private StopState(String state) {
      this.state = state;
    }

    public static StopState of(String name) {
      for (StopState state : values()) {
        if (state.getValue().equals(name)) {
          return state;
        }
      }
      return null;
    }
  }

  public static class Manifest {
    public Vehicle vehicle;
    public Task[] tasks;
    public Stop[] stops;

    @SerializedName("client_id")
    public String clientId;

    @SerializedName("current_stop_state")
    public StopState currentStopState;

    @SerializedName("remaining_stop_id_list")
    public String[] remainingStopIdList;

    Manifest() {}
  }

  public static class Vehicle {

    @SerializedName("vehicle_id")
    public String vehicleId;

    @SerializedName("provider_id")
    public String providerId;

    @SerializedName("start_location")
    public Waypoint startLocation;

    Vehicle() {}
  }

  public enum TaskType {
    @SerializedName("PICKUP")
    PICKUP("PICKUP"),

    @SerializedName("DELIVERY")
    DELIVERY("DELIVERY"),

    @SerializedName("SCHEDULED_STOP")
    SCHEDULED_STOP("SCHEDULED_STOP"),

    @SerializedName("UNAVAILABLE")
    UNAVAILABLE("UNAVAILABLE");

    private final String type;

    public String getValue() {
      return this.type;
    }

    private TaskType(String type) {
      this.type = type;
    }
  }

  public static class Task {
    @SerializedName("task_id")
    public String taskId;

    @SerializedName("tracking_id")
    public String trackingId;

    @SerializedName("planned_waypoint")
    public Waypoint plannedWaypoint;

    @SerializedName("planned_completion_time")
    public ZonedDateTime plannedCompletionTime;

    @SerializedName("planned_completion_time_range_seconds")
    public Duration plannedCompletionTimeRangeSeconds;

    @SerializedName("duration_seconds")
    public Duration duration;

    @SerializedName("task_type")
    public TaskType type;

    @SerializedName("contact_name")
    public String contactName;

    public String description;

    Task() {}
  }

  public static class Stop {
    @SerializedName("stop_id")
    public String stopId;

    @SerializedName("planned_waypoint")
    public Waypoint plannedWaypoint;

    /** A list of task IDs. */
    public String[] tasks;

    Stop() {}
  }

  public static class Waypoint {
    public String description;
    public double lat;
    public double lng;

    Waypoint() {}
  }
}
