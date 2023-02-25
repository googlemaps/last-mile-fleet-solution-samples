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
import com.example.backend.json.BackendConfig;
import com.example.backend.json.BackendConfigGsonProvider;
import com.example.backend.utils.ServletUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.protobuf.FieldMask;
import google.maps.fleetengine.delivery.v1.DeliveryServiceGrpc;
import google.maps.fleetengine.delivery.v1.DeliveryVehicle;
import google.maps.fleetengine.delivery.v1.UpdateDeliveryVehicleRequest;
import google.maps.fleetengine.delivery.v1.VehicleJourneySegment;
import google.maps.fleetengine.delivery.v1.VehicleStop;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet for retrieving and assigning delivery vehicles. */
@Singleton
public final class ManifestServlet extends HttpServlet {

  private final ServletState servletState;
  private static final Logger logger = Logger.getLogger(ManifestServlet.class.getName());

  private final AuthenticatedGrpcServiceProvider grpcServiceProvider;

  @Inject
  public ManifestServlet(
      ServletState servletState, AuthenticatedGrpcServiceProvider grpcServiceProvider) {
    this.servletState = servletState;
    this.grpcServiceProvider = grpcServiceProvider;
  }

  /**
   * Fetches a manifest.
   *
   * <p>GET /manifest/:vehicleId
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    PrintWriter responseWriter = response.getWriter();

    if (request.getPathInfo() == null) {
      logger.log(Level.WARNING, "The client attempted to get a vehicle without specifying its ID");
      ServletUtils.setErrorResponse(response, "The vehicle ID must be specified.", 400);
      return;
    }
    String vehicleId = request.getPathInfo().substring(1);
    BackendConfig.Manifest manifest = servletState.getManifest(vehicleId);
    if (manifest != null) {
      responseWriter.print(BackendConfigGsonProvider.get().toJson(manifest));
      responseWriter.flush();
    } else {
      logger.log(
          Level.WARNING,
          "The client attempted to retrieve the manifest for a non-existent vehicle");
      ServletUtils.setErrorResponse(response, "The requested manifest doesn't exist.", 404);
    }
  }

  /**
   * Updates a manifest.
   *
   * <p>POST /manifest/[:vehicleId]
   *
   * <p>Valid updates include:
   *
   * <ul>
   *   <li>assignment of a manifest to a client, by setting client_id in the body. For this update,
   *       vehicleId is optional; when it is not supplied, the backend attempts to assign any
   *       available vehicle to the client. All other updates require vehicleId to be set.
   *       Assignment is an exclusive operation, and cannot be done in the same request as any of
   *       the other updates.
   *   <li>update of the vehicle's stop state, by setting current_stop_state in the body.
   *   <li>marking the vehicle's current stop as complete, by setting remaining_stop_id_list to a
   *       new list which is shorter than the previous version. This updates the state of all tasks
   *       associated with the stop(s) that were removed from the list as CLOSED. The outcomes of
   *       those tasks are unaffected.
   *   <li>reordering the vehicle's sequence of stops, by reordering the stop IDs in
   *       remaining_stop_id_list.
   * </ul>
   *
   * <p>Note: this would ideally be done with the PATCH method, rather than POST. However, the
   * sample backend is implemented with Java's HttpServlet
   * (https://javaee.github.io/javaee-spec/javadocs/javax/servlet/http/HttpServlet.html) which does
   * not support PATCH. As such, for this operation, updates are supported via POST.
   */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    PrintWriter responseWriter = response.getWriter();
    DeliveryVehicle vehicle;
    String vehicleId;
    if (request.getPathInfo() == null || request.getPathInfo().equals("")) {
      vehicleId = "";
    } else {
      vehicleId = request.getPathInfo().substring(1);
    }

    logger.log(Level.INFO, String.format("manifest post with vehicleId (%s)", vehicleId));

    // Read the post body to figure out what to update. Right now, we only support assignment, or
    // updating the vehicle status. The post body should just be a json object with keys.
    JsonObject updates =
        BackendConfigGsonProvider.get().fromJson(request.getReader(), JsonObject.class);
    if (updates == null) {
      updates = new JsonObject();
    }

    boolean hasClientIdUpdate = updates.has("client_id");
    boolean hasStopIdListUpdate = updates.has("remaining_stop_id_list");
    boolean hasStopStateUpdate = updates.has("current_stop_state");
    boolean hasOtherUpdate = hasStopIdListUpdate || hasStopStateUpdate;

    if (!(hasClientIdUpdate || hasOtherUpdate)) {
      logger.log(
          Level.WARNING,
          "The client requested a manifest update but did not supply a valid update.");
      ServletUtils.setErrorResponse(response, "The update must be specified.", 400);
      return;
    }

    if (hasClientIdUpdate && hasOtherUpdate) {
      logger.log(
          Level.WARNING,
          "The client requested an assignment but also requested other mutually exclusive"
              + " updates.");
      ServletUtils.setErrorResponse(
          response, "The request cannot contain both an assignment and other updates.", 400);
      return;
    }

    // Assign a client ID to a vehicle. For this request, vehicleId is not strictly necessary, but
    // the request body must have a "client_id" field.
    if (hasClientIdUpdate) {
      String clientId = updates.get("client_id").getAsString();
      if (clientId.equals("")) {
        logger.log(
            Level.WARNING,
            String.format(
                "The client requested a manifest assignment, but the client ID (%s) is invalid",
                clientId));
        ServletUtils.setErrorResponse(response, "The client ID is invalid.", 400);
        return;
      }
      logger.log(Level.INFO, String.format("clientId is %s", clientId));
      try {
        vehicle = assignVehicleToClient(clientId, vehicleId);
      } catch (ManifestException e) {
        logger.log(Level.WARNING, e.getLogMessage());
        ServletUtils.setErrorResponse(response, e.getErrorMessage(), e.getErrorCode());
        return;
      }
      BackendConfig.Manifest manifest =
          servletState.getManifest(ServletState.getId(vehicle.getName()));
      responseWriter.print(BackendConfigGsonProvider.get().toJson(manifest));
      return;
    }

    // Otherwise, a vehicleId is required.
    vehicle = servletState.getDeliveryVehicleById(vehicleId);
    if (vehicle == null) {
      logger.log(
          Level.WARNING,
          String.format(
              "The client requested a delivery vehicle update, but the vehicle ID (%s) does not"
                  + " match any vehicle.",
              vehicleId));
      ServletUtils.setErrorResponse(response, "The vehicle ID matched no vehicles.", 404);
      return;
    }

    // The following operations can be specified in the same update message, but currently they
    // are not implemented in an atomic manner. If both remaining_stop_id_list and
    // current_stop_state are specified, the remaining_stop_id_list update must go first, and the
    // current_stop_state update will manipulate the state of the first stop AFTER the
    // remaining_stop_id_list update is complete.

    // Update the list of remaining stop IDs. The request body must have the
    // "remaining_stop_id_list" field and its value must be a list of stop IDs.
    if (hasStopIdListUpdate) {
      ArrayList<String> stopIdList = new ArrayList<>();
      for (JsonElement e : updates.get("remaining_stop_id_list").getAsJsonArray()) {
        stopIdList.add(e.getAsString());
      }
      try {
        vehicle = updateVehicleStopList(vehicle, stopIdList);
      } catch (ManifestException e) {
        logger.log(Level.WARNING, e.getLogMessage());
        ServletUtils.setErrorResponse(response, e.getErrorMessage(), e.getErrorCode());
        return;
      }
    }

    // Update the current stop state. The request body must have the "current_stop_state" field
    // and its value must be one of the enum values in VehicleStop.State.
    if (hasStopStateUpdate) {
      String stopStateName = updates.get("current_stop_state").getAsString();
      try {
        vehicle = updateVehicleStopState(vehicle, stopStateName);
      } catch (ManifestException e) {
        logger.log(Level.WARNING, e.getLogMessage());
        ServletUtils.setErrorResponse(response, e.getErrorMessage(), e.getErrorCode());
        return;
      }
    }

    // After all non-mutually-exclusive updates are processed, return the most up to date manifest.
    BackendConfig.Manifest manifest = servletState.getManifest(vehicleId);
    responseWriter.print(BackendConfigGsonProvider.get().toJson(manifest));
    responseWriter.flush();
  }

  /**
   * Assigns a vehicle to the client.
   *
   * @param clientId The ID of the client.
   * @param vehicleId The ID of the vehicle.
   * @return The assigned vehicle.
   * @throws ManifestException if the vehicle could not be assigned to the client.
   */
  private DeliveryVehicle assignVehicleToClient(String clientId, String vehicleId)
      throws ManifestException {
    // Assignment does not require a vehicleId; but if a vehicleId is supplied, attempt to use it.
    String existingVehicleId = servletState.getDeliveryVehicleMapByClient(clientId);
    DeliveryVehicle vehicle;

    if (existingVehicleId != null) {
      // If the client is already assigned another vehicle...
      if (!vehicleId.equals("") && !vehicleId.equals(existingVehicleId)) {
        throw new ManifestException(
            "The client attempted to re-request another vehicle",
            "You cannot request different vehicles.",
            403);
      }

      vehicle = servletState.getDeliveryVehicleById(existingVehicleId);
      if (vehicle == null) {
        throw new ManifestException(
            "The client\'s assigned vehicle doesn't exist.",
            "The requested vehicle doesn't exist.",
            404);
      }
    } else if (vehicleId.equals("")) {
      // If vehicleId is null, assign the next available vehicle
      vehicle = servletState.getAnyAvailableDeliveryVehicle();
      if (vehicle == null) {
        throw new ManifestException(
            "The client requested a vehicle for assignment, but none were available.",
            "There are no available vehicles for assignment.",
            404);
      }
    } else {
      // If vehicleId is not null, attempt to assign that vehicle
      vehicle = servletState.getDeliveryVehicleById(vehicleId);
      if (vehicle == null) {
        throw new ManifestException(
            "The client attempted to request a non-existent vehicle",
            "The requested vehicle doesn't exist.",
            404);
      }
      if (servletState.isDeliveryVehicleAssigned(vehicleId)) {
        throw new ManifestException(
            "The client attempted to request a vehicle that is currently assigned",
            "The requested vehicle is currently assigned.",
            403);
      }
    }

    // We have a vehicle available for assignment, or is already assigned to the same vehicle.
    servletState.addClientToDeliveryVehicleMap(clientId, vehicle);
    return vehicle;
  }

  /**
   * Updates the vehicle stop state.
   *
   * @throws ManifestException If the update fails.
   */
  private DeliveryVehicle updateVehicleStopState(DeliveryVehicle vehicle, String stopStateName)
      throws ManifestException {
    VehicleStop.State state;
    switch (stopStateName) {
      case "STATE_UNSPECIFIED":
        state = VehicleStop.State.STATE_UNSPECIFIED;
        break;
      case "NEW":
        state = VehicleStop.State.NEW;
        break;
      case "ENROUTE":
        state = VehicleStop.State.ENROUTE;
        break;
      case "ARRIVED":
        state = VehicleStop.State.ARRIVED;
        break;
      default:
        throw new ManifestException(
            String.format("Stop state %s is invalid.", stopStateName),
            String.format("Stop state %s is invalid.", stopStateName),
            400);
    }

    DeliveryVehicle.Builder vehicleBuilder = vehicle.toBuilder();
    vehicleBuilder.getRemainingVehicleJourneySegmentsBuilder(0).getStopBuilder().setState(state);

    UpdateDeliveryVehicleRequest updateReq =
        UpdateDeliveryVehicleRequest.newBuilder()
            .setDeliveryVehicle(vehicleBuilder)
            .setUpdateMask(FieldMask.newBuilder().addPaths("remaining_vehicle_journey_segments"))
            .build();
    DeliveryServiceGrpc.DeliveryServiceBlockingStub authenticatedDeliveryService =
        grpcServiceProvider.getAuthenticatedDeliveryService();
    DeliveryVehicle responseVehicle = authenticatedDeliveryService.updateDeliveryVehicle(updateReq);
    servletState.addDeliveryVehicle(responseVehicle);
    String vehicleId = ServletState.getId(vehicle.getName());
    BackendConfig.StopState backendConfigStopState = BackendConfig.StopState.of(stopStateName);
    logger.log(
        Level.INFO,
        String.format(
            "updating manifest with vehicle ID %s and stop state %s",
            vehicleId, backendConfigStopState.getValue()));
    servletState.getManifest(vehicleId).currentStopState = backendConfigStopState;

    return responseVehicle;
  }

  private DeliveryVehicle updateVehicleStopList(DeliveryVehicle vehicle, List<String> stopIds)
      throws ManifestException {

    String vehicleId = ServletState.getId(vehicle.getName());
    BackendConfig.Manifest manifest = servletState.getManifest(vehicleId);

    // stopIds must be a subset of the existing set of remaining stopIds. To check this, create a
    // map of remainingStopIdList -> VehicleJourneySegment.
    HashMap<String, VehicleJourneySegment> stopsMap = new HashMap<>();
    for (int i = 0; i < manifest.remainingStopIdList.length; i++) {
      stopsMap.put(manifest.remainingStopIdList[i], vehicle.getRemainingVehicleJourneySegments(i));
    }
    ArrayList<VehicleJourneySegment> newVJSList = new ArrayList<>();
    for (String stopId : stopIds) {
      if (!stopsMap.containsKey(stopId)) {
        throw new ManifestException(
            "The update request contained a stopId that isn't in the original set",
            "The update request contained a stopId that isn't in the original set",
            404);
      }

      // It is an error condition for the state of stops after the first stop to be anything other
      // than NEW. Starting with the second stop, reset its state.
      if (newVJSList.size() == 0) {
        newVJSList.add(stopsMap.get(stopId));
      } else {
        VehicleJourneySegment.Builder vjsBuilder = stopsMap.get(stopId).toBuilder();
        vjsBuilder.getStopBuilder().setState(VehicleStop.State.NEW);
        newVJSList.add(vjsBuilder.build());
      }
    }

    DeliveryVehicle.Builder vehicleBuilder = vehicle.toBuilder();
    vehicleBuilder
        .clearRemainingVehicleJourneySegments()
        .addAllRemainingVehicleJourneySegments(newVJSList);

    UpdateDeliveryVehicleRequest updateReq =
        UpdateDeliveryVehicleRequest.newBuilder()
            .setDeliveryVehicle(vehicleBuilder)
            .setUpdateMask(FieldMask.newBuilder().addPaths("remaining_vehicle_journey_segments"))
            .build();
    DeliveryServiceGrpc.DeliveryServiceBlockingStub authenticatedDeliveryService =
        grpcServiceProvider.getAuthenticatedDeliveryService();
    DeliveryVehicle responseVehicle = authenticatedDeliveryService.updateDeliveryVehicle(updateReq);
    servletState.addDeliveryVehicle(responseVehicle);

    manifest.remainingStopIdList = stopIds.toArray(new String[0]);
    return responseVehicle;
  }

  /** An exception class for manifest servlet methods. */
  private final class ManifestException extends Exception {

    private final String logMessage;
    private final String errorMessage;
    private final int errorCode;

    public ManifestException(String logMessage, String errorMessage, int errorCode) {
      this.logMessage = logMessage;
      this.errorMessage = errorMessage;
      this.errorCode = errorCode;
    }

    public String getLogMessage() {
      return logMessage;
    }

    public String getErrorMessage() {
      return errorMessage;
    }

    public int getErrorCode() {
      return errorCode;
    }
  }
}
