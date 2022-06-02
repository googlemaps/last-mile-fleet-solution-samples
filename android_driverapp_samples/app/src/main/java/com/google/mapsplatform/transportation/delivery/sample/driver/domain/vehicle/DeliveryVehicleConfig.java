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

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.mapsplatform.transportation.delivery.sample.driver.domain.common.WaypointConfig;
import javax.annotation.Nullable;

/** Representation of a delivery vehicle configuration. */
@AutoValue
public abstract class DeliveryVehicleConfig {

  @SerializedName("vehicle_id")
  public abstract String vehicleId();

  @SerializedName("provider_id")
  public abstract String providerId();

  @SerializedName("start_location")
  @Nullable
  public abstract WaypointConfig startLocation();

  public static Builder builder() {
    return new AutoValue_DeliveryVehicleConfig.Builder();
  }

  public static TypeAdapter<DeliveryVehicleConfig> typeAdapter(Gson gson) {
    return new AutoValue_DeliveryVehicleConfig.GsonTypeAdapter(gson);
  }

  /** Builder for DeliveryVehicle class. */
  @AutoValue.Builder
  public abstract static class Builder {

    /**
     * Sets the delivery vehicle id.
     *
     * @param value Vehicle id.
     */
    public abstract Builder setVehicleId(String value);

    /**
     * Sets the provider id.
     *
     * @param value Provider id.
     */
    public abstract Builder setProviderId(String value);

    /**
     * Sets the start location.
     *
     * @param value The start location.
     */
    public abstract Builder setStartLocation(WaypointConfig value);

    public abstract DeliveryVehicleConfig build();
  }
}
