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

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Tests for exercising the task endpoint. */
@RunWith(JUnit4.class)
public class TaskServletTest {

  static TestBackendConfigServletProvider testBackendConfigServletProvider;
  static TaskServlet servlet;
  static Logger logger = Logger.getLogger(DeliveryVehicleServletTest.class.getName());
  static MockHttpServletRequest request;
  static MockHttpServletResponse response;

  @BeforeClass
  public static void setUpInjectionAndBackendConfig() throws ServletException, IOException {

    // Set up the BackendConfig and pre-load it with vehicles and tasks.
    testBackendConfigServletProvider = TestBackendConfigServletProvider.get();
    servlet =
        new TaskServlet(
            testBackendConfigServletProvider.servletState,
            testBackendConfigServletProvider.grpcServiceProvider);
  }

  @Before
  public void setUp() {
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
  }

  @Test
  public void stopsGetWhenNoVehicleIdIsSupplied() throws ServletException, IOException {
    request.setMethod("GET");
    request.setServletPath("/tasks");
    servlet.service(request, response);
    assertThat(response.getStatus()).isEqualTo(400);
  }

  @Test
  public void stopsGetWhenVehicleIdIsInvalid() throws ServletException, IOException {
    request.setMethod("GET");
    request.setServletPath("/tasks");
    request.setQueryString("vehicleId=fake_vehicle_id");
    servlet.service(request, response);
    assertThat(response.getStatus()).isEqualTo(404);
  }

  @Test
  public void fetchesTasks() throws ServletException, IOException {
    request.setMethod("GET");
    request.setServletPath("/tasks");
    logger.info(testBackendConfigServletProvider.vehicleId);
    request.setQueryString(
        String.format("vehicleId=%s", testBackendConfigServletProvider.vehicleId));
    servlet.service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);

    Gson gson = new Gson();
    JsonArray responseJsonArray = gson.fromJson(response.getContentAsString(), JsonArray.class);
    assertThat(responseJsonArray).isNotEmpty();
  }

  @Test
  public void stopsPostWhenNoTaskIdIsSpecified() throws ServletException, IOException {
    request.setMethod("POST");
    request.setServletPath("/task");
    request.setPathInfo("/" + testBackendConfigServletProvider.taskId);
    request.setContent("{\"task_outcome\"=\"SUCCEEDED\"}".getBytes(UTF_8));
    servlet.service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
  }
}
