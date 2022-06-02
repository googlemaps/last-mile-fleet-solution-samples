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
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Tests for exercising the manifest endpoint.
 *
 * <p>Because the backend config assignment process is stateful, we use a fixed method order.
 */
@RunWith(JUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ManifestServletTest {

  static ManifestServlet servlet;
  static Logger logger = Logger.getLogger(ManifestServletTest.class.getName());
  static MockHttpServletRequest request;
  static MockHttpServletResponse response;
  static TestBackendConfigServletProvider testBackendConfigServletProvider;
  static final Gson gson = new Gson();

  // Keep a copy of the manifest for testing across multiple tests.
  static JsonObject manifestJson;

  // Keep a DeliveryVehicle proto in json format for comparison purposes.
  static HashMap<String, JsonObject> stopIdToProtoStopMap = new HashMap<>();

  @BeforeClass
  public static void setUpInjectionAndProvideConfig() throws ServletException, IOException {

    // Set up the BackendConfig and pre-load it with vehicles and tasks.
    testBackendConfigServletProvider = TestBackendConfigServletProvider.get();
    servlet =
        new ManifestServlet(
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
    request.setServletPath("/manifest");
    servlet.service(request, response);
    assertThat(response.getStatus()).isEqualTo(400);
  }

  @Test
  public void stopsGetWhenVehicleIdIsInvalid() throws ServletException, IOException {
    request.setMethod("GET");
    request.setServletPath("/manifest");
    request.setPathInfo("/invalid_vehicle_id");
    servlet.service(request, response);
    assertThat(response.getStatus()).isEqualTo(404);
  }

  @Test
  public void getReturnsManifest() throws ServletException, IOException {
    request.setMethod("GET");
    request.setServletPath("/manifest");
    request.setPathInfo("/" + testBackendConfigServletProvider.vehicleId);
    servlet.service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    JsonObject responseObject = gson.fromJson(response.getContentAsString(), JsonObject.class);
    assertThat(responseObject.getAsJsonObject("vehicle").get("vehicle_id").getAsString())
        .isEqualTo(testBackendConfigServletProvider.vehicleId);
    assertThat(responseObject.getAsJsonObject("vehicle").get("provider_id").getAsString())
        .isNotEmpty();
  }

  @Test
  public void fixedOrder_01_stopsAssignmentWhenNoClientIdIsSupplied()
      throws ServletException, IOException {
    request.setMethod("POST");
    request.setServletPath("/manifest");
    request.setContent("".getBytes(UTF_8));
    servlet.service(request, response);
    assertThat(response.getStatus()).isEqualTo(400);
  }

  @Test
  public void fixedOrder_02_stopsAssignmentWhenVehicleIdIsInvalid()
      throws ServletException, IOException {
    request.setMethod("POST");
    request.setServletPath("/manifest");
    request.setPathInfo("/INVALID_VEHICLE_ID");
    request.setContent("{\"client_id\":\"TEST_CLIENT_ID\"}".getBytes(UTF_8));
    servlet.service(request, response);
    assertThat(response.getStatus()).isEqualTo(404);
  }

  /**
   * This is a test that exercises most of the functionality in ManifestServlet. Generally such a
   * test is not recommended, as each test should exercise one component or piece of functionality.
   * However, since a vehicle needs to be assigned before its status can be updated, we must
   * exercise the components in sequence.
   *
   * @throws ServletException on servlet errors.
   * @throws IOException on IO errors.
   */
  @Test
  public void fixedOrder_10_assignsClientToVehicle() throws ServletException, IOException {
    request.setMethod("POST");
    request.setServletPath("/manifest");
    request.setContent("{\"client_id\":\"TEST_CLIENT_ID\"}".getBytes(UTF_8));
    servlet.service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);

    // Validate the structure of the data returned.
    Gson gson = new Gson();
    manifestJson = gson.fromJson(response.getContentAsString(), JsonObject.class);

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
        .isEqualTo(String.format("task_1_%s", timestamp));
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

  @Test
  public void fixedOrder_11_stopsAssignmentWhenOtherClientAttemptsToAssignSameVehicle()
      throws ServletException, IOException {
    request.setMethod("POST");
    request.setServletPath("/manifest");
    request.setPathInfo("/" + testBackendConfigServletProvider.vehicleId);
    request.setContent("{\"client_id\":\"OTHER_TEST_CLIENT_ID\"}".getBytes(UTF_8));
    servlet.service(request, response);
    assertThat(response.getStatus()).isEqualTo(403);
  }

  @Test
  public void fixedOrder_12_stopsAssignmentWhenOtherClientAttemptsToAssignAnyOtherVehicle()
      throws ServletException, IOException {
    request.setMethod("POST");
    request.setServletPath("/manifest");
    request.setContent("{\"client_id\":\"OTHER_TEST_CLIENT_ID\"}".getBytes(UTF_8));
    servlet.service(request, response);
    assertThat(response.getStatus()).isEqualTo(404);
  }

  @Test
  public void fixedOrder_13_stopsAssignmentWhenClientAttemptsToAssignAnotherVehicle()
      throws ServletException, IOException {
    request.setMethod("POST");
    request.setServletPath("/manifest");
    request.setPathInfo("/some_other_vehicle_id");
    request.setContent("{\"client_id\":\"TEST_CLIENT_ID\"}".getBytes(UTF_8));
    servlet.service(request, response);
    assertThat(response.getStatus()).isEqualTo(403);
  }

  @Test
  public void fixedOrder_14_returnsAssignmentWhenClientAttemptsToAssignAnyVehicle()
      throws ServletException, IOException {
    request.setMethod("POST");
    request.setServletPath("/manifest");
    request.setContent("{\"client_id\":\"TEST_CLIENT_ID\"}".getBytes(UTF_8));
    servlet.service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    JsonObject responseObject = gson.fromJson(response.getContentAsString(), JsonObject.class);
    assertThat(responseObject.getAsJsonObject("vehicle").get("vehicle_id").getAsString())
        .isEqualTo(testBackendConfigServletProvider.vehicleId);
  }

  @Test
  public void fixedOrder_15_returnsAssignmentWhenClientAttemptsToAssignSameVehicle()
      throws ServletException, IOException {
    request.setMethod("POST");
    request.setServletPath("/manifest");
    request.setPathInfo("/" + testBackendConfigServletProvider.vehicleId);
    request.setContent("{\"client_id\":\"TEST_CLIENT_ID\"}".getBytes(UTF_8));
    servlet.service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    JsonObject responseObject = gson.fromJson(response.getContentAsString(), JsonObject.class);
    assertThat(responseObject.getAsJsonObject("vehicle").get("vehicle_id").getAsString())
        .isEqualTo(testBackendConfigServletProvider.vehicleId);
  }

  @Test
  public void fixedOrder_20_stopOrderIsCorrect() throws ServletException, IOException {
    String[] originalOrder = {"stop_1", "stop_2", "stop_3", "stop_4"};
    JsonArray stopIdListJsonArray = manifestJson.get("remaining_stop_id_list").getAsJsonArray();
    assertThat(originalOrder).hasLength(stopIdListJsonArray.size());
    for (int i = 0; i < originalOrder.length; i++) {
      assertThat(originalOrder[i]).isEqualTo(stopIdListJsonArray.get(i).getAsString());
    }

    // Fetch the actual JSON-encoded protos.
    JsonObject deliveryVehicle = getJsonDeliveryVehicle(testBackendConfigServletProvider.vehicleId);
    for (int i = 0; i < originalOrder.length; i++) {
      JsonObject manifestStop = manifestJson.getAsJsonArray("stops").get(i).getAsJsonObject();
      assertThat(manifestStop.get("stop_id").getAsString()).isEqualTo(originalOrder[i]);
      JsonObject protoStop = getDeliveryVehicleStop(deliveryVehicle, i);
      assertThat(manifestStop.getAsJsonObject("planned_waypoint").get("lat").getAsLong())
          .isEqualTo(getDeliveryVehicleStopCoordinate(protoStop, "latitude"));
      assertThat(manifestStop.getAsJsonObject("planned_waypoint").get("lng").getAsLong())
          .isEqualTo(getDeliveryVehicleStopCoordinate(protoStop, "longitude"));

      JsonArray manifestTaskIds = manifestStop.getAsJsonArray("tasks");
      JsonArray protoTasks = protoStop.getAsJsonArray("tasks");
      assertThat(manifestTaskIds.size()).isEqualTo(protoTasks.size());
      for (int j = 0; j < manifestTaskIds.size(); j++) {
        assertThat(manifestTaskIds.get(j).getAsString())
            .isEqualTo(protoTasks.get(j).getAsJsonObject().get("task_id").getAsString());
      }

      // Keep for future use.
      stopIdToProtoStopMap.put(originalOrder[i], protoStop);
    }
  }

  @Test
  public void fixedOrder_21_reordersStops() throws ServletException, IOException {
    String[] newOrder = {"stop_1", "stop_3", "stop_2", "stop_4"};
    String newOrderString =
        stream(newOrder).map(stopId -> String.format("\"%s\"", stopId)).collect(joining(", "));

    request.setMethod("POST");
    request.setServletPath("/manifest");
    request.setPathInfo("/" + testBackendConfigServletProvider.vehicleId);
    request.setContent(
        String.format("{\"remaining_stop_id_list\":[%s]}", newOrderString).getBytes(UTF_8));
    servlet.service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);

    Gson gson = new Gson();
    JsonObject responseObject = gson.fromJson(response.getContentAsString(), JsonObject.class);
    JsonArray stopIdListJsonArray = responseObject.get("remaining_stop_id_list").getAsJsonArray();
    assertThat(newOrder).hasLength(stopIdListJsonArray.size());
    for (int i = 0; i < newOrder.length; i++) {
      assertThat(newOrder[i]).isEqualTo(stopIdListJsonArray.get(i).getAsString());
    }

    // Fetch the actual JSON-encoded protos.
    JsonObject deliveryVehicle = getJsonDeliveryVehicle(testBackendConfigServletProvider.vehicleId);
    for (int i = 0; i < newOrder.length; i++) {
      assertThat(stopIdToProtoStopMap.get(newOrder[i]))
          .isEqualTo(getDeliveryVehicleStop(deliveryVehicle, i));
    }
  }

  @Test
  public void fixedOrder_22_stopsWhenStopReorederingContainsInvalidStopId()
      throws ServletException, IOException {
    String[] newOrder = {"stop_1", "stop_3", "stop_x", "stop_4"};
    String newOrderString =
        stream(newOrder).map(stopId -> String.format("\"%s\"", stopId)).collect(joining(", "));

    request.setMethod("POST");
    request.setServletPath("/manifest");
    request.setPathInfo("/" + testBackendConfigServletProvider.vehicleId);
    request.setContent(
        String.format("{\"remaining_stop_id_list\":[%s]}", newOrderString).getBytes(UTF_8));
    servlet.service(request, response);
    assertThat(response.getStatus()).isEqualTo(404);
  }

  @Test
  public void fixedOrder_30_setsStopState() throws ServletException, IOException {
    String vehicleId = testBackendConfigServletProvider.vehicleId;
    String newState = "ARRIVED";

    // Do a vehicle status update and verify the result.
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();

    request.setMethod("POST");
    request.setServletPath("/manifest");
    request.setPathInfo(String.format("/%s", vehicleId));
    request.setContent(String.format("{\"current_stop_state\":\"%s\"}", newState).getBytes(UTF_8));
    servlet.service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);

    JsonObject deliveryVehicle = getJsonDeliveryVehicle(vehicleId);
    assertThat(getDeliveryVehicleStop(deliveryVehicle, 0).get("state").getAsString())
        .isEqualTo(newState);
  }

  @Test
  public void fixedOrder_40_removesStop() throws ServletException, IOException {
    String vehicleId = testBackendConfigServletProvider.vehicleId;
    String[] newStops = {"stop_3", "stop_2", "stop_4"};
    String newStopsString =
        stream(newStops).map(stopId -> String.format("\"%s\"", stopId)).collect(joining(", "));

    request.setMethod("POST");
    request.setServletPath("/manifest");
    request.setPathInfo("/" + vehicleId);
    request.setContent(
        String.format("{\"remaining_stop_id_list\":[%s]}", newStopsString).getBytes(UTF_8));
    servlet.service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);

    Gson gson = new Gson();
    JsonObject responseObject = gson.fromJson(response.getContentAsString(), JsonObject.class);
    JsonArray stopIdListJsonArray = responseObject.get("remaining_stop_id_list").getAsJsonArray();
    assertThat(newStops).hasLength(stopIdListJsonArray.size());
    for (int i = 0; i < newStops.length; i++) {
      assertThat(newStops[i]).isEqualTo(stopIdListJsonArray.get(i).getAsString());
    }

    // Fetch the actual JSON-encoded protos.
    JsonObject deliveryVehicle = getJsonDeliveryVehicle(vehicleId);
    for (int i = 0; i < newStops.length; i++) {
      assertThat(stopIdToProtoStopMap.get(newStops[i]))
          .isEqualTo(getDeliveryVehicleStop(deliveryVehicle, i));
    }
  }

  @Test
  public void fixedOrder_50_removesStopAndUpdatesStopState() throws ServletException, IOException {
    String vehicleId = testBackendConfigServletProvider.vehicleId;
    String[] newStops = {"stop_2", "stop_4"};
    String newStopsString =
        stream(newStops).map(stopId -> String.format("\"%s\"", stopId)).collect(joining(", "));
    String newState = "ENROUTE";

    request.setMethod("POST");
    request.setServletPath("/manifest");
    request.setPathInfo("/" + vehicleId);
    request.setContent(
        String.format(
                "{\"remaining_stop_id_list\":[%s], \"current_stop_state\":\"%s\"}",
                newStopsString, newState)
            .getBytes(UTF_8));
    servlet.service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);

    Gson gson = new Gson();
    JsonObject responseObject = gson.fromJson(response.getContentAsString(), JsonObject.class);
    JsonArray stopIdListJsonArray = responseObject.get("remaining_stop_id_list").getAsJsonArray();
    assertThat(newStops).hasLength(stopIdListJsonArray.size());
    for (int i = 0; i < newStops.length; i++) {
      assertThat(newStops[i]).isEqualTo(stopIdListJsonArray.get(i).getAsString());
    }

    // Fetch the actual JSON-encoded protos.
    JsonObject deliveryVehicle = getJsonDeliveryVehicle(vehicleId);
    assertThat(getDeliveryVehicleStop(deliveryVehicle, 0).get("state").getAsString())
        .isEqualTo(newState);

    // Verify the 2nd stop onwards because the 1st stop had its state updated.
    for (int i = 1; i < newStops.length; i++) {
      assertThat(stopIdToProtoStopMap.get(newStops[i]))
          .isEqualTo(getDeliveryVehicleStop(deliveryVehicle, i));
    }
  }

  @Test
  public void fixedOrder_51_reordersFirstTwoStops() throws ServletException, IOException {
    String vehicleId = testBackendConfigServletProvider.vehicleId;
    String[] newStops = {"stop_4", "stop_2"};
    String newStopsString =
        stream(newStops).map(stopId -> String.format("\"%s\"", stopId)).collect(joining(", "));

    request.setMethod("POST");
    request.setServletPath("/manifest");
    request.setPathInfo("/" + vehicleId);
    request.setContent(
        String.format("{\"remaining_stop_id_list\":[%s]}", newStopsString).getBytes(UTF_8));
    servlet.service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);

    Gson gson = new Gson();
    JsonObject responseObject = gson.fromJson(response.getContentAsString(), JsonObject.class);
    JsonArray stopIdListJsonArray = responseObject.get("remaining_stop_id_list").getAsJsonArray();
    assertThat(newStops).hasLength(stopIdListJsonArray.size());
    for (int i = 0; i < newStops.length; i++) {
      assertThat(newStops[i]).isEqualTo(stopIdListJsonArray.get(i).getAsString());
    }

    // Fetch the actual JSON-encoded protos.
    // Both stops should now be set to the original NEW state.
    JsonObject deliveryVehicle = getJsonDeliveryVehicle(vehicleId);
    assertThat(getDeliveryVehicleStop(deliveryVehicle, 0).get("state").getAsString())
        .isEqualTo("NEW");
    assertThat(getDeliveryVehicleStop(deliveryVehicle, 1).get("state").getAsString())
        .isEqualTo("NEW");
    for (int i = 0; i < newStops.length; i++) {
      assertThat(stopIdToProtoStopMap.get(newStops[i]))
          .isEqualTo(getDeliveryVehicleStop(deliveryVehicle, i));
    }
  }

  @Test
  public void fixedOrder_99_verifiesStopsList() throws ServletException, IOException {
    request.setMethod("GET");
    request.setServletPath("/manifest");
    request.setPathInfo("/" + testBackendConfigServletProvider.vehicleId);
    servlet.service(request, response);
    assertThat(response.getStatus()).isEqualTo(200);
    JsonObject responseObject = gson.fromJson(response.getContentAsString(), JsonObject.class);
    assertThat(responseObject.getAsJsonArray("stops").size()).isEqualTo(4);
  }

  /** Calls the DeliveryVehicleServlet to fetch the JSONified proto of a vehicle. */
  private JsonObject getJsonDeliveryVehicle(String vehicleId) throws ServletException, IOException {
    MockHttpServletRequest req = new MockHttpServletRequest();
    MockHttpServletResponse res = new MockHttpServletResponse();
    DeliveryVehicleServlet deliveryVehicleServlet =
        new DeliveryVehicleServlet(
            testBackendConfigServletProvider.servletState,
            testBackendConfigServletProvider.grpcServiceProvider);
    req.setMethod("GET");
    req.setServletPath("/delivery_vehicle");
    req.setPathInfo("/" + vehicleId);
    deliveryVehicleServlet.service(req, res);
    return new Gson().fromJson(res.getContentAsString(), JsonObject.class);
  }

  /** Gets the stop structure from the deliveryVehicle's index-th remaining segment. */
  private JsonObject getDeliveryVehicleStop(JsonObject deliveryVehicle, int index) {
    return deliveryVehicle
        .getAsJsonArray("remaining_vehicle_journey_segments")
        .get(index)
        .getAsJsonObject()
        .getAsJsonObject("stop");
  }

  /** Gets the latitude or longitude value from the stop. */
  private long getDeliveryVehicleStopCoordinate(JsonObject deliveryVehicleStop, String coordKey) {
    return deliveryVehicleStop
        .getAsJsonObject("planned_location")
        .getAsJsonObject("point")
        .get(coordKey)
        .getAsLong();
  }
}
