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
import com.example.backend.json.BackendConfigGsonProvider;
import com.example.backend.utils.ServletUtils;
import com.google.gson.JsonObject;
import com.google.protobuf.FieldMask;
import google.maps.fleetengine.delivery.v1.DeliveryServiceGrpc;
import google.maps.fleetengine.delivery.v1.DeliveryVehicle;
import google.maps.fleetengine.delivery.v1.UpdateDeliveryVehicleRequest;
import google.maps.fleetengine.delivery.v1.VehicleStop;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet for retrieving and assigning delivery vehicles. */
@Singleton
public final class DeliveryVehicleServlet extends HttpServlet {

  private final ServletState servletState;
  private static final Logger logger = Logger.getLogger(DeliveryVehicleServlet.class.getName());

  private final AuthenticatedGrpcServiceProvider grpcServiceProvider;

  @Inject
  public DeliveryVehicleServlet(
      ServletState servletState, AuthenticatedGrpcServiceProvider grpcServiceProvider) {
    this.servletState = servletState;
    this.grpcServiceProvider = grpcServiceProvider;
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    PrintWriter responseWriter = response.getWriter();

    if (request.getServletPath().equals("/delivery_vehicle")) {
      if (request.getPathInfo() == null) {
        logger.log(
            Level.WARNING, "The client attempted to get a vehicle without specifying its ID");
        ServletUtils.setErrorResponse(response, "The vehicle ID must be specified.", 400);
        return;
      }
      String vehicleId = request.getPathInfo().substring(1);
      DeliveryVehicle vehicle = servletState.getDeliveryVehicleById(vehicleId);
      if (vehicle != null) {
        ServletUtils.writeProtoJson(responseWriter, vehicle);
      } else {
        logger.log(Level.WARNING, "The client attempted to request a non-existent vehicle");
        ServletUtils.setErrorResponse(response, "The requested vehicle doesn't exist.", 404);
        return;
      }
    }

    responseWriter.flush();
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    if (request.getPathInfo() == null) {
      logger.log(
          Level.WARNING,
          "The client requested a delivery vehicle update but did not supply a vehicleId.");
      ServletUtils.setErrorResponse(response, "The delivery vehicle ID must be specified.", 400);
      return;
    }
    String vehicleId = request.getPathInfo().substring(1);

    DeliveryVehicle vehicle = servletState.getDeliveryVehicleById(vehicleId);
    if (vehicle == null) {
      logger.log(
          Level.WARNING,
          String.format(
              "The client requested a delivery vehicle update, but the vehicle ID %s does not match"
                  + " any vehicle.",
              vehicleId));
      ServletUtils.setErrorResponse(response, "The vehicle ID matched no vehicles.", 404);
      return;
    }

    // Read the post body to figure out what to update. Right now, we only support updating the
    // vehicle status. The post body should just be a json object with keys.
    JsonObject updates =
        BackendConfigGsonProvider.get().fromJson(request.getReader(), JsonObject.class);
    if (!updates.has("stopState")) {
      logger.log(
          Level.WARNING, String.format("No stop state for vehicle ID %s specified.", vehicleId));
      ServletUtils.setErrorResponse(response, "No stop state was specified.", 400);
      return;
    }
    String stopStateName = updates.get("stopState").getAsString();
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
        logger.log(Level.WARNING, String.format("Stop state %s is invalid.", stopStateName));
        ServletUtils.setErrorResponse(response, String.format("Stop state is invalid."), 400);
        return;
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

    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    ServletUtils.writeProtoJson(response.getWriter(), responseVehicle);
    response.getWriter().flush();
  }

  @Override
  public void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
    DeliveryServiceGrpc.DeliveryServiceBlockingStub deliveryService =
        grpcServiceProvider.getAuthenticatedDeliveryService();

    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    PrintWriter responseWriter = response.getWriter();

    // As above, we're using the IP address to identify the client.
    String clientIdentifier = request.getRemoteAddr();

    String vehicleId = request.getPathInfo().substring(1);
    DeliveryVehicle vehicle =
        servletState.getDeliveryVehicleById(
            servletState.getDeliveryVehicleMapByClient(clientIdentifier));

    if (vehicle == null) {
      logger.log(Level.WARNING, "The client is not assigned to any vehicle");
      ServletUtils.setErrorResponse(response, "The requested vehicle doesn't exist.", 404);
      return;
    }

    if (!servletState.getId(vehicle.getName()).equals(vehicleId)) {
      logger.log(Level.WARNING, "The client attempted to request a non-existent vehicle");
      ServletUtils.setErrorResponse(response, "The requested vehicle doesn't exist.", 404);
      return;
    }

    // After verifying the client is able to update the vehicle, fetch the serialized vehicle
    // in the body.
    DeliveryVehicle updatedVehicle =
        ServletUtils.readJsonProto(request.getReader(), DeliveryVehicle.newBuilder());

    // Set the updated fieldmask.
    UpdateDeliveryVehicleRequest updateRequest =
        UpdateDeliveryVehicleRequest.newBuilder()
            .setDeliveryVehicle(updatedVehicle)
            .setUpdateMask(FieldMask.newBuilder().addPaths("remaining_vehicle_journey_segments"))
            .build();
    DeliveryVehicle responseVehicle = deliveryService.updateDeliveryVehicle(updateRequest);
    ServletUtils.writeProtoJson(responseWriter, responseVehicle);
    servletState.addDeliveryVehicle(responseVehicle);
    responseWriter.flush();
  }
}
