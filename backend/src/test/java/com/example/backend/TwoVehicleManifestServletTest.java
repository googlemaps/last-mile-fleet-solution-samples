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
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Tests exercising a manifest with two vehicles. */
@RunWith(JUnit4.class)
public class TwoVehicleManifestServletTest {

  static TestBackendConfigServletProvider testBackendConfigServletProvider;
  static ManifestServlet servlet;

  @BeforeClass
  public static void setUpInjectionAndProvideConfig() throws ServletException, IOException {

    // Set up the BackendConfig and pre-load it with two vehicles and tasks.
    testBackendConfigServletProvider =
        TestBackendConfigServletProvider.get("test-two-vehicles.json");
    servlet =
        new ManifestServlet(
            testBackendConfigServletProvider.servletState,
            testBackendConfigServletProvider.grpcServiceProvider);
  }

  @Test
  public void assignsClientsToTwoVehicles() throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.setMethod("POST");
    request.setServletPath("/manifest");
    request.setPathInfo("/" + testBackendConfigServletProvider.vehicleId);
    request.setContent("{\"client_id\":\"TEST_CLIENT_ID\"}".getBytes(UTF_8));
    servlet.service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);

    // Validate the structure of the data returned.
    Gson gson = new Gson();
    JsonObject manifestJson = gson.fromJson(response.getContentAsString(), JsonObject.class);

    // Verify that the vehicle ID field is present and correct.
    String vehicleId = manifestJson.getAsJsonObject("vehicle").get("vehicle_id").getAsString();
    Pattern vehicleIdPattern = Pattern.compile("^vehicle_1_(\\d{13})$");
    Matcher matcher = vehicleIdPattern.matcher(vehicleId);
    assertThat(matcher.matches()).isTrue();

    // Verify that there is at least one stop and one task.
    String timestamp = matcher.group(1);
    assertThat(manifestJson.getAsJsonArray("tasks")).isNotEmpty();
    assertThat(
            manifestJson
                .getAsJsonArray("tasks")
                .get(0)
                .getAsJsonObject()
                .get("task_id")
                .getAsString())
        .isEqualTo(String.format("vehicle_1_task_1_%s", timestamp));
    assertThat(manifestJson.getAsJsonArray("stops")).isNotEmpty();
    assertThat(
            manifestJson
                .getAsJsonArray("stops")
                .get(0)
                .getAsJsonObject()
                .get("tasks")
                .getAsJsonArray())
        .isNotEmpty();

    // Now do the same with a second client id.
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    request.setMethod("POST");
    request.setServletPath("/manifest");
    request.setContent("{\"client_id\":\"ANOTHER_CLIENT_ID\"}".getBytes(UTF_8));
    servlet.service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);

    manifestJson = gson.fromJson(response.getContentAsString(), JsonObject.class);

    // Verify that the vehicle ID field is present and correct.
    vehicleId = manifestJson.getAsJsonObject("vehicle").get("vehicle_id").getAsString();
    vehicleIdPattern = Pattern.compile("^vehicle_2_(\\d{13})$");
    matcher = vehicleIdPattern.matcher(vehicleId);
    assertThat(matcher.matches()).isTrue();

    // Verify that there is at least one stop and one task.
    timestamp = matcher.group(1);
    assertThat(manifestJson.getAsJsonArray("tasks")).isNotEmpty();
    assertThat(
            manifestJson
                .getAsJsonArray("tasks")
                .get(0)
                .getAsJsonObject()
                .get("task_id")
                .getAsString())
        .isEqualTo(String.format("vehicle_2_task_1_%s", timestamp));
    assertThat(manifestJson.getAsJsonArray("stops")).isNotEmpty();
    assertThat(
            manifestJson
                .getAsJsonArray("stops")
                .get(0)
                .getAsJsonObject()
                .get("tasks")
                .getAsJsonArray())
        .isNotEmpty();
  }
}
