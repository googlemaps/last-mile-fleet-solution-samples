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
package com.example.backend;

import com.example.backend.auth.grpcservice.GrpcServiceModule;
import com.google.inject.servlet.ServletModule;

/** Module for configuring routes for this sample backend. */
public final class SampleServletModule extends ServletModule {

  @Override
  protected void configureServlets() {
    super.configureServlets();
    install(new GrpcServiceModule());
    serve("/token/*").with(TokenServlet.class);
    serve("/tasks", "/task/*", "/taskInfoByTrackingId/*").with(TaskServlet.class);
    serve("/delivery_vehicle/*").with(DeliveryVehicleServlet.class);
    serve("/backend_config").with(BackendConfigServlet.class);
    serve("/manifest", "/manifest/*").with(ManifestServlet.class);
    serve("/config.js").with(JavaScriptConfigServlet.class);
  }
}
