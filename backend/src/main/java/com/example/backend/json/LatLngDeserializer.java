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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.type.LatLng;
import java.lang.reflect.Type;

/** Class to provide deserializing for LatLng through Gson. */
final class LatLngDeserializer implements JsonDeserializer<LatLng> {
  private static final String LATITUDE_KEY = "latitude";
  private static final String LONGITUDE_KEY = "longitude";
  private static final String NOT_PROVIDED_MESSAGE = "%s not provided";

  @Override
  public LatLng deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
    JsonObject jsonObject = json.getAsJsonObject();

    if (!jsonObject.has(LATITUDE_KEY)) {
      String errorMsg = String.format(NOT_PROVIDED_MESSAGE, LATITUDE_KEY);
      throw new JsonParseException(errorMsg);
    }

    if (!jsonObject.has(LONGITUDE_KEY)) {
      String errorMsg = String.format(NOT_PROVIDED_MESSAGE, LONGITUDE_KEY);
      throw new JsonParseException(errorMsg);
    }

    double latitude = jsonObject.get(LATITUDE_KEY).getAsDouble();
    double longitude = jsonObject.get(LONGITUDE_KEY).getAsDouble();
    return LatLng.newBuilder().setLatitude(latitude).setLongitude(longitude).build();
  }
}
