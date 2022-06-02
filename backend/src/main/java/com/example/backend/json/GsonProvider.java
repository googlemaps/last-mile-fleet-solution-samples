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
import com.google.gson.GsonBuilder;
import com.google.type.LatLng;
import google.maps.fleetengine.delivery.v1.DeliveryVehicle;
import google.maps.fleetengine.delivery.v1.Task;

/**
 * Provider for Gson after setting custom serializer and deserializer.
 *
 * <p>Registers the following serializers and deserializers:
 *
 * <ul>
 *   <li>LatLngDeserializer for deserializing LatLng protos
 *   <li>TaskSerializer for serializing Task protos
 *   <li>DeliveryVehicleSerializer for serializing Fleet Engine delivery vehicles
 * </ul>
 */
public final class GsonProvider {

  private static Gson gson;

  /**
   * Returns the created Gson if it was already created before. Otherwise, create a new one with
   * serializers and deserializers.
   */
  public static Gson get() {
    if (gson != null) {
      return gson;
    }

    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder
        .registerTypeAdapter(LatLng.class, new LatLngDeserializer())
        .registerTypeAdapter(Task.class, new TaskSerializer())
        .registerTypeAdapter(DeliveryVehicle.class, new DeliveryVehicleSerializer())
        .setPrettyPrinting();
    gson = gsonBuilder.create();
    return gson;
  }

  private GsonProvider() {}
}
