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

import androidx.annotation.Nullable;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.mapsplatform.transportation.delivery.sample.driver.domain.common.WaypointConfig;

/** Representation of a vehicle stop. */
@AutoValue
public abstract class VehicleStopConfig {

  @SerializedName("stop_id")
  public abstract String stopId();

  @SerializedName("planned_waypoint")
  public abstract WaypointConfig plannedWaypoint();

  public abstract ImmutableList<String> tasks();

  @Nullable
  @SerializedName("vehicle_state")
  public abstract VehicleStopState vehicleState();

  public static Builder builder() {
    return new AutoValue_VehicleStopConfig.Builder().setTasks(ImmutableList.of());
  }

  public static TypeAdapter<VehicleStopConfig> typeAdapter(Gson gson) {
    return new AutoValue_VehicleStopConfig.GsonTypeAdapter(gson);
  }

  /** Builder class for VehicleStop. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setStopId(String value);

    public abstract Builder setPlannedWaypoint(WaypointConfig value);

    public abstract Builder setTasks(ImmutableList<String> value);

    public abstract Builder setVehicleState(VehicleStopState value);

    public abstract VehicleStopConfig build();
  }
}
