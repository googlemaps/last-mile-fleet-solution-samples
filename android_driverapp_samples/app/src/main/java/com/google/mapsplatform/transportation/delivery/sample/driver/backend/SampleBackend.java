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
package com.google.mapsplatform.transportation.delivery.sample.driver.backend;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.content.Context;
import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkInfo.State;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import com.google.common.base.VerifyException;
import com.google.common.io.CharStreams;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.mapsplatform.transportation.delivery.sample.driver.MainActivity;
import com.google.mapsplatform.transportation.delivery.sample.driver.settings.Preferences;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * This class communicates with the sample backend server, handling operations such as creating a
 * signed JSON Web Token (JWT) and updating vehicle / task data.
 */
public class SampleBackend implements DeliveryBackend {

  private static final Logger logger = Logger.getLogger(SampleBackend.class.getName());

  /** The host prefix for the backend. E.g. "http://localhost:8080/somePath". */
  public Uri backendHostPrefix;

  /** Endpoint to get the manifest from. */
  public Uri manifestEndpoint;

  /** Endpoint to update tasks by ID. */
  public Uri taskEndpoint;

  /** Endpoint to update vehicle manifest data by vehicle ID. */
  public Uri vehicleEndpoint;

  /** Endpoint to fetch untrusted driver tokens by vehicle ID. */
  public Uri tokenEndpoint;

  /** ID of the vehicle on Fleet Engine. */
  private String vehicleId = "";

  /** Gson helper. */
  private static final Gson gson = new Gson();

  /** Activity context. */
  private final Context context;

  /** Work manager for running network requests. */
  private final WorkManager workManager;

  /** A reference to the preference object. */
  private final Preferences preferences;

  /** Constructor of {@link SampleBackend}. */
  public SampleBackend(Context context) {
    this.context = context;
    this.preferences = Preferences.getInstance(context);
    this.workManager = WorkManager.getInstance(context);

    // Fetch the backend host and set the endpoints.
    initializeEndpointUrls(preferences.getBackendUrl());
  }

  public void setVehicleId(String vehicleId) {
    this.vehicleId = vehicleId;
  }

  /**
   * Fetches a JSON manifest from the manifest endpoint.
   */
  public ListenableFuture<JsonObject> getDeliveryConfigJson() {
    SettableFuture<JsonObject> future = SettableFuture.create();

    // This endpoint is not run via WorkManager because its response can exceed the worker maximum
    // of 10kB.
    runOnNewThread(
        () -> {
          try {
            JsonObject response =
                gson.fromJson(
                    postWithPayload(
                        manifestEndpoint.toString(),
                        String.format("{\"client_id\":\"%s\"}", preferences.getClientId())
                            .getBytes(UTF_8)),
                    JsonObject.class);
            future.set(response);
          } catch (IOException e) {
            logger.log(Level.SEVERE, "Error fetching manifest from test backend.", e);
            future.setException(e);
          }
        });
    return future;
  }

  @Override
  public ListenableFuture<String> getDriverToken(String vehicleId) {
    return getTokenString(Uri.withAppendedPath(tokenEndpoint, vehicleId).toString());
  }

  /**
   * Updates the next stop state.
   * @param state stop state to be used.
   * @return a future that resolves to Boolean.TRUE when the stop state is updated. If the request
   *   failed, the future contains the exception thrown.
   */
  @Override
  public ListenableFuture<Boolean> updateNextStopState(String state) {
    SettableFuture<Boolean> future = SettableFuture.create();

    // Set ignoreOutput here to prevent serialization from getting too large.
    schedulePost(
        Uri.withAppendedPath(vehicleEndpoint, vehicleId).toString(),
        String.format("{\"current_stop_state\":\"%s\"}", state).getBytes(UTF_8),
        workInfo -> {
          if (workInfo.getState() == State.SUCCEEDED) {
            future.set(Boolean.TRUE);
          } else {
            String msg = String.format("Error updating next stop state: %s",
                workInfo.getOutputData().getString("error"));
            logger.log(Level.SEVERE, msg);
            future.setException(new Throwable(msg));
          }
        },
        /* ignoreOutput= */ true);
    return future;
  }

  /**
   * Updates a task outcome.
   * @param taskId ID of the task being closed.
   * @param taskOutcome Outcome of the task being closed.
   * @return A future that returns Boolean.TRUE when the request succeeds. If the request fails,
   *   the future contains the exception thrown.
   */
  @Override
  public ListenableFuture<Boolean> updateTaskOutcome(String taskId, String taskOutcome) {
    SettableFuture<Boolean> future = SettableFuture.create();

    // Set ignoreOutput here to prevent serialization from getting too large.
    schedulePost(
        Uri.withAppendedPath(taskEndpoint, taskId).toString(),
        String.format("{\"task_outcome\":\"%s\"}", taskOutcome).getBytes(UTF_8),
        workInfo -> {
          if (workInfo.getState() == State.SUCCEEDED) {
            future.set(Boolean.TRUE);
          } else {
            String msg = String.format("Error updating task outcome: %s",
                workInfo.getOutputData().getString("error"));
            logger.log(Level.SEVERE, msg);
            future.setException(new Throwable(msg));
          }
        },
        /* ignoreOutput= */true);
    return future;
  }

  /**
   * Updates the list of stop IDs. Used to mark a stop as complete, or resequence upcoming stops.
   * @param stopIdList The list of stop IDs.
   * @return a future that returns Boolean.TRUE when the request succeeds. If the request fails,
   *   the future contains the exception thrown.
   */
  @Override
  public ListenableFuture<Boolean> updateStopIdList(List<String> stopIdList) {
    SettableFuture<Boolean> future = SettableFuture.create();

    String stopIdsString = stopIdList.stream().map(stopId -> String.format("\"%s\"", stopId))
        .collect(Collectors.joining(", "));
    logger.log(Level.INFO, String.format("updating stop id list with %s", stopIdsString));

    schedulePost(
        Uri.withAppendedPath(vehicleEndpoint, vehicleId).toString(),
        String.format("{\"remaining_stop_id_list\": [%s]}", stopIdsString).getBytes(UTF_8),
        workInfo -> {
          if (workInfo.getState() == State.SUCCEEDED) {
            future.set(Boolean.TRUE);
          } else {
            String msg = String.format("Error updating stop ID list: %s",
                workInfo.getOutputData().getString("error"));
            logger.log(Level.SEVERE, msg);
            future.setException(new Throwable(msg));
          }
        },
        /* ignoreOutput= */ true);
    return future;
  }

  /**
   * Runs callback on a separate thread.
   *
   * <p>NOTE: starting a thread in Java is expensive. Use sparingly.
   */
  private static void runOnNewThread(Runnable r) {
    new Thread(r).start();
  }

  /**
   * Schedules a get request using the work manager.
   *
   * <p>This method isn't used by any backend request at the moment, but it is implemented here for
   * completeness.
   *
   * @param url The URL to call.
   * @param callback The callback executed when the work request completes.
   * @param ignoreOutput If true, the body of the get request is ignored.
   */
  private void scheduleGet(
      String url, @Nullable ScheduleRequestCompleteCallback callback, boolean ignoreOutput) {
    // Schedule the work. The work will proceed when network is available.
    // If the app is killed while the work is being run, it will still finish.
    WorkRequest networkWorkRequest = new OneTimeWorkRequest.Builder(GetWorker.class)
        .setInputData(
            new Data.Builder().putString("url", url).putBoolean("ignoreOutput", ignoreOutput)
                .build())
        .setConstraints(
            new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build();
    workManager.enqueue(networkWorkRequest);
    LiveData<WorkInfo> liveData = workManager.getWorkInfoByIdLiveData(networkWorkRequest.getId());
    liveData.observe((MainActivity) context, workInfo -> {
      if (workInfo.getState() != null && workInfo.getState().isFinished()) {
        if (callback != null) {
          callback.run(workInfo);
        }
        liveData.removeObservers((MainActivity) context);
      }
    });
  }

  /**
   * Schedules a post request using the work manager.
   * @param url The URL to call.
   * @param payload The payload to be placed in the body of the post request.
   * @param callback The callback executed when the work request completes.
   * @param ignoreOutput If true, the body of the get request is ignored.
   */
  private void schedulePost(String url, byte[] payload,
      @Nullable ScheduleRequestCompleteCallback callback, boolean ignoreOutput) {
    // Schedule the work. The work will proceed when network is available.
    // If the app is killed while the work is being run, it will still finish.
    WorkRequest networkWorkRequest = new OneTimeWorkRequest.Builder(PostWorker.class)
        .setInputData(
            new Data.Builder().putString("url", url).putByteArray("payload", payload)
                .putBoolean("ignoreOutput", ignoreOutput).build())
        .setConstraints(
            new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build();
    workManager.enqueue(networkWorkRequest);
    LiveData<WorkInfo> liveData = workManager.getWorkInfoByIdLiveData(networkWorkRequest.getId());
    liveData.observe((MainActivity) context, workInfo -> {
      if (workInfo.getState() != null && workInfo.getState().isFinished()) {
        if (callback != null) {
          callback.run(workInfo);
        }
        liveData.removeObservers((MainActivity) context);
      }
    });
  }

  private ListenableFuture<String> getTokenString(String tokenUrl) {
    SettableFuture<String> future = SettableFuture.create();

    try {
      JsonObject response = fetchToken(tokenUrl);
      TokenEndpointResponse json = gson.fromJson(response, TokenEndpointResponse.class);

      String token = json.token;
      logger.info(String.format("Created token %s for vehicle %s", token, vehicleId));
      future.set(token);
      return future;
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Error fetching token from backend.", e);
    }

    throw new VerifyException("Could not retrieve token.");
  }

  public static String postWithPayload(String url, byte[] payload) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
    connection.setDoInput(true);
    connection.setDoOutput(true);
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Content-Type", "application/json; utf-8");
    connection.setConnectTimeout(5000);

    OutputStream outputStream = connection.getOutputStream();
    outputStream.write(payload, 0, payload.length);
    outputStream.close();

    String response = CharStreams
        .toString(new InputStreamReader(connection.getInputStream(), UTF_8));
    connection.disconnect();
    return response;
  }

  /**
   * Issues a GET request and returns the response body.
   * @param url
   * @return
   * @throws IOException
   */
  public static String get(String url) throws IOException {
    logger.log(Level.INFO, "Starting get...");
    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
    connection.setDoInput(true);
    connection.setRequestMethod("GET");
    connection.setConnectTimeout(5000);
    logger.log(Level.INFO, String.valueOf(connection.getResponseCode()));

    String response = CharStreams
        .toString(new InputStreamReader(connection.getInputStream(), UTF_8));
    connection.disconnect();
    return response;
  }

  private JsonObject fetchToken(String tokenUrl) throws IOException {
    return gson.fromJson(get(tokenUrl), JsonObject.class);
  }

  /** Response from the backend's token endpoint. */
  private static class TokenEndpointResponse {

    @SerializedName("token")
    String token;
  }

  /** Callback executed when a scheduled request work item completes. */
  private interface ScheduleRequestCompleteCallback {
    void run(WorkInfo workInfo);
  }

  /** Sets the endpoint URLs. */
  private void initializeEndpointUrls(String hostPrefix) {
    backendHostPrefix = Uri.parse(hostPrefix);
    manifestEndpoint = Uri.withAppendedPath(backendHostPrefix, "manifest");
    taskEndpoint = Uri.withAppendedPath(backendHostPrefix, "task");
    vehicleEndpoint = Uri.withAppendedPath(backendHostPrefix, "manifest");
    tokenEndpoint = Uri.withAppendedPath(backendHostPrefix, "token/delivery_driver");
  }
}
