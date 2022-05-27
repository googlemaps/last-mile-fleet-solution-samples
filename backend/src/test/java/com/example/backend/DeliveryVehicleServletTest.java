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

/** Tests for exercising the delivery vehicle endpoint. */
@RunWith(JUnit4.class)
public class DeliveryVehicleServletTest {

  static DeliveryVehicleServlet servlet;
  static Logger logger = Logger.getLogger(DeliveryVehicleServletTest.class.getName());
  static MockHttpServletRequest request;
  static MockHttpServletResponse response;

  @BeforeClass
  public static void setUpInjectionAndBackendConfig() throws ServletException, IOException {

    // Set up the BackendConfig and pre-load it with vehicles and tasks.
    TestBackendConfigServletProvider testBackendConfigServletProvider =
        TestBackendConfigServletProvider.get();
    servlet =
        new DeliveryVehicleServlet(
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
    request.setServletPath("/delivery_vehicle");
    servlet.service(request, response);
    assertThat(response.getStatus()).isEqualTo(400);
  }

  @Test
  public void stopsGetWhenVehicleIdIsInvalid() throws ServletException, IOException {
    request.setMethod("GET");
    request.setServletPath("/delivery_vehicle");
    request.setPathInfo("/invalid_vehicle_id");
    servlet.service(request, response);
    assertThat(response.getStatus()).isEqualTo(404);
  }

  @Test
  public void stopsPostWhenNoVehicleIdIsSupplied() throws ServletException, IOException {
    request.setMethod("POST");
    request.setServletPath("/delivery_vehicle");
    servlet.service(request, response);
    assertThat(response.getStatus()).isEqualTo(400);
  }

  @Test
  public void stopPostWhenVehicleIdIsInvalid() throws ServletException, IOException {
    request.setMethod("POST");
    request.setServletPath("/delivery_vehicle");
    request.setPathInfo("/fake_vehicle_id");
    servlet.service(request, response);
    assertThat(response.getStatus()).isEqualTo(404);
  }
}
