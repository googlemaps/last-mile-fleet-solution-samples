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

import com.example.backend.auth.grpcservice.AuthenticatedGrpcServiceProvider;
import com.example.backend.auth.grpcservice.GrpcServiceModule;
import com.google.inject.Guice;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Utility class for providing a grpc servlet backend that has already communicated with Fleet
 * Engine to set up vehicles and tasks.
 */
public class TestBackendConfigServletProvider {
  public AuthenticatedGrpcServiceProvider grpcServiceProvider;
  public ServletState servletState;
  public String vehicleId;
  public String taskId;

  private TestBackendConfigServletProvider() {}

  /** Returns a fresh TestBackendConfigServletProvider pre-loaded with the vehicle and tasks. */
  public static TestBackendConfigServletProvider get() throws ServletException, IOException {
    return get("test.json");
  }

  public static TestBackendConfigServletProvider get(String testJsonPath)
      throws ServletException, IOException {
    TestBackendConfigServletProvider backend = new TestBackendConfigServletProvider();
    backend.grpcServiceProvider =
        Guice.createInjector(new GrpcServiceModule())
            .getInstance(AuthenticatedGrpcServiceProvider.class);
    backend.servletState = new ServletState();
    BackendConfigServlet backendConfigServlet =
        new BackendConfigServlet(backend.servletState, backend.grpcServiceProvider);

    // Initial BackendConfig request to add all data to be tested to Fleet Engine.
    MockHttpServletResponse response = new MockHttpServletResponse();
    try (InputStream testStream =
        TestBackendConfigServletProvider.class.getClassLoader().getResourceAsStream(testJsonPath)) {
      backendConfigServlet.serveUpload(testStream, response);
    }

    // Extract the vehicleId, as it may be useful to some tests.
    Pattern vehicleIdPattern = Pattern.compile("vehicle_1_\\d{13}");
    Matcher vehicleIdMatcher = vehicleIdPattern.matcher(response.getContentAsString());
    if (vehicleIdMatcher.find()) {
      backend.vehicleId = vehicleIdMatcher.group(0);
    }

    // Extract one of the taskIds.
    Pattern taskIdPattern = Pattern.compile("task_1_\\d{13}");
    Matcher taskIdMatcher = taskIdPattern.matcher(response.getContentAsString());
    if (taskIdMatcher.find()) {
      backend.taskId = taskIdMatcher.group(0);
    }

    return backend;
  }
}
