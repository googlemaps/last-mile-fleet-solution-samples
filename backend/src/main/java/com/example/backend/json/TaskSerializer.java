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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import google.maps.fleetengine.delivery.v1.Task;
import java.lang.reflect.Type;

final class TaskSerializer implements JsonSerializer<Task> {
  @Override
  public JsonElement serialize(Task src, Type typeOfSrc, JsonSerializationContext context) {
    SerializedLocation plannedLocation =
        SerializedLocation.newBuilder().setPoint(src.getPlannedLocation().getPoint()).build();
    SerializedTask task =
        SerializedTask.newBuilder()
            .setName(src.getName())
            .setType(src.getType().name())
            .setState(src.getState().name())
            .setTaskOutcome(src.getTaskOutcome().name())
            .setDeliveryVehicleId(src.getDeliveryVehicleId())
            .setTrackingId(src.getTrackingId())
            .setPlannedLocation(plannedLocation)
            .build();
    return new Gson().toJsonTree(task);
  }
}
