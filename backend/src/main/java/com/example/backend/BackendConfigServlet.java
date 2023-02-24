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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;

import com.example.backend.auth.grpcservice.AuthenticatedGrpcServiceProvider;
import com.example.backend.json.BackendConfig;
import com.example.backend.json.BackendConfigGsonProvider;
import com.example.backend.utils.BackendConfigException;
import com.example.backend.utils.BackendConfigUtils;
import com.example.backend.utils.SampleBackendUtils;
import com.example.backend.utils.ServletUtils;
import com.google.gson.Gson;
import com.google.protobuf.FieldMask;
import com.google.type.LatLng;
import google.maps.fleetengine.delivery.v1.CreateDeliveryVehicleRequest;
import google.maps.fleetengine.delivery.v1.CreateTaskRequest;
import google.maps.fleetengine.delivery.v1.DeliveryServiceGrpc;
import google.maps.fleetengine.delivery.v1.DeliveryVehicle;
import google.maps.fleetengine.delivery.v1.DeliveryVehicleLocation;
import google.maps.fleetengine.delivery.v1.Task;
import google.maps.fleetengine.delivery.v1.UpdateDeliveryVehicleRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

/**
 * Servlet for uploading backend config files.
 *
 * <p>POST /backend_config
 */
@Singleton
@MultipartConfig(
    location = "/tmp",
    fileSizeThreshold = 1 * 1024 * 1024,
    maxRequestSize = 100 * 1024 * 1024,
    maxFileSize = 100 * 1024 * 1024)
public final class BackendConfigServlet extends HttpServlet {

  private final ServletState servletState;
  private static final Logger logger = Logger.getLogger(BackendConfigServlet.class.getName());

  private final AuthenticatedGrpcServiceProvider grpcServiceProvider;

  @Inject
  public BackendConfigServlet(
      ServletState servletState, AuthenticatedGrpcServiceProvider grpcServiceProvider) {
    this.servletState = servletState;
    this.grpcServiceProvider = grpcServiceProvider;
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    if (request.getContentType() != null
        && request.getContentType().startsWith("multipart/form-data")) {
      request.setAttribute(
          "org.eclipse.jetty.multipartConfig",
          new MultipartConfigElement(
              "/tmp", 100 * 1024 * 1024, 100 * 1024 * 1024, 1 * 1024 * 1024));
    }
    Part filePart = request.getPart("file");
    if (filePart == null) {
      logger.log(
          Level.WARNING,
          "The client's backend config update request did not include a backend config");
      ServletUtils.setErrorResponse(response, "The backend config was not attached.", 400);
      return;
    }
    InputStream fileContent = filePart.getInputStream();
    serveUpload(fileContent, response);
  }

  public void serveUpload(InputStream fileContent, HttpServletResponse response)
      throws IOException {
    InputStreamReader reader = new InputStreamReader(fileContent, UTF_8);
    BackendConfig backendConfig;

    Gson gson = BackendConfigGsonProvider.get();
    try {
      backendConfig = gson.fromJson(reader, BackendConfig.class);
    } finally {
      reader.close();
    }

    BackendConfigUtils.setTimestamp(System.currentTimeMillis());
    this.servletState.clearDeliveryState();

    DeliveryServiceGrpc.DeliveryServiceBlockingStub authenticatedDeliveryService =
        grpcServiceProvider.getAuthenticatedDeliveryService();

    response.setCharacterEncoding("UTF-8");
    PrintWriter responseWriter = response.getWriter();

    // At this point, the backend config has been read into memory. Loop through its
    // contents and invoke the corresponding Fleet Engine APIs.
    for (BackendConfig.Manifest m : backendConfig.manifests) {
      m.vehicle.vehicleId = BackendConfigUtils.getTimestampedId(m.vehicle.vehicleId);

      LatLng.Builder startLocation =
          LatLng.newBuilder().setLatitude(37.42311).setLongitude(-122.09259);
      if (m.vehicle.startLocation != null) {
        startLocation
            .setLatitude(m.vehicle.startLocation.lat)
            .setLongitude(m.vehicle.startLocation.lng);
      }

      // Create the vehicle. Note: most of the fields in the DeliveryVehicle
      // passed to CreateDeliveryVehicleRequest are ignored; instantiating a
      // blank proto is good enough.
      CreateDeliveryVehicleRequest deliveryVehicleRequest =
          CreateDeliveryVehicleRequest.newBuilder()
              .setParent(BackendConfigUtils.PARENT)
              .setDeliveryVehicleId(m.vehicle.vehicleId)
              .setDeliveryVehicle(
                  DeliveryVehicle.newBuilder()
                      .setLastLocation(
                          DeliveryVehicleLocation.newBuilder().setLocation(startLocation)))
              .build();

      DeliveryVehicle responseDeliveryVehicle =
          authenticatedDeliveryService.createDeliveryVehicle(deliveryVehicleRequest);
      logger.info(responseDeliveryVehicle.toString());

      // Create the tasks for the vehicle.
      for (BackendConfig.Task t : m.tasks) {
        t.taskId = BackendConfigUtils.getTimestampedId(t.taskId);
        t.trackingId = BackendConfigUtils.getTimestampedId(t.trackingId);
        CreateTaskRequest taskRequest =
            CreateTaskRequest.newBuilder()
                .setParent(BackendConfigUtils.PARENT)
                .setTaskId(t.taskId)
                .setTask(BackendConfigUtils.createTask(t))
                .build();

        Task responseTask = authenticatedDeliveryService.createTask(taskRequest);
        responseWriter.print("\nTask created:\n");
        ServletUtils.writeProtoJson(responseWriter, responseTask);
        logger.info(responseTask.toString());
        this.servletState.addTask(responseTask);
      }

      // Update the created delivery vehicle to include the VehicleJourneySegments.
      DeliveryVehicle.Builder vehicleBuilder = responseDeliveryVehicle.toBuilder();

      // Create the stops in the order specified in m.remainingStopIdList. If that field doesn't
      // exist, use the order in m.stops.
      if (m.remainingStopIdList == null) {
        m.remainingStopIdList = stream(m.stops).map(s -> s.stopId).toArray(String[]::new);
      }

      HashMap<String, BackendConfig.Stop> stopsMap = new HashMap<>();
      for (BackendConfig.Stop s : m.stops) {
        stopsMap.put(s.stopId, s);
        s.tasks = stream(s.tasks).map(BackendConfigUtils::getTimestampedId).toArray(String[]::new);
      }
      m.stops = stream(m.remainingStopIdList).map(stopsMap::get).toArray(BackendConfig.Stop[]::new);
      try {
        vehicleBuilder.addAllRemainingVehicleJourneySegments(
            BackendConfigUtils.createVehicleJourneySegments(m));
      } catch (BackendConfigException e) {
        logger.log(Level.WARNING, e.getMessage());
        ServletUtils.setErrorResponse(response, e.getMessage(), 400);
        return;
      }
      UpdateDeliveryVehicleRequest updateRequest =
          UpdateDeliveryVehicleRequest.newBuilder()
              .setDeliveryVehicle(vehicleBuilder)
              .setUpdateMask(FieldMask.newBuilder().addPaths("remaining_vehicle_journey_segments"))
              .build();
      DeliveryVehicle updatedResponseDeliveryVehicle =
          authenticatedDeliveryService.updateDeliveryVehicle(updateRequest);
      responseWriter.print("\nVehicle created and assigned:\n");
      ServletUtils.writeProtoJson(responseWriter, updatedResponseDeliveryVehicle);
      servletState.addDeliveryVehicle(updatedResponseDeliveryVehicle);
    }

    // Set the backend ID for each manifest.
    for (BackendConfig.Manifest manifest : backendConfig.manifests) {
      manifest.vehicle.providerId = SampleBackendUtils.backendProperties.providerId();
    }

    // Everything worked fine; add the backend config and exit.
    servletState.setBackendConfig(backendConfig);
    logger.info(response.toString());
    responseWriter.flush();
  }
}
