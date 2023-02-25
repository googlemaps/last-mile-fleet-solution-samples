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
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class BackendConfigGsonProvider {

  private static Gson gson;

  public static Gson get() {
    if (gson == null) {
      gson =
          new GsonBuilder()
              .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeTypeConverter())
              .registerTypeAdapter(Duration.class, new DurationTypeConverter())
              .create();
    }
    return gson;
  }

  private static class ZonedDateTimeTypeConverter
      implements JsonSerializer<ZonedDateTime>, JsonDeserializer<ZonedDateTime> {
    @Override
    public JsonElement serialize(
        ZonedDateTime src, Type srcType, JsonSerializationContext context) {
      return new JsonPrimitive(src.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }

    @Override
    public ZonedDateTime deserialize(
        JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
      try {
        return ZonedDateTime.parse(json.getAsString());
      } catch (DateTimeParseException e) {
        throw new JsonParseException(e);
      }
    }
  }

  private static class DurationTypeConverter
      implements JsonSerializer<Duration>, JsonDeserializer<Duration> {
    @Override
    public JsonElement serialize(Duration src, Type srcType, JsonSerializationContext context) {
      return new JsonPrimitive(src.getSeconds());
    }

    @Override
    public Duration deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
      return Duration.ofSeconds(json.getAsLong());
    }
  }

  private BackendConfigGsonProvider() {}
}
