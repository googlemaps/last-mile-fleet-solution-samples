/* Copyright 2020 Google LLC
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
package com.example.backend.utils;

public final class VehicleUtils {
  public static final String BACKEND_NAME =
      String.format("providers/%s", SampleBackendUtils.backendProperties.providerId());
  public static final String VEHICLE_NAME_FORMAT = BACKEND_NAME + "/deliveryVehicles/%s";

  private VehicleUtils() {}

  public static String getVehicleNameFromId(String vehicleId) {
    return String.format(VEHICLE_NAME_FORMAT, vehicleId);
  }

  public static String getRawVehicleId(String vehicleId) {
    // Assume vehicle ID has _<timestamp> appended. Remove this and return the rest.
    int lastUnderscoreIndex = vehicleId.lastIndexOf("_");

    // There should be at least one character before the last underscore. Otherwise it's an error.
    if (lastUnderscoreIndex < 1) {
      return null;
    }
    return vehicleId.substring(0, lastUnderscoreIndex);
  }
}
