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
package com.google.mapsplatform.transportation.delivery.sample.driver.domain.vehicle;

import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.Task;
import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.TaskInfo;
import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.VehicleStop;
import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.VehicleStop.VehicleStopState;
import com.google.android.libraries.navigation.Waypoint;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.List;

/** The SDK's VehicleStop, but augmented with full Task objects instead of TaskInfo */
@AutoValue
public abstract class AppVehicleStop {

  public List<TaskInfo> getTaskInfoList() {
    return Lists.transform(
        getTasks(),
        task ->
            TaskInfo.builder()
                .setTaskId(task.getTaskId())
                .setTaskDurationSeconds(task.getTaskDurationSeconds())
                .build());
  }

  /** Returns the waypoint of the vehicle stop. */
  public abstract Waypoint getWaypoint();

  /** Returns the state of the VehicleStop. */
  @VehicleStopState
  public abstract int getVehicleStopState();

  public abstract List<Task> getTasks();

  /** Returns new Builder instance set with current VehicleStop state. */
  public abstract Builder toBuilder();

  /** Returns new default Builder instance, with the VehicleStop state set to NEW. */
  public static Builder builder() {
    return new com.google.mapsplatform.transportation.delivery.sample.driver.domain.vehicle.AutoValue_AppVehicleStop.Builder()
        .setVehicleStopState(VehicleStopState.NEW);
  }

  // lossy translation - doesn't have tasks.
  public static AppVehicleStop fromVehicleStop(VehicleStop stop) {
    return AppVehicleStop.builder()
        .setWaypoint(stop.getWaypoint())
        .setVehicleStopState(stop.getVehicleStopState())
        .build();
  }

  public static VehicleStop toVehicleStop(AppVehicleStop stop) {
    return VehicleStop.builder()
        .setTaskInfoList(stop.getTaskInfoList())
        .setVehicleStopState(stop.getVehicleStopState())
        .setWaypoint(stop.getWaypoint())
        .build();
  }

  /** Builder class for AppVehicleStop. */
  @AutoValue.Builder
  public abstract static class Builder {

    /** Sets the location that should be navigated to for the stop. */
    public abstract Builder setWaypoint(Waypoint waypoint);

    /** Sets the state of the VehicleStop. */
    public abstract Builder setVehicleStopState(@VehicleStopState int value);

    public abstract Builder setTasks(List<Task> tasks);

    /**
     * Returns new AppVehicleStop instance with the state set by the Builder.
     *
     * @throws NullPointerException if any non-nullable values are not set.
     * @throws IllegalArgumentException if the VehicleStop is set without any TaskInfos.
     */
    public AppVehicleStop build() {
      AppVehicleStop stop = autoBuild();

      Preconditions.checkArgument(!stop.getTaskInfoList().isEmpty());

      return stop;
    }

    Builder() {}

    abstract AppVehicleStop autoBuild();
  }
}
