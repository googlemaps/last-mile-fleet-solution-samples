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
package com.example.backend.utils;

import com.google.auto.value.AutoValue;

/** Holds Properties used in the Backend. */
@AutoValue
public abstract class BackendProperties {

  public static BackendProperties create(
      String providerId,
      String fleetEngineAddress,
      String serverServiceAccountEmail,
      String driverServiceAccountEmail,
      String consumerServiceAccountEmail,
      String fleetReaderServiceAccountEmail,
      String apiKey,
      String backendHost) {
    return new AutoValue_BackendProperties(
        providerId,
        fleetEngineAddress,
        serverServiceAccountEmail,
        driverServiceAccountEmail,
        consumerServiceAccountEmail,
        fleetReaderServiceAccountEmail,
        apiKey,
        backendHost);
  }

  public abstract String providerId();

  public abstract String fleetEngineAddress();

  public abstract String serverServiceAccountEmail();

  public abstract String driverServiceAccountEmail();

  public abstract String consumerServiceAccountEmail();

  public abstract String fleetReaderServiceAccountEmail();

  public abstract String apiKey();

  public abstract String backendHost();
}
