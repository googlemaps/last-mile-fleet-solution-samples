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
package com.google.mapsplatform.transportation.delivery.sample.driver.auth;

import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.AuthTokenContext.AuthTokenFactory;
import com.google.common.base.VerifyException;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mapsplatform.transportation.delivery.sample.driver.backend.DeliveryBackend;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Provides signed JWTs (JSON Web Tokens) to enable communication with Fleet Engine. */
public final class DeliveryDriverTokenFactory {

  private static final Logger logger = Logger.getLogger(DeliveryDriverTokenFactory.class.getName());

  private DeliveryDriverTokenFactory() {}

  /**
   * Provides an authentication factory for delivery vehicles.
   *
   * @param backend Service providing a driver token.
   */
  public static final AuthTokenFactory getInstance(DeliveryBackend backend) {

    return context -> {
      ListenableFuture<String> future = backend.getDriverToken(context.getVehicleId());

      try {
        // Note that the below call to future.get is a blocking call. However we expect the token
        // factory to be called internally by Driver SDK, which will run this code in a separate
        // thread, avoiding the UI main thread to be blocked.
        String token = future.get();
        logger.info(String.format("Auth token retrieved: %s", token));
        return token;
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        logger.log(Level.SEVERE, "Thread was interrupted.", e);
      } catch (Exception e) {
        logger.log(Level.SEVERE, "Error retrieving Driver token.", e);
      }
      throw new VerifyException("Could not retrieve token from backend.");
    };
  }
}
