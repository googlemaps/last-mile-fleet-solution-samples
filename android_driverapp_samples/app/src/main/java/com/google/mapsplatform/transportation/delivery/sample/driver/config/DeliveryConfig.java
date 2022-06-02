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
package com.google.mapsplatform.transportation.delivery.sample.driver.config;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.mapsplatform.transportation.delivery.sample.driver.domain.task.DeliveryTaskConfig;
import com.google.mapsplatform.transportation.delivery.sample.driver.domain.vehicle.DeliveryVehicleConfig;
import com.google.mapsplatform.transportation.delivery.sample.driver.domain.vehicle.VehicleStopConfig;

/** Represents a delivery configuration object used to initialize the Delivery test app. */
@AutoValue
public abstract class DeliveryConfig {

  /** The delivery vehicle being tracked. */
  public abstract DeliveryVehicleConfig vehicle();

  @SerializedName("remaining_stop_id_list")
  public abstract ImmutableList<String> remainingStopIdList();

  /** A list of individual tasks belonging to the delivery. */
  @SerializedName("tasks")
  public abstract ImmutableList<DeliveryTaskConfig> deliveryTasks();

  /** A list of individual stops a vehicle will perform during the delivery. */
  @SerializedName("stops")
  public abstract ImmutableList<VehicleStopConfig> vehicleStops();

  /** Whether or not the IDs on the JSON file should be made unique when created on Fleet Engine. */
  @SerializedName("unique_ids")
  public abstract boolean uniqueIds();

  public static Builder builder() {
    return new AutoValue_DeliveryConfig.Builder().setUniqueIds(false);
  }

  public static TypeAdapter<DeliveryConfig> typeAdapter(Gson gson) {
    return new AutoValue_DeliveryConfig.GsonTypeAdapter(gson);
  }

  /** Builder for DeliveryConfig class. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setVehicle(DeliveryVehicleConfig value);

    public abstract Builder setRemainingStopIdList(ImmutableList<String> value);

    public abstract Builder setDeliveryTasks(ImmutableList<DeliveryTaskConfig> value);

    public abstract Builder setVehicleStops(ImmutableList<VehicleStopConfig> value);

    public abstract Builder setUniqueIds(boolean uniqueIds);

    public abstract DeliveryConfig build();
  }
}
