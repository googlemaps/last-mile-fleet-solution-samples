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

import com.google.auto.value.AutoValue;

/** Represents a vehicle waypoint. */
@AutoValue
abstract class Waypoint {

  /** Geo location of the waypoint. */
  abstract SerializedLocation location();

  /** Type of waypoint. */
  abstract WaypointType waypointType();

  static Builder newBuilder() {
    return new AutoValue_Waypoint.Builder();
  }

  @AutoValue.Builder
  abstract static class Builder {

    abstract Builder setLocation(SerializedLocation location);
    abstract Builder setWaypointType(WaypointType type);

    abstract Waypoint build();
  }

  /** Type of waypoint. */
  public enum WaypointType {
    PICKUP_WAYPOINT_TYPE,
    DROP_OFF_WAYPOINT_TYPE
  }
}
