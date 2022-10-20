/* Copyright 2020 Google LLC
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
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.example.backend.json.BackendConfigGsonProvider;
import com.example.backend.auth.grpcservice.AuthenticatedGrpcServiceProvider;
import google.maps.fleetengine.delivery.v1.DeliveryServiceGrpc;
import google.maps.fleetengine.delivery.v1.GetTaskRequest;
import google.maps.fleetengine.delivery.v1.GetDeliveryVehicleRequest;
import com.example.backend.utils.TaskUtils;
import com.example.backend.utils.VehicleUtils;
import google.maps.fleetengine.delivery.v1.DeliveryServiceGrpc;

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
  private DatastoreService datastore;

  public ServletState() {
    this.tasks = new HashMap<>();
    this.deliveryVehicles = new HashMap<>();
    this.clientToDeliveryVehicleMapping = new HashMap<>();
    this.datastore = DatastoreServiceFactory.getDatastoreService();
  }

  /** Adds a delivery vehicle into the servlet state. */
  public synchronized void addDeliveryVehicle(DeliveryVehicle deliveryVehicle) {
    //this.deliveryVehicles.put(getId(deliveryVehicle.getName()), deliveryVehicle);
  }

  /** Retrieves a delivery vehicle by ID. Null if vehicle ID doesn't match any vehicle. */
  public synchronized DeliveryVehicle getDeliveryVehicleById(DeliveryServiceGrpc.DeliveryServiceBlockingStub authenticatedDeliveryService, 
        String vehicleId) {
      // TODO: need to prepend provide//... stuf
      GetDeliveryVehicleRequest req =
          GetDeliveryVehicleRequest.newBuilder().setName(VehicleUtils.getVehicleNameFromId(vehicleId)).build();
      return authenticatedDeliveryService.getDeliveryVehicle(req);
  }

  /**
   * Adds a task into the servlet state. If a task with the ID is already present, it is
   * overwritten.
   */
  public synchronized void addTask(Task task) {
    //this.tasks.put(getId(task.getName()), task);
  }

  /** Retrieves a task by ID. Null if task ID doesn't match any task. */
  public synchronized Task getTaskById(DeliveryServiceGrpc.DeliveryServiceBlockingStub authenticatedDeliveryService, String taskId) {
      // Fetch the task from Fleet Engine, just in case.
      GetTaskRequest req =
          GetTaskRequest.newBuilder().setName(TaskUtils.getTaskNameFromId(taskId)).build();
      return authenticatedDeliveryService.getTask(req);
  }

  public synchronized String lookupAndAssignVehicle(String clientIdentifier) {
   Query q = new Query("VehicleManifest")
        .setFilter(new FilterPredicate("clientIdentifier", FilterOperator.EQUAL, clientIdentifier));
   PreparedQuery pq = datastore.prepare(q);
   Entity vehicleManifest = pq.asSingleEntity();
   if (vehicleManifest != null) {
      String vehicleId = vehicleManifest.getKey().getName();
      vehicleManifest.setProperty("assigned", true);
      this.datastore.put(vehicleManifest);
      return vehicleId;
   } else {
      return null;
   }
  }

  /** Retrieves the vehicle mapped to a client. */
  public synchronized String getDeliveryVehicleMapByClient(String clientIdentifier) {
   Query q = new Query("VehicleManifest")
        .setFilter(new FilterPredicate("clientIdentifier", FilterOperator.EQUAL, clientIdentifier));
   PreparedQuery pq = datastore.prepare(q);
   Entity result = pq.asSingleEntity();
   if (result != null) {
      return result.getKey().getName();
   } else {
      return null;
   }
  }

  /** Returns true if the vehicle is mapped to a client. */
  public synchronized boolean isDeliveryVehicleAssigned(String vehicleId) {
    try {
       Key vehicleManifestKey = KeyFactory.createKey("VehicleManifest", vehicleId);
       Entity vehicleManifest = this.datastore.get(vehicleManifestKey);
       // TODO: is this just really has a clientIdentifier set ... I think so
       Boolean assigned = (Boolean)vehicleManifest.getProperty("assigned");
       return assigned;
    } catch (EntityNotFoundException e) {
       return false;
    }
    //return clientToDeliveryVehicleMapping.containsValue(vehicleId);
  }


  /** Retrieves any available (unassigned) vehicle. If all vehicles are assigned, returns null. */
  public synchronized DeliveryVehicle getAnyAvailableDeliveryVehicle() {
    return null;
    /*
    for (DeliveryVehicle vehicle : deliveryVehicles.values()) {
      if (!isDeliveryVehicleAssigned(getId(vehicle.getName()))) {
        return vehicle;
      }
    }
    return null;
    */
  }

  public synchronized void setBackendConfig(BackendConfig backendConfig) {
    this.backendConfig = backendConfig;
    for (BackendConfig.Manifest manifest : backendConfig.manifests) {
      logger.log(Level.INFO, String.format("got manifest for %s", manifest.vehicle.vehicleId));
      Entity vehicleManifest = new Entity("VehicleManifest", manifest.vehicle.vehicleId);
      logger.log(Level.INFO, String.format("got key for %s", vehicleManifest.getKey().getName()));
      vehicleManifest.setProperty("json", new Text(BackendConfigGsonProvider.get().toJson(manifest)));
      vehicleManifest.setProperty("clientIdentifier", manifest.expectedClientId);
      vehicleManifest.setProperty("assigned", false);
      this.datastore.put(vehicleManifest);
    }
  }

  public synchronized void updateManifest(BackendConfig.Manifest m) {
    String vehicleId = m.vehicle.vehicleId;
    logger.log(Level.INFO, String.format("Setting manifest for %s", vehicleId));
    Key vehicleManifestKey = KeyFactory.createKey("VehicleManifest",vehicleId);
    try {
       Entity vehicleManifest = this.datastore.get(vehicleManifestKey);
       vehicleManifest.setProperty("json", new Text(BackendConfigGsonProvider.get().toJson(m)));
       this.datastore.put(vehicleManifest);
    } catch (EntityNotFoundException e) {
       logger.log(Level.WARNING, String.format("Failed to update manifest for %s", vehicleId));
    }
  }

  public synchronized BackendConfig.Manifest getManifest(String vehicleId) {
    logger.log(Level.INFO, String.format("getting manifest for %s", vehicleId));
    Key vehicleManifestKey = KeyFactory.createKey("VehicleManifest", vehicleId);
    try {
       Entity vehicleManifest = this.datastore.get(vehicleManifestKey);
       if (vehicleManifest == null) {
         logger.log(Level.INFO, String.format("no manifest found for %s", vehicleId));
         return null;
       }
       Text manifestJSON = (Text)vehicleManifest.getProperty("json");
       return BackendConfigGsonProvider.get().fromJson(manifestJSON.getValue(), BackendConfig.Manifest.class);
    } catch (EntityNotFoundException e) {
       return null;
    }
  }

  public synchronized String getManifestDS(String vehicleId) {
    logger.log(Level.INFO, String.format("getting manifest for %s", vehicleId));
    Key vehicleManifestKey = KeyFactory.createKey("VehicleManifest", vehicleId);
    try {
       Entity vehicleManifest = this.datastore.get(vehicleManifestKey);
       Text manifestJSON = (Text)vehicleManifest.getProperty("json");
       return manifestJSON.getValue();
    } catch (EntityNotFoundException e) {
       return null;
    }
  }

  public synchronized BackendConfig.Task getBackendConfigTask(String taskId) {
    //TODO query database
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

  public synchronized void removeBackendConfigTask(String vehicleId, String taskId) {
   logger.log(Level.INFO, String.format("Removing task %s from vehicle for %s", taskId, vehicleId));
   BackendConfig.Manifest manifest = this.getManifest(vehicleId);
   for (BackendConfig.Stop stop : manifest.stops) {
     ArrayList<String> tasksList = new ArrayList<>(Arrays.asList(stop.tasks));
     tasksList.remove(taskId);
     stop.tasks = tasksList.toArray(new String[tasksList.size()]);
   }
   this.updateManifest(manifest);
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
