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
import com.example.backend.utils.TaskUtils;
import com.google.gson.JsonObject;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Timestamp;
import google.maps.fleetengine.delivery.v1.DeliveryServiceGrpc;
import google.maps.fleetengine.delivery.v1.DeliveryVehicle;
import google.maps.fleetengine.delivery.v1.GetTaskRequest;
import google.maps.fleetengine.delivery.v1.Task;
import google.maps.fleetengine.delivery.v1.UpdateTaskRequest;
import google.maps.fleetengine.delivery.v1.VehicleJourneySegment;
import google.maps.fleetengine.delivery.v1.VehicleStop;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet for fetching and updating tasks. */
@Singleton
public final class TaskServlet extends HttpServlet {

  private final ServletState servletState;
  private static final Logger logger = Logger.getLogger(TaskServlet.class.getName());
  private final AuthenticatedGrpcServiceProvider grpcServiceProvider;

  @Inject
  public TaskServlet(
      ServletState servletState, AuthenticatedGrpcServiceProvider grpcServiceProvider) {
    this.servletState = servletState;
    this.grpcServiceProvider = grpcServiceProvider;
  }

  /**
   * Fetches a task.
   *
   * <p>GET /task/:taskId
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    PrintWriter responseWriter = response.getWriter();
    DeliveryServiceGrpc.DeliveryServiceBlockingStub authenticatedDeliveryService =
        grpcServiceProvider.getAuthenticatedDeliveryService();

    // Fetch tasks by vehicle ID.
    // It's silly that HttpServletRequest doesn't deal well with URL queries out of the box.
    if (request.getServletPath().equals("/tasks")) {
      String queryString = request.getQueryString();
      if (queryString == null) {
        logger.log(Level.WARNING, "The client requested tasks but did not supply a vehicleId.");
        ServletUtils.setErrorResponse(response, "The vehicle ID must be specified.", 400);
        return;
      }
      String vehicleId = ServletUtils.getUrlQueryData(queryString, "vehicleId");
      if (vehicleId == null) {
        logger.log(Level.WARNING, "The client requested tasks but did not supply a vehicleId.");
        ServletUtils.setErrorResponse(response, "The vehicle ID must be specified.", 400);
        return;
      }
      DeliveryVehicle vehicle = servletState.getDeliveryVehicleById(vehicleId);
      ArrayList<Task> tasks = new ArrayList<>();
      if (vehicle == null) {
        logger.log(Level.WARNING, "The client requested tasks for a non-existent vehicle.");
        ServletUtils.setErrorResponse(response, "The requested vehicle doesn't exist.", 404);
        return;
      }
      for (VehicleJourneySegment vjs : vehicle.getRemainingVehicleJourneySegmentsList()) {
        if (!vjs.hasStop()) {
          continue;
        }
        for (VehicleStop.TaskInfo taskInfo : vjs.getStop().getTasksList()) {
          if (taskInfo.getTaskId().isEmpty()) {
            continue;
          }
          Task task = servletState.getTaskById(taskInfo.getTaskId());
          if (task == null) {
            continue;
          }
          tasks.add(task);
        }
      }

      // Manually print out a json list.
      responseWriter.print("[");
      boolean isWritingFirstTask = true;
      for (Task task : tasks) {
        if (isWritingFirstTask) {
          isWritingFirstTask = false;
        } else {
          responseWriter.print(",");
        }
        ServletUtils.writeProtoJson(responseWriter, task);
      }
      responseWriter.print("]");
      responseWriter.flush();
      return;
    }

    if (request.getServletPath().equals("/taskInfoByTrackingId")) {
      // get task information by tracking id
      if (request.getPathInfo() == null) {
        logger.log(
            Level.WARNING,
            "The client requested a task by tracking ID but did not supply a trackingId.");
        ServletUtils.setErrorResponse(response, "The tracking ID must be specified.", 400);
        return;
      }
      String trackingId = request.getPathInfo().substring(1);

      BackendConfig.Task t = servletState.getBackendConfigTaskByTrackingId(trackingId);
      if (t == null) {
        logger.log(
            Level.WARNING,
            String.format("The tracking ID %s does not exist in the manifest.", trackingId));
        ServletUtils.setErrorResponse(response, "The task with the tracking ID cannot be found.", 404);
        return;
      }

      responseWriter.print(BackendConfigGsonProvider.get().toJson(t));
      responseWriter.flush();
      return;
    }

    if (request.getServletPath().equals("/task")) {
      // get a task by id
      if (request.getPathInfo() == null) {
        logger.log(Level.WARNING, "The client requested a task but did not supply a taskId.");
        ServletUtils.setErrorResponse(response, "The task ID must be specified.", 400);
        return;
      }
      String taskId = request.getPathInfo().substring(1);
      String queryString = request.getQueryString();
      if (queryString != null && queryString.length() > 0) {
        String manifestRequestedQueryValue =
            ServletUtils.getUrlQueryData(queryString, "manifestDataRequested");
        if (manifestRequestedQueryValue != null && manifestRequestedQueryValue.equals("true")) {

          // Return manifest data instead.
          BackendConfig.Task t = servletState.getBackendConfigTask(taskId);
          if (t == null) {
            logger.log(
                Level.WARNING,
                String.format("The task ID %s does not exist in the manifest.", taskId));
            ServletUtils.setErrorResponse(response, "The task cannot be found.", 404);
            return;
          }

          responseWriter.print(BackendConfigGsonProvider.get().toJson(t));
          responseWriter.flush();
          return;
        }
      }

      Task task = servletState.getTaskById(taskId);
      if (task == null) {
        logger.log(
            Level.WARNING,
            String.format(
                "The client requested a task update, but the task ID %s does not match any task.",
                taskId));
        ServletUtils.setErrorResponse(response, "The task ID matched no tasks.", 400);
        return;
      }

      // Fetch the task from Fleet Engine, just in case.
      GetTaskRequest req =
          GetTaskRequest.newBuilder().setName(TaskUtils.getTaskNameFromId(taskId)).build();
      Task responseTask = authenticatedDeliveryService.getTask(req);

      // Store the new task.
      servletState.addTask(responseTask);
      ServletUtils.writeProtoJson(responseWriter, responseTask);
      responseWriter.flush();
    }
  }

  /**
   * Updates a task.
   *
   * <p>POST /task/:taskId
   *
   * <p>Valid updates include:
   *
   * <ul>
   *   <li>update of the task's outcome, by setting task_outcome in the body.
   * </ul>
   *
   * <p>Note: this would ideally be done with the PATCH method, rather than POST. However, the
   * sample backend is implemented with Java's HttpServlet
   * (https://javaee.github.io/javaee-spec/javadocs/javax/servlet/http/HttpServlet.html) which does
   * not support PATCH. As such, for this operation, updates are supported via POST.
   */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    if (request.getPathInfo() == null) {
      logger.log(Level.WARNING, "The client requested a task update but did not supply a taskId.");
      ServletUtils.setErrorResponse(response, "The task ID must be specified.", 400);
      return;
    }
    String taskId = request.getPathInfo().substring(1);

    Task task = servletState.getTaskById(taskId);
    if (task == null) {
      logger.log(
          Level.WARNING,
          String.format(
              "The client requested a task update, but the task ID %s does not match any task.",
              taskId));
      ServletUtils.setErrorResponse(response, "The task ID matched no tasks.", 404);
      return;
    }

    // Read the post body to figure out what to update. Right now, we only support updating the
    // task outcome. The post body should just be a json object with keys.
    JsonObject updates =
        BackendConfigGsonProvider.get().fromJson(request.getReader(), JsonObject.class);
    if (!updates.has("task_outcome")) {
      logger.log(Level.WARNING, String.format("No task outcome for task ID %s specified.", taskId));
      ServletUtils.setErrorResponse(response, "No task outcome was specified.", 400);
      return;
    }
    String taskOutcomeName = updates.get("task_outcome").getAsString();
    Task.TaskOutcome outcome;
    switch (taskOutcomeName) {
      case "TASK_OUTCOME_UNSPECIFIED":
        outcome = Task.TaskOutcome.TASK_OUTCOME_UNSPECIFIED;
        break;
      case "SUCCEEDED":
        outcome = Task.TaskOutcome.SUCCEEDED;
        break;
      case "FAILED":
        outcome = Task.TaskOutcome.FAILED;
        break;
      default:
        logger.log(Level.WARNING, String.format("Task outcome %s is invalid.", taskOutcomeName));
        ServletUtils.setErrorResponse(response, "Task outcome is invalid.", 400);
        return;
    }
    UpdateTaskRequest updateReq =
        UpdateTaskRequest.newBuilder()
            .setTask(
                task.toBuilder()
                    .setTaskOutcome(outcome)
                    .setTaskOutcomeTime(
                        Timestamp.newBuilder().setSeconds(System.currentTimeMillis() / 1000)))
            .setUpdateMask(
                FieldMask.newBuilder().addPaths("task_outcome").addPaths("task_outcome_time"))
            .build();
    DeliveryServiceGrpc.DeliveryServiceBlockingStub authenticatedDeliveryService =
        grpcServiceProvider.getAuthenticatedDeliveryService();
    Task responseTask = authenticatedDeliveryService.updateTask(updateReq);
    servletState.addTask(responseTask);

    // The task has been marked as complete; remove it from the manifest.
    if (!outcome.equals(Task.TaskOutcome.TASK_OUTCOME_UNSPECIFIED)) {
      servletState.removeBackendConfigTask(taskId);
    }

    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    ServletUtils.writeProtoJson(response.getWriter(), responseTask);
    response.getWriter().flush();
  }

  @Override
  public void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
    if (request.getServletPath().equals("/task")) {
      if (request.getPathInfo() == null) {
        logger.log(
            Level.WARNING, "The client requested a task update but did not supply a taskId.");
        ServletUtils.setErrorResponse(response, "The task ID must be specified.", 400);
        return;
      }
      String taskId = request.getPathInfo().substring(1);

      // Verify that the task exists in servletState.
      Task task = servletState.getTaskById(taskId);

      if (task == null) {
        logger.log(
            Level.WARNING,
            String.format(
                "The client requested a task update, but the task ID %s does not match any task.",
                taskId));
        ServletUtils.setErrorResponse(response, "The task ID matched no tasks.", 400);
        return;
      }

      Task updatedTask = ServletUtils.readJsonProto(request.getReader(), Task.newBuilder());

      // For example, we can update the task outcome.
      if (task.getTaskOutcome() == updatedTask.getTaskOutcome()) {
        logger.log(
            Level.WARNING,
            "The client requested a task update, but the new task outcome is the same as the"
                + " original task outcome.");
        ServletUtils.setErrorResponse(response, "The task outcome must be updated", 400);
        return;
      }

      UpdateTaskRequest updateReq =
          UpdateTaskRequest.newBuilder()
              .setTask(updatedTask)
              .setUpdateMask(FieldMask.newBuilder().addPaths("task_outcome"))
              .build();
      DeliveryServiceGrpc.DeliveryServiceBlockingStub authenticatedDeliveryService =
          grpcServiceProvider.getAuthenticatedDeliveryService();
      Task responseTask = authenticatedDeliveryService.updateTask(updateReq);
      servletState.addTask(responseTask);

      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");
      ServletUtils.writeProtoJson(response.getWriter(), responseTask);
      response.getWriter().flush();
    }
  }
}
