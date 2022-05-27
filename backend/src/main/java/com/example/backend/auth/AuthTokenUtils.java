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
package com.example.backend.auth;

import com.example.backend.utils.SampleBackendUtils;
import com.google.fleetengine.auth.AuthTokenMinter;
import com.google.fleetengine.auth.client.FleetEngineAuthClientInterceptor;
import com.google.fleetengine.auth.token.DeliveryVehicleClaims;
import com.google.fleetengine.auth.token.FleetEngineToken;
import com.google.fleetengine.auth.token.TrackingClaims;
import com.google.fleetengine.auth.token.factory.signer.ImpersonatedSigner;
import com.google.fleetengine.auth.token.factory.signer.SignerInitializationException;
import com.google.fleetengine.auth.token.factory.signer.SigningTokenException;

/** Utility class for authentication. Handles token minting. */
public final class AuthTokenUtils {

  public static final AuthTokenMinter AUTH_TOKEN_MINTER = initializeMinter();

  public static final FleetEngineAuthClientInterceptor AUTH_CLIENT_INTERCEPTOR =
      FleetEngineAuthClientInterceptor.create(AUTH_TOKEN_MINTER);

  /** Initializes the minter. */
  private static final AuthTokenMinter initializeMinter() {
    AuthTokenMinter minter = null;
    try {
      minter =
          AuthTokenMinter.deliveryBuilder()
              .setDeliveryServerSigner(
                  ImpersonatedSigner.create(
                      SampleBackendUtils.backendProperties.serverServiceAccountEmail()))
              .setUntrustedDeliveryDriverSigner(
                  ImpersonatedSigner.create(
                      SampleBackendUtils.backendProperties.driverServiceAccountEmail()))
              .setDeliveryConsumerSigner(
                  ImpersonatedSigner.create(
                      SampleBackendUtils.backendProperties.consumerServiceAccountEmail()))
              .setDeliveryFleetReaderSigner(
                  ImpersonatedSigner.create(
                      SampleBackendUtils.backendProperties.fleetReaderServiceAccountEmail()))
              .build();
    } catch (SignerInitializationException e) {
      // Don't continue if the minter fails to initialize.
      e.printStackTrace();
      System.exit(1);
    }
    return minter;
  }

  /**
   * Convert the FleetEngineToken returned by the minter to the AuthToken format expected by the
   * clients.
   */
  static final AuthToken toAuthToken(FleetEngineToken fleetEngineToken) {
    return AuthToken.builder()
        .setCreationTimestampMs(fleetEngineToken.creationTimestamp().getTime())
        .setExpirationTimestampMs(fleetEngineToken.expirationTimestamp().getTime())
        .setToken(fleetEngineToken.jwt())
        .build();
  }

  /** Returns an existing or newly minted server token. */
  public static final AuthToken getServerToken() throws SigningTokenException {
    return toAuthToken(AUTH_TOKEN_MINTER.getDeliveryServerToken());
  }

  /**
   * Returns a new consumer token with tracking id as given. This function does not cache minted
   * tokens.
   */
  public static final AuthToken getDeliveryConsumerToken(String id) throws SigningTokenException {
    return toAuthToken(AUTH_TOKEN_MINTER.getDeliveryConsumerToken(TrackingClaims.create(id)));
  }

  /**
   * Returns a new untrusted driver token with vehicle id as given. This function does not cache
   * minted tokens.
   */
  public static final AuthToken getDeliveryDriverToken(String id) throws SigningTokenException {
    return toAuthToken(
        AUTH_TOKEN_MINTER.getUntrustedDeliveryVehicleToken(DeliveryVehicleClaims.create(id)));
  }

  /** Returns a new fleet reader token. This function does not cache minted tokens. */
  public static final AuthToken getDeliveryFleetReaderToken() throws SigningTokenException {
    return toAuthToken(AUTH_TOKEN_MINTER.getDeliveryFleetReaderToken());
  }

  private AuthTokenUtils() {}
}
