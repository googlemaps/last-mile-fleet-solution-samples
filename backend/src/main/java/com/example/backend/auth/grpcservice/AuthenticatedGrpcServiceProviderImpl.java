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
package com.example.backend.auth.grpcservice;

import com.example.backend.auth.AuthTokenUtils;
import com.example.backend.utils.SampleBackendUtils;
import google.maps.fleetengine.delivery.v1.DeliveryServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;

final class AuthenticatedGrpcServiceProviderImpl implements AuthenticatedGrpcServiceProvider {

  private final ManagedChannel fleetEngineChannel =
      ManagedChannelBuilder.forTarget(SampleBackendUtils.backendProperties.fleetEngineAddress())
          .intercept(AuthTokenUtils.AUTH_CLIENT_INTERCEPTOR)
          .build();

  private final DeliveryServiceGrpc.DeliveryServiceBlockingStub deliveryService =
      DeliveryServiceGrpc.newBlockingStub(fleetEngineChannel);

  @Override
  public DeliveryServiceGrpc.DeliveryServiceBlockingStub getAuthenticatedDeliveryService() {
    DeliveryServiceGrpc.DeliveryServiceBlockingStub authenticatedDeliveryService =
        deliveryService.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(new Metadata()));
    return authenticatedDeliveryService;
  }
}
