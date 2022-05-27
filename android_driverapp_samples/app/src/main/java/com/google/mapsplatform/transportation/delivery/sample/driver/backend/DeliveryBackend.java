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
package com.google.mapsplatform.transportation.delivery.sample.driver.backend;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonObject;
import java.util.List;

/**
 * Defines the interface of a Delivery Backend service used by the test app to retrieve external
 * data needed to function. Backends are usually external services reached through RESTful APIs,
 * gRPC requests or other means.
 */
public interface DeliveryBackend {

  /**
   * Returns a signed JSON Web Token (JWT) valid to Fleet Engine calls under the Driver role for the
   * application vehicle.
   *
   * @param vehicleId vehicle id.
   */
  ListenableFuture<String> getDriverToken(String vehicleId);

  /**
   * Updates the next vehicle stop with a given vehicle stop state. Returns true if the operation
   * is succeed.
   *
   * @param state stop state to be used.
   */
  ListenableFuture<Boolean> updateNextStopState(String state);

  /**
   * Updates a task's outcome. Returns true if operation successful.
   * @param taskId ID of the task being closed.
   * @param taskOutcome Outcome of the task being closed.
   */
  ListenableFuture<Boolean> updateTaskOutcome(String taskId, String taskOutcome);

  /**
   * Sets the vehicle ID that the backend will send updates for.
   *
   * @param vehicleId ID of the vehicle.
   */
  void setVehicleId(String vehicleId);

  /**
   * Fetches the manifest for the vehicle from the backend.
   *
   * @return a JSON object that contains the manifest.
   */
  ListenableFuture<JsonObject> getDeliveryConfigJson();

  /**
   * Updates the stop ID list for the vehicle.
   *
   * This is used to mark the current stop as complete, or reorder the stops.
   *
   * To mark the current stop as complete, use a stopIdList that does not include the current stop.
   *
   * To reorder the stops, use a stopIdList that corresponds to the new order of the stops.
   */
  ListenableFuture<Boolean> updateStopIdList(List<String> stopIdList);
}
