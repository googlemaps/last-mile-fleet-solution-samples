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
package com.google.mapsplatform.transportation.delivery.sample.driver.domain.task;

import androidx.annotation.Nullable;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.mapsplatform.transportation.delivery.sample.driver.domain.common.WaypointConfig;

/** Representation of delivery a tracked action. */
@AutoValue
public abstract class DeliveryTaskConfig {

  @SerializedName("task_id")
  public abstract String taskId();

  @Nullable
  @SerializedName("tracking_id")
  public abstract String trackingId();

  @SerializedName("planned_waypoint")
  public abstract WaypointConfig plannedWaypoint();

  @SerializedName("duration_seconds")
  public abstract long durationSeconds();

  @SerializedName("task_type")
  public abstract TaskType taskType();

  @Nullable
  @SerializedName("task_outcome")
  public abstract TaskOutcome taskOutcome();

  @Nullable
  @SerializedName("contact_name")
  public abstract String contactName();

  public static Builder builder() {
    return new AutoValue_DeliveryTaskConfig.Builder();
  }

  public static TypeAdapter<DeliveryTaskConfig> typeAdapter(Gson gson) {
    return new AutoValue_DeliveryTaskConfig.GsonTypeAdapter(gson);
  }

  /** Builder class for DeliveryTask. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setTaskId(String value);

    public abstract Builder setTrackingId(@Nullable String value);

    public abstract Builder setPlannedWaypoint(WaypointConfig value);

    public abstract Builder setDurationSeconds(long value);

    public abstract Builder setTaskType(TaskType value);

    public abstract Builder setTaskOutcome(TaskOutcome value);

    public abstract Builder setContactName(@Nullable String value);

    public abstract DeliveryTaskConfig build();
  }
}
