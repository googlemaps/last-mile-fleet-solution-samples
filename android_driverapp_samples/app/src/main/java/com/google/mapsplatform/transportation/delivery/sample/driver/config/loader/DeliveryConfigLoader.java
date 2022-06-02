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
package com.google.mapsplatform.transportation.delivery.sample.driver.config.loader;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.mapsplatform.transportation.delivery.sample.driver.config.DeliveryConfig;
import java.io.IOException;

/**
 * Utility class to load a delivery configuration object.
 */
public final class DeliveryConfigLoader {

  private static final Gson gson =
      new GsonBuilder()
          .registerTypeAdapterFactory(AutoValueAdapterFactory.create())
          .registerTypeAdapter(ImmutableList.class, new ImmutableAdapter())
          .create();

  private DeliveryConfigLoader() {
  }

  /**
   * Loads a delivery configuration ("manifest") from a JSON object.
   *
   * @param jsonObject the JSON object, likely obtained from the backend.
   * @return delivery configuration object.
   * @throws IOException if the JSON object cannot be deserialized.
   */
  public static DeliveryConfig fromJsonObject(JsonObject jsonObject) throws IOException {
    return gson.fromJson(jsonObject, DeliveryConfig.class);
  }
}
