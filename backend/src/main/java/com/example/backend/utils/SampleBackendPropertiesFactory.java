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

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Factory class for BackendProperties. */
public final class SampleBackendPropertiesFactory {

  @VisibleForTesting static final String FLEET_ENGINE_ADDRESS_PROP_KEY = "fleetengine-address";

  @VisibleForTesting static final String PROVIDER_ID_PROP_KEY = "provider-id";

  @VisibleForTesting
  static final String SERVER_SERVICE_ACCOUNT_EMAIL_PROP_KEY = "server-service-account-email";

  @VisibleForTesting
  static final String DRIVER_SERVICE_ACCOUNT_EMAIL_PROP_KEY = "driver-service-account-email";

  @VisibleForTesting
  static final String CONSUMER_SERVICE_ACCOUNT_EMAIL_PROP_KEY = "consumer-service-account-email";

  @VisibleForTesting
  static final String FLEET_READER_SERVICE_ACCOUNT_EMAIL_PROP_KEY =
      "fleet-reader-service-account-email";

  @VisibleForTesting static final String API_KEY_PROP_KEY = "api-key";

  @VisibleForTesting static final String BACKEND_HOST_PROP_KEY = "backend-host";

  private SampleBackendPropertiesFactory() {}

  /** Creates {@code BackendProperties} from params. */
  public static BackendProperties create(
      final String providerId,
      final String fleetEngineAddress,
      final String serverServiceAccountEmail,
      final String driverServiceAccountEmail,
      final String consumerServiceAccountEmail,
      final String fleetReaderServiceAccountEmail,
      final String apiKey,
      final String backendHost) {
    return BackendProperties.create(
        providerId,
        fleetEngineAddress,
        serverServiceAccountEmail,
        driverServiceAccountEmail,
        consumerServiceAccountEmail,
        fleetReaderServiceAccountEmail,
        apiKey,
        backendHost);
  }

  /**
   * Creates {@code BackendProperties} from properties in {@code InputStream}.
   *
   * @throws IOException if an error occurs when writing stream to {@code backendProperties}
   */
  public static BackendProperties create(InputStream stream) throws IOException {
    Properties properties = loadPropertiesFromInputStream(stream);
    return create(
        getPropertyFromKey(properties, PROVIDER_ID_PROP_KEY),
        getPropertyFromKey(properties, FLEET_ENGINE_ADDRESS_PROP_KEY),
        getPropertyFromKey(properties, SERVER_SERVICE_ACCOUNT_EMAIL_PROP_KEY),
        getPropertyFromKey(properties, DRIVER_SERVICE_ACCOUNT_EMAIL_PROP_KEY),
        getPropertyFromKey(properties, CONSUMER_SERVICE_ACCOUNT_EMAIL_PROP_KEY),
        getPropertyFromKey(properties, FLEET_READER_SERVICE_ACCOUNT_EMAIL_PROP_KEY),
        getPropertyFromKey(properties, API_KEY_PROP_KEY),
        getPropertyFromKey(properties, BACKEND_HOST_PROP_KEY));
  }

  /**
   * Loads properties from an {@code InputStream}.
   *
   * @throws IOException if an error occurs when writing stream to {@code backendProperties}
   */
  private static Properties loadPropertiesFromInputStream(InputStream stream) throws IOException {
    Properties properties = new Properties();
    properties.load(stream);
    return properties;
  }

  /**
   * Returns the value for a given property.
   *
   * @throws IllegalArgumentException if a property with given key does not exist
   */
  private static String getPropertyFromKey(Properties properties, String propertyKey) {
    String propertyValue = properties.getProperty(propertyKey);
    if (propertyValue == null) {
      throw new IllegalArgumentException(
          String.format("Could not find expected property '%s'", propertyKey));
    }
    return propertyValue;
  }
}
