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

/**
 * Task object to serialize to clients. This object will only contain relevant task information to
 * send to the clients.
 */
@AutoValue
abstract class SerializedTask {

  /** Name of the task. */
  abstract String name();

  /** Type of the task. */
  abstract String type();

  /** State of the task. */
  abstract String state();

  /** Outcome of the task. */
  abstract String taskOutcome();

  /** ID of the vehicle serving this task. */
  abstract String deliveryVehicleId();

  /** Tracking ID for the task. */
  abstract String trackingId();

  /** Location where the task is to be completed. */
  abstract SerializedLocation plannedLocation();

  static Builder newBuilder() {
    return new AutoValue_SerializedTask.Builder();
  }

  //   /** Segments. To be implemented later. */
  //   abstract ImmutableList<VehicleJourneySegment> segments();

  @AutoValue.Builder
  abstract static class Builder {
    abstract Builder setName(String name);

    abstract Builder setType(String type);

    abstract Builder setState(String state);

    abstract Builder setTaskOutcome(String taskOutcome);

    abstract Builder setDeliveryVehicleId(String deliveryVehicleId);

    abstract Builder setTrackingId(String trackingId);

    abstract Builder setPlannedLocation(SerializedLocation plannedLocation);

    abstract SerializedTask build();
  }
}
