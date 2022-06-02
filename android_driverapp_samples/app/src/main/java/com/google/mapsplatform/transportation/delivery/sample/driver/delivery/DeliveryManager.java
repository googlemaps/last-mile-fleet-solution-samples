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
package com.google.mapsplatform.transportation.delivery.sample.driver.delivery;

import static com.google.mapsplatform.transportation.delivery.sample.driver.domain.vehicle.VehicleStopState.ARRIVED;
import static com.google.mapsplatform.transportation.delivery.sample.driver.domain.vehicle.VehicleStopState.COMPLETED;
import static com.google.mapsplatform.transportation.delivery.sample.driver.domain.vehicle.VehicleStopState.ENROUTE;
import static com.google.mapsplatform.transportation.delivery.sample.driver.domain.vehicle.VehicleStopState.NEW;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.Task;
import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.VehicleStop.VehicleStopState;
import com.google.android.libraries.mapsplatform.transportation.driver.api.delivery.data.DeliveryTask;
import com.google.android.libraries.navigation.Waypoint;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.mapsplatform.transportation.delivery.sample.driver.backend.DeliveryBackend;
import com.google.mapsplatform.transportation.delivery.sample.driver.config.DeliveryConfig;
import com.google.mapsplatform.transportation.delivery.sample.driver.domain.common.WaypointConfig;
import com.google.mapsplatform.transportation.delivery.sample.driver.domain.task.DeliveryTaskConfig;
import com.google.mapsplatform.transportation.delivery.sample.driver.domain.task.TaskOutcome;
import com.google.mapsplatform.transportation.delivery.sample.driver.domain.vehicle.AppVehicleStop;
import com.google.mapsplatform.transportation.delivery.sample.driver.domain.vehicle.VehicleStopConfig;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The delivery manager is responsible for holding the logic necessary to create and maintain
 * information related to a single delivery such as the list of stops, current stop and more.
 */
public class DeliveryManager {

  public static DeliveryManager manager;

  private static final String TAG = DeliveryManager.class.getName();

  private static final Logger logger = Logger.getLogger(TAG);

  /** List of updated stops on the current delivery. */
  private final MutableLiveData<ImmutableList<AppVehicleStop>> stops;
  /** The current stop of the stop chain. */
  private final LiveData<Optional<AppVehicleStop>> currentStop;
  /** Delivery backend instance. Should be used for untrusted role communication. */
  @Nullable private DeliveryBackend backend;
  /** The config object should only be used as a reference to fill fields returned by backend. */
  @Nullable private DeliveryConfig config;
  /** The provider id. */
  private String providerId;

  private List<String> remainingStopIds;

  /**
   * A bijective mapping of stop IDs to the VehicleStop objects. Used for marking stops as complete
   * and reordering them.
   */
  private HashBiMap<String, AppVehicleStop> stopIdMap;

  /**
   * A mapping of taskIds to DeliveryTaskConfig objects. Used to obtain data about tasks that aren't
   * stored in Driver SDK objects.
   */
  private ImmutableMap<String, DeliveryTaskConfig> taskConfigMap;

  public static DeliveryManager getInstance() {
    if (manager == null) {
      manager = new DeliveryManager();
    }
    return manager;
  }

  /** Constructor of {@link DeliveryManager}. */
  public DeliveryManager() {
    this(ImmutableList.of());
  }

  /**
   * Constructor of {@link DeliveryManager}.
   *
   * @param stops list of initial stops.
   */
  public DeliveryManager(ImmutableList<AppVehicleStop> stops) {
    this.stops = new MutableLiveData<>(stops);
    currentStop = Transformations.map(this.stops, DeliveryManager::getNextStop);
  }

  public LiveData<ImmutableList<AppVehicleStop>> getStops() {
    return stops;
  }

  /**
   * Gets the next stop of the stop list. The next stop is considered the first element at the stop
   * list that is not on {@link VehicleStopState#ARRIVED} state.
   *
   * @param stops list of stops.
   * @return next stop.
   */
  private static final Optional<AppVehicleStop> getNextStop(ImmutableList<AppVehicleStop> stops) {
    if (stops.isEmpty()) {
      logger.warning("Next stop was requested, but there is no stop remaining.");
      return Optional.absent();
    }

    Collection<AppVehicleStop> remainingStops =
        Collections2.filter(
            stops,
            Predicates.compose(
                Predicates.not(Predicates.equalTo(COMPLETED.getCode())),
                AppVehicleStop::getVehicleStopState));

    if (remainingStops.isEmpty()) {
      logger.warning("All stops were completed. There is no stop remaining.");
      return Optional.absent();
    }

    return Optional.of(ImmutableList.copyOf(remainingStops).get(0));
  }

  /**
   * Update the current stop to new in the internal stop management. At the end of the operation
   * the stop list is updated to reflect the new states.
   */
  public void notArrivingAtStop() {
    AppVehicleStop stop = updateStopStatus(NEW);

    if (stop != null) {
      logger.info(String.format("Stop state: new (waypoint: %s).", stop.getWaypoint().getTitle()));
    }
  }

  /**
   * Update the current stop to enroute in the internal stop management. At the end of the operation
   * the stop list is updated to reflect the new states.
   */
  public void enrouteToStop() {
    AppVehicleStop stop = updateStopStatus(ENROUTE);

    if (stop != null) {
      logger.info(String.format("Enroute to stop (waypoint: %s).", stop.getWaypoint().getTitle()));
    }
  }

  /**
   * Update the current stop to arrived in the internal stop management. At the end of the operation
   * the stop list is updated to reflect the new states.
   */
  public void arrivedAtStop() {
    AppVehicleStop stop = updateStopStatus(ARRIVED);

    if (stop != null) {
      logger.info(String.format("Arrived to stop (waypoint: %s).", stop.getWaypoint().getTitle()));
    }
  }

  /**
   * Completes the stop in the internal stop management. At the end of the operation the stop list
   * is updated to reflect the new states.
   */
  public void completeStop() {
    AppVehicleStop stop = updateStopStatus(COMPLETED);

    if (stop != null) {
      logger.info(
          String.format("Stop (waypoint: %s) has been completed.", stop.getWaypoint().getTitle()));
    }
  }

  /**
   * Updates the status of the current stop and returns the updated object, if set.
   *
   * @param state new stop status.
   */
  @Nullable
  private AppVehicleStop updateStopStatus(
      com.google.mapsplatform.transportation.delivery.sample.driver.domain.vehicle.VehicleStopState state) {

    ImmutableList<AppVehicleStop> allStops = stops.getValue();
    Optional<AppVehicleStop> optionalStop = getNextStop(allStops);

    // This flow purpose is to primarily keep the internal stop list with states up-to-date with
    // UI interactions. It could potentially be extended to additional verifications such as task
    // states and state auto-fill.
    if (optionalStop.isPresent()) {
      AppVehicleStop stop = optionalStop.get();
      AppVehicleStop updatedStop = stop.toBuilder().setVehicleStopState(state.getCode()).build();

      List<AppVehicleStop> updatedStops = new ArrayList<>(allStops);
      updatedStops.set(allStops.indexOf(stop), updatedStop);
      stops.setValue(ImmutableList.copyOf(updatedStops));
      stopIdMap.put(stopIdMap.inverse().get(stop), updatedStop);

      // When the untrusted role is enabled we also need to update the new vehicle stop state
      // remotely on the delivery backend server since the SDK API call is being skipped.
      // COMPLETED is not a valid Fleet Engine vehicle stop state so we only call the backend
      // on NEW, ENROUTE or ARRIVED states.
      if (NEW.equals(state) || ENROUTE.equals(state) || ARRIVED.equals(state)) {
        updateRemoteVehicleState(state.name());
      }

      return updatedStop;
    }

    return null;
  }

  /**
   * Updates the vehicle stop on the remote delivery backend server with a given state name.
   *
   * @param state name of the state to be updated. Must comply with {@link VehicleStopState}.
   */
  private void updateRemoteVehicleState(String state) {
    ListenableFuture<Boolean> future = backend.updateNextStopState(state);

    Futures.addCallback(
        future,
        // Failure on updating next stop state should be relatively rare, occurring mostly when the
        // delivery backend server is unresponsive, or experiencing internal errors when talking to
        // Fleet Engine. For this reason we opt to throw the illegal state exception rather than
        // trying to recover & retry. If this become a frequent problem during tests this logic can
        // be updated to a more sophisticated approach.
        new FutureCallback<Boolean>() {
          @Override
          public void onSuccess(Boolean result) {
            if (!result) {
              String msg = String.format("Could not update stop state remotely.");
              logger.log(Level.SEVERE, msg);
              throw new IllegalStateException(msg);
            }

            logger.info(String.format("Stop state successfully updated to %s.", state));
          }

          @Override
          public void onFailure(Throwable t) {
            String msg = "Could not update stop state remotely.";
            logger.log(Level.SEVERE, msg, t);
            throw new IllegalStateException(msg);
          }
        },
        MoreExecutors.directExecutor());
  }

  /** Setter for the list of vehicle stops. */
  public void setStops(ImmutableList<AppVehicleStop> stops) {
    this.stops.setValue(stops);
  }

  /**
   * Creates a delivery itinerary using the untrusted role on a delivery backend server.
   *
   * @param config delivery configuration to be created.
   */
  public void setupUntrustedDelivery(DeliveryConfig config) {
    if (backend == null) {
      throw new IllegalStateException("Delivery backend is not defined.");
    }

    this.config = config;
    convertVehicleStops();
  }

  private Waypoint convertWaypoint(WaypointConfig waypointConfig) {
    return Waypoint.builder().setTitle(waypointConfig.title())
        .setLatLng(waypointConfig.latitude(), waypointConfig.longitude()).build();
  }

  private void convertVehicleStops() {
    // Convert the list of tasks to a map to ease lookup.
    List<DeliveryTask> tasks = Lists.transform(config.deliveryTasks(),
        task ->
            DeliveryTask.builder(providerId, task.taskId())
                .setTaskId(task.taskId())
                .setTaskType(task.taskType().getCode())
                .setPlannedWaypoint(convertWaypoint(task.plannedWaypoint()))
                .setTaskDurationSeconds(task.durationSeconds())
                .build()
    );
    Map<String, DeliveryTask> taskMap = Maps.uniqueIndex(tasks, task -> task.getTaskId());
    taskConfigMap = Maps.uniqueIndex(config.deliveryTasks(), taskConfig -> taskConfig.taskId());

    stopIdMap = HashBiMap.create();
    // Only create stops with IDs listed in config.remainingStopIdList.
    for (VehicleStopConfig stopConfig : config.vehicleStops()) {
      List<Task> vehicleTasks = Lists.transform(
          stopConfig.tasks(),
          taskId -> {
            DeliveryTask task = taskMap.get(taskId);
            if (task == null) {
              logger.log(Level.SEVERE, "Task " + taskId + " not found in task list");
            }
            return task;
          }
      );
      // Skip stops that don't have any tasks assigned.
      if (vehicleTasks.isEmpty()) {
        continue;
      }
      stopIdMap.put(stopConfig.stopId(), AppVehicleStop.builder()
          .setWaypoint(convertWaypoint(stopConfig.plannedWaypoint()))
          .setTasks(vehicleTasks)
          .build());
    }

    // Use remainingStopIdList to generate the sequence of stops.
    ArrayList<AppVehicleStop> vehicleStops = new ArrayList<>();
    for (String stopId : config.remainingStopIdList()) {
      AppVehicleStop stop = stopIdMap.get(stopId);

      // remainingStopIdList has a stopId that doesn't match anything in stops. This is an error.
      if (stop == null) {
        logger.log(Level.SEVERE, "Stop " + stopId + " not found in stop list");
        continue;
      }
      vehicleStops.add(stop);
    }
    this.stops.postValue(ImmutableList.copyOf(vehicleStops));
    this.remainingStopIds = config.remainingStopIdList();
  }

  public void refreshStopsList(@Nullable UpdateStopsCallback callback) {
    updateStopsList(getStops().getValue(), callback);
  }

  public void updateStopsList(List<AppVehicleStop> stops, @Nullable UpdateStopsCallback callback) {
    List<String> stopIds = getRemainingStopIds(stops);

    // Only fire an update if stopIds has changed compared to what's on file.
    if (stopIds.equals(remainingStopIds)) {
      return;
    }
    ListenableFuture<Boolean> future = backend.updateStopIdList(stopIds);
    Futures.addCallback(future, new FutureCallback<Boolean>() {
      @Override
      public void onSuccess(@Nullable Boolean result) {
        if (result) {
          DeliveryManager.this.stops.postValue(ImmutableList.copyOf(stops));
          remainingStopIds = stopIds;
          if (callback != null) {
            callback.run(null);
          }
        } else {
          DeliveryManager.this.stops.postValue(DeliveryManager.this.stops.getValue());
        }
      }

      @Override
      public void onFailure(Throwable t) {
        DeliveryManager.this.stops.postValue(DeliveryManager.this.stops.getValue());
        callback.run(t);
      }
    }, MoreExecutors.directExecutor());
  }

  public void updateTaskOutcome(List<Task> tasks, boolean successful,
      @Nullable UpdateTaskOutcomeCallback callback) {
    TaskOutcome outcome = successful ? TaskOutcome.SUCCEEDED : TaskOutcome.FAILED;
    List<ListenableFuture<Boolean>> futures = new ArrayList<>();
    for (Task task : tasks) {
      futures.add(backend.updateTaskOutcome(task.getTaskId(), outcome.name()));
    }
    ListenableFuture<List<Boolean>> taskOutcomesFuture = Futures.successfulAsList(futures);
    Futures.addCallback(taskOutcomesFuture, new FutureCallback<List<Boolean>>() {
      @Override
      public void onSuccess(@Nullable List<Boolean> result) {
        List<String> failedTaskIds = new ArrayList<>();
        for (int i = 0; i < result.size(); i++) {
          if (result.get(i) == null || result.get(i) == false) {
            failedTaskIds.add(tasks.get(i).getTaskId());
          } else {
            // Update the corresponding task in the cached vehicleStop.
            // TODO: setTaskOutcomeTimestamp should contain the time of the request instead.
            updateTaskInStop(
                ((DeliveryTask) tasks.get(i)).toBuilder().setTaskOutcome(
                    successful ? Task.TaskOutcome.SUCCEEDED : Task.TaskOutcome.FAILED)
                    .setTaskOutcomeTimestamp(System.currentTimeMillis()).build());
          }
        }
        if (callback != null) {
          callback.run(failedTaskIds);
        }
      }

      @Override
      public void onFailure(Throwable t) {
        // the future created by successfulAsList shouldn't have an onFailure mode.
      }
    }, MoreExecutors.directExecutor());
  }

  private void updateTaskInStop(Task updatedTask) {
    List<AppVehicleStop> stops = new ArrayList<>(getStops().getValue());

    boolean taskFound = false;
    for (int s = 0; s < stops.size(); s ++) {
      List<Task> tasks = new ArrayList<>(stops.get(s).getTasks());
      for (int i = 0; i < tasks.size(); i ++) {
        if (tasks.get(i).getTaskId() == updatedTask.getTaskId()) {
          tasks.set(i, updatedTask);
          taskFound = true;
          break;
        }
      }
      if (taskFound) {
        AppVehicleStop originalStop = stops.get(s);
        String stopId = stopIdMap.inverse().get(originalStop);
        AppVehicleStop.Builder updatedStopBuilder = originalStop.toBuilder().setTasks(tasks);

        // Set the stop state to complete if all its tasks have outcomes.
        if (tasks.stream()
            .allMatch(task -> task.getTaskOutcome() != Task.TaskOutcome.UNSPECIFIED)) {
          updatedStopBuilder.setVehicleStopState(COMPLETED.getCode());
        }
        AppVehicleStop updatedStop = updatedStopBuilder.build();
        stopIdMap.put(stopId, updatedStop);
        stops.set(s, updatedStop);
        break;
      }
    }
    if (taskFound) {
      ImmutableList<AppVehicleStop> immutableStops = ImmutableList.copyOf(stops);
      this.stops.setValue(immutableStops);
    }
  }

  private List<String> getRemainingStopIds(List<AppVehicleStop> stops) {
    ArrayList<String> remainingStopIds = new ArrayList<>();

    for (AppVehicleStop stop : stops) {
      if (stop.getTasks().stream()
          .anyMatch(task -> task.getTaskOutcome() == Task.TaskOutcome.UNSPECIFIED)) {
        remainingStopIds.add(stopIdMap.inverse().get(stop));
      }
    }
    return remainingStopIds;
  }

  /** Setter for the delivery backend. */
  public void setDeliveryBackend(DeliveryBackend backend, String providerId) {
    this.backend = backend;
    this.providerId = providerId;
  }

  public DeliveryBackend getBackend() {
    return backend;
  }

  public DeliveryTaskConfig getTaskConfig(String taskId) {
    if (taskConfigMap != null) {
      return taskConfigMap.get(taskId);
    }
    return null;
  }

  /** Callback for the remote getNextStop method. */
  public interface GetNextStopCallback {
    void run(Optional<AppVehicleStop> stopOptional);
  }

  public interface UpdateTaskOutcomeCallback {
    void run(List<String> failedTaskIds);
  }

  public interface UpdateStopsCallback {
    void run(Throwable throwable);
  }
}
