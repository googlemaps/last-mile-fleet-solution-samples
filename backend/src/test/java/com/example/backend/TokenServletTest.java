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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Guice;
import java.io.IOException;
import javax.inject.Inject;
import javax.servlet.ServletException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Tests for exercising the token generation endpoint.
 */
@RunWith(JUnit4.class)
public class TokenServletTest {
  @Inject TokenServlet servlet;

  MockHttpServletRequest request;
  MockHttpServletResponse response;

  @Before
  public void setUp() {
    Guice.createInjector().injectMembers(this);
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
  }

  @Test
  public void stopsIfTokenTypeInvalid() throws ServletException, IOException {
    request.setMethod("GET");
    request.setServletPath("/token");
    request.setPathInfo("/invalid_token_type");
    servlet.service(request, response);
    assertThat(response.getStatus()).isEqualTo(400);
  }

  @Test
  public void stopsIfVehicleIdNotProvidedWhenFetchingDeliveryDriverToken()
      throws ServletException, IOException {
    request.setMethod("GET");
    request.setServletPath("/token");
    request.setPathInfo("/delivery_driver");
    servlet.service(request, response);
    assertThat(response.getStatus()).isEqualTo(400);
  }

  @Test
  public void fetchesDeliveryDriverToken() throws ServletException, IOException {
    request.setMethod("GET");
    request.setServletPath("/token");
    request.setPathInfo("/delivery_driver/fake_vehicle_id");
    servlet.service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);

    // Parse the return to make sure it contains a valid token with a valid lifetime
    long currentTimestamp = java.lang.System.currentTimeMillis();
    Gson gson = new Gson();
    JsonObject responseJsonObject = gson.fromJson(response.getContentAsString(), JsonObject.class);
    assertThat(responseJsonObject.get("token").getAsString()).isNotEmpty();
    assertThat(responseJsonObject.get("creation_timestamp_ms").getAsLong())
        .isLessThan(currentTimestamp);
    assertThat(responseJsonObject.get("expiration_timestamp_ms").getAsLong())
        .isGreaterThan(currentTimestamp);
  }

  @Test
  public void stopsIfVehicleIdNotProvidedWhenFetchingDeliveryConsumerToken()
      throws ServletException, IOException {
    request.setMethod("GET");
    request.setServletPath("/token");
    request.setPathInfo("/delivery_consumer");
    servlet.service(request, response);
    assertThat(response.getStatus()).isEqualTo(400);
  }

  @Test
  public void fetchesDeliveryConsumerToken() throws ServletException, IOException {
    request.setMethod("GET");
    request.setServletPath("/token");
    request.setPathInfo("/delivery_consumer/fake_tracking_id");
    servlet.service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);

    // Parse the return to make sure it contains a valid token with a valid lifetime
    long currentTimestamp = java.lang.System.currentTimeMillis();
    Gson gson = new Gson();
    JsonObject responseJsonObject = gson.fromJson(response.getContentAsString(), JsonObject.class);
    assertThat(responseJsonObject.get("token").getAsString()).isNotEmpty();
    assertThat(responseJsonObject.get("creation_timestamp_ms").getAsLong())
        .isLessThan(currentTimestamp);
    assertThat(responseJsonObject.get("expiration_timestamp_ms").getAsLong())
        .isGreaterThan(currentTimestamp);
  }

  @Test
  public void fetchesFleetReaderToken() throws ServletException, IOException {
    request.setMethod("GET");
    request.setServletPath("/token");
    request.setPathInfo("/fleet_reader");
    servlet.service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);

    // Parse the return to make sure it contains a valid token with a valid lifetime
    long currentTimestamp = java.lang.System.currentTimeMillis();
    Gson gson = new Gson();
    JsonObject responseJsonObject = gson.fromJson(response.getContentAsString(), JsonObject.class);
    assertThat(responseJsonObject.get("token").getAsString()).isNotEmpty();
    assertThat(responseJsonObject.get("creation_timestamp_ms").getAsLong())
        .isLessThan(currentTimestamp);
    assertThat(responseJsonObject.get("expiration_timestamp_ms").getAsLong())
        .isGreaterThan(currentTimestamp);
  }
}
