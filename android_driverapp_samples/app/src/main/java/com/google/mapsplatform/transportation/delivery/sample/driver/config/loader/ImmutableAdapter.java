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
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.logging.Logger;

/** Utility class to allow deserializing {@link ImmutableList} through GSON. */
final class ImmutableAdapter implements JsonDeserializer<ImmutableList<?>> {

  /**
   * Creates a deserializer for JSON list values into {@link ImmutableList}.
   *
   * <p>This method extracts lists into temporary ordinary List objects and create a shallow copy of
   * it into an instance of ImmutableList.
   */
  @Override
  public ImmutableList<?> deserialize(
      JsonElement json, Type type, JsonDeserializationContext context) {
    if (!(type instanceof ParameterizedType)) {
      throw new AssertionError("Type not supported. Use a ParametrizedType.");
    }

    List<?> list =
        context.deserialize(
            json,
            TypeToken.getParameterized(
                    List.class, ((ParameterizedType) type).getActualTypeArguments()[0])
                .getType());

    return ImmutableList.copyOf(list);
  }
}
