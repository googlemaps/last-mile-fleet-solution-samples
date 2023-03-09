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

import com.example.backend.json.BackendConfig;
import google.maps.fleetengine.delivery.v1.DeliveryVehicle;
import google.maps.fleetengine.delivery.v1.Task;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Singleton;

/**
 * Shared state among the servlets.
 *
 * <p>Stores data related to the most recently uploaded manifest.
 *
 * <p>This class demonstrates the kind of data stored in a backend used for LMFS, and should not be
 * taken as a reference of a production environment.
 */
@Singleton
class ServletState {
  private final Logger logger = Logger.getLogger(ServletState.class.getName());
  private BackendConfig backendConfig;
  private HashMap<String, Task> tasks;
  private HashMap<String, DeliveryVehicle> deliveryVehicles;
  private HashMap<String, String> clientToDeliveryVehicleMapping;

  public ServletState() {
    this.tasks = new HashMap<>();
    this.deliveryVehicles = new HashMap<>();
    this.clientToDeliveryVehicleMapping = new HashMap<>();
  }

  /** Adds a delivery vehicle into the servlet state. */
  public synchronized void addDeliveryVehicle(DeliveryVehicle deliveryVehicle) {
    this.deliveryVehicles.put(getId(deliveryVehicle.getName()), deliveryVehicle);
  }

  /** Retrieves a delivery vehicle by ID. Null if vehicle ID doesn't match any vehicle. */
  public synchronized DeliveryVehicle getDeliveryVehicleById(String vehicleId) {
    return deliveryVehicles.get(vehicleId);
  }

  /**
   * Adds a task into the servlet state. If a task with the ID is already present, it is
   * overwritten.
   */
  public synchronized void addTask(Task task) {
    this.tasks.put(getId(task.getName()), task);
  }

  /** Retrieves a task by ID. Null if task ID doesn't match any task. */
  public synchronized Task getTaskById(String taskId) {
    return tasks.get(taskId);
  }

  /**
   * Adds a client into the assignment list. The client is the courier servicing this set of tasks.
   */
  public synchronized void addClientToDeliveryVehicleMap(String clientId, DeliveryVehicle vehicle) {
    String vehicleId = getId(vehicle.getName());
    if (!isDeliveryVehicleAssigned(vehicleId)) {
      clientToDeliveryVehicleMapping.put(clientId, vehicleId);
      getManifest(vehicleId).clientId = clientId;
    }
  }

  /** Retrieves the vehicle mapped to a client. */
  public synchronized String getDeliveryVehicleMapByClient(String clientIdentifier) {
    return clientToDeliveryVehicleMapping.get(clientIdentifier);
  }

  /** Returns true if the vehicle is mapped to a client. */
  public synchronized boolean isDeliveryVehicleAssigned(String vehicleId) {
    return clientToDeliveryVehicleMapping.containsValue(vehicleId);
  }

  /** Retrieves any available (unassigned) vehicle. If all vehicles are assigned, returns null. */
  public synchronized DeliveryVehicle getAnyAvailableDeliveryVehicle() {
    for (DeliveryVehicle vehicle : deliveryVehicles.values()) {
      if (!isDeliveryVehicleAssigned(getId(vehicle.getName()))) {
        return vehicle;
      }
    }
    return null;
  }

  public synchronized void setBackendConfig(BackendConfig backendConfig) {
    this.backendConfig = backendConfig;
  }

  public synchronized BackendConfig.Manifest getManifest(String vehicleId) {
    logger.log(Level.INFO, String.format("getting manifest for %s", vehicleId));
    if (backendConfig == null) {
      return null;
    }
    for (BackendConfig.Manifest manifest : backendConfig.manifests) {
      if (manifest.vehicle.vehicleId.equals(vehicleId)) {
        return manifest;
      }
    }
    return null;
  }

  public synchronized BackendConfig.Task getBackendConfigTask(String taskId) {
    if (backendConfig == null) {
      return null;
    }
    for (BackendConfig.Manifest manifest : backendConfig.manifests) {
      for (BackendConfig.Task task : manifest.tasks) {
        if (task.taskId.equals(taskId)) {
          return task;
        }
      }
    }
    return null;
  }

  public synchronized BackendConfig.Task getBackendConfigTaskByTrackingId(String trackingId) {
    if (backendConfig == null) {
      return null;
    }
    for (BackendConfig.Manifest manifest : backendConfig.manifests) {
      for (BackendConfig.Task task : manifest.tasks) {
        if (task.trackingId.equals(trackingId)) {
          return task;
        }
      }
    }
    return null;
  }

  public synchronized void removeBackendConfigTask(String taskId) {
    for (BackendConfig.Manifest manifest : backendConfig.manifests) {
      for (BackendConfig.Stop stop : manifest.stops) {
        ArrayList<String> tasksList = new ArrayList<>(Arrays.asList(stop.tasks));
        tasksList.remove(taskId);
        stop.tasks = tasksList.toArray(new String[tasksList.size()]);
      }
    }
  }

  /**
   * Resets the delivery state (tasks, vehicles, assignments). Use this when uploading a new
   * backendConfig.
   */
  public synchronized void clearDeliveryState() {
    this.tasks.clear();
    this.deliveryVehicles.clear();
    this.clientToDeliveryVehicleMapping.clear();
  }

  /**
   * Returns a (vehicle, task) ID from its name by stripping away the backend ID and other constant
   * elements.
   */
  public static String getId(String name) {
    String[] nameParts = name.split("/");
    return nameParts[nameParts.length - 1];
  }
}
