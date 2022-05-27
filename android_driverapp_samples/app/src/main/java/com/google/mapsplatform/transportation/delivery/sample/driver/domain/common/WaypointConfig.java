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
package com.google.mapsplatform.transportation.delivery.sample.driver.domain.common;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import javax.annotation.Nullable;

/** Representation of map waypoint. */
@AutoValue
public abstract class WaypointConfig {

  @SerializedName("lat")
  public abstract double latitude();

  @SerializedName("lng")
  public abstract double longitude();

  @SerializedName("description")
  @Nullable
  public abstract String title();

  public static Builder builder() {
    return new AutoValue_WaypointConfig.Builder();
  }

  public static TypeAdapter<WaypointConfig> typeAdapter(Gson gson) {
    return new AutoValue_WaypointConfig.GsonTypeAdapter(gson);
  }

  /** Builder for Waypoint class. */
  @AutoValue.Builder
  public abstract static class Builder {

    /**
     * Sets the waypoint latitude coordinate.
     *
     * @param value latitude.
     */
    public abstract Builder setLatitude(double value);

    /**
     * Sets the waypoint longitude coordinate.
     *
     * @param value longitude.
     */
    public abstract Builder setLongitude(double value);

    /**
     * Sets the waypoint title.
     *
     * @param title Waypoint title.
     */
    public abstract Builder setTitle(String title);

    public abstract WaypointConfig build();
  }
}
