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
package com.google.mapsplatform.transportation.delivery.sample.driver;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonObject;
import com.google.mapsplatform.transportation.delivery.sample.driver.backend.DeliveryBackend;
import com.google.mapsplatform.transportation.delivery.sample.driver.backend.SampleBackend;
import com.google.mapsplatform.transportation.delivery.sample.driver.config.DeliveryConfig;
import com.google.mapsplatform.transportation.delivery.sample.driver.config.loader.DeliveryConfigLoader;
import com.google.mapsplatform.transportation.delivery.sample.driver.delivery.DeliveryManager;
import com.google.mapsplatform.transportation.delivery.sample.driver.domain.vehicle.AppVehicleStop;
import com.google.mapsplatform.transportation.delivery.sample.driver.domain.vehicle.DeliveryVehicleConfig;
import com.google.mapsplatform.transportation.delivery.sample.driver.settings.Preferences;
import com.google.mapsplatform.transportation.delivery.sample.driver.ui.AppViewModel;
import com.google.mapsplatform.transportation.delivery.sample.driver.ui.NavigationActivity;
import com.google.mapsplatform.transportation.delivery.sample.driver.utils.ItineraryListUtils;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/** Main activity class. */
public class MainActivity extends AppCompatActivity {

  /** Logger setup. */
  private static final String TAG = MainActivity.class.getName();

  private static final Logger logger = Logger.getLogger(TAG);

  /** Manager to handle delivery updates. */
  private final DeliveryManager deliveryManager = DeliveryManager.getInstance();
  /** The Delivery Config object loaded by the activity. */
  private final MutableLiveData<DeliveryConfig> jsonConfig = new MutableLiveData<>();
  /** Used to communicate with the backend. */
  private DeliveryBackend backend;
  /** Reference to the preferences object. */
  private Preferences preferences;

  private String vehicleId;
  private LatLng vehicleStartLocation = null;
  private String providerId;

  private boolean isFirstLocation = false;

  /** Activity launcher for the navigation activity. */
  private ActivityResultLauncher<Intent> navigationActivityLauncher = registerForActivityResult(
      new StartActivityForResult(),
      new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
          // Nav arrived at the destination. Notify delivery manager.
          if (result.getResultCode() == Activity.RESULT_OK) {
            deliveryManager.arrivedAtStop();
          } else {
            // Reset the state to NEW if the vehicle hasn't arrived at the stop.
            deliveryManager.notArrivingAtStop();
          }
        }
      }
  );

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    setupBottomNavigationView();
    verifyMapsApiKey();
    preferences = Preferences.getInstance(this);
    backend = new SampleBackend(this);
    jsonConfig.observe(this, this::loadConfiguration);

    // This fetches the manifest when the app starts. You may wish to instead
    // fetch a manifest with a button press, etc.
    getManifestData();
    deliveryManager.getStops().observe(
        this, new ViewModelProvider(this).get(AppViewModel.class)::setVehicleStops);
  }

  public DeliveryManager getDeliveryManager() {
    return deliveryManager;
  }

  private void verifyMapsApiKey() {
    String mapsApiKey;
    try {
      Bundle metadata =
          getPackageManager()
              .getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA)
              .metaData;
      mapsApiKey = metadata.getString("com.google.android.geo.API_KEY");
    } catch (PackageManager.NameNotFoundException e) {
      mapsApiKey = "";
    }

    if (!Pattern.matches("AIza[0-9A-Za-z-_]{35}", mapsApiKey)) {
      String invalidMapsApiKeyMessage =
          getResources().getString(R.string.invalid_maps_api_key, mapsApiKey);
      logger.log(Level.SEVERE, invalidMapsApiKeyMessage);
      showToast(invalidMapsApiKeyMessage);
    }
  }

  private void setupBottomNavigationView() {
    BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation_view);
    NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_main);
    NavigationUI.setupWithNavController(bottomNavigationView, navController);
  }

  /**
   * Starts the process to fetch the manifest data. This should be bound to an user interaction,
   * instead of executing on main UI creation.
   */
  private void getManifestData() {
    try {
      // Fetch the delivery config ("manifest") from the manifest endpoint.
      ListenableFuture<JsonObject> configJsonFuture = backend.getDeliveryConfigJson();
      DeliveryConfig config = DeliveryConfigLoader.fromJsonObject(configJsonFuture.get());
      jsonConfig.setValue(config);
      showToast(getResources().getString(R.string.backend_manifest_loaded));
    } catch (InterruptedException e) {
      logger.log(Level.SEVERE, "Thread was interrupted.", e);
    } catch (IOException e) {
      logger.log(Level.WARNING, "", e);
      showToast(e.toString());
    } catch (ExecutionException e) {
      logger.log(Level.SEVERE, "execution interrupted", e);
      if (e.getCause() instanceof java.io.FileNotFoundException) {
        showToast(getResources().getString(R.string.backend_manifest_404));
      } else {
        showToast(
            getResources().getString(R.string.backend_manifest_error, e.getCause().getMessage()));
      }
    }
  }

  /**
   * Converts the delivery config object into a configuration, and associates the manager with this
   * configuration.
   */
  private void loadConfiguration(DeliveryConfig config) {
    DeliveryVehicleConfig vehicleConfig = config.vehicle();

    this.vehicleId = vehicleConfig.vehicleId();
    if (vehicleConfig.startLocation() != null) {
      this.vehicleStartLocation = new LatLng(vehicleConfig.startLocation().latitude(),
          vehicleConfig.startLocation().longitude());
    }
    this.providerId = vehicleConfig.providerId();
    backend.setVehicleId(vehicleId);
    deliveryManager.setDeliveryBackend(backend, providerId);
    deliveryManager.setupUntrustedDelivery(config);
    isFirstLocation = true;
  }

  public void launchNavigationActivity(AppVehicleStop vehicleStop) {
    Intent intent = new Intent(this, NavigationActivity.class);
    intent.putExtra(NavigationActivity.EXTRA_END_LOCATION,
        vehicleStop.getWaypoint().getPosition());
    intent.putExtra(NavigationActivity.EXTRA_END_LOCATION_ADDRESS,
        vehicleStop.getWaypoint().getTitle());
    intent.putExtra(NavigationActivity.EXTRA_DETAIL,
        ItineraryListUtils.getTasksCountString(vehicleStop.getTasks(), this));
    intent.putExtra(NavigationActivity.EXTRA_PROVIDER_ID, providerId);
    intent.putExtra(NavigationActivity.EXTRA_VEHICLE_ID, vehicleId);
    intent.putExtra(NavigationActivity.EXTRA_SIMULATION, preferences.getSimulationSetting());
    intent.putExtra(
        NavigationActivity.EXTRA_LOCATION_TRACKING, preferences.getLocationTrackingSetting());
    if (isFirstLocation) {
      if (vehicleStartLocation != null) {
        intent.putExtra(NavigationActivity.EXTRA_START_LOCATION, vehicleStartLocation);
      }
      isFirstLocation = false;
    }
    navigationActivityLauncher.launch(intent);
    deliveryManager.enrouteToStop();
  }

  public void markTasks(List<Task> tasks, boolean successful) {
    deliveryManager.updateTaskOutcome(tasks, successful, failedTaskIds -> {
      // Just report the number of failed task outcomes for now.
      if (failedTaskIds.isEmpty()) {
        String msg = getResources().getString(R.string.task_outcomes_update_done);
        logger.log(Level.INFO, msg);
        showToast(msg);
      } else {
        String msg = getResources()
            .getQuantityString(R.plurals.task_outcome_update_failed, failedTaskIds.size(),
                failedTaskIds.size());
        logger.log(Level.SEVERE, msg);
        showToast(msg);
      }

      deliveryManager.refreshStopsList(throwable -> {
        if (throwable == null) {
          showToast(getResources().getString(R.string.stops_list_update_done));
        } else {
          logger.log(Level.SEVERE, throwable.getMessage());
          showToast(getResources().getString(R.string.stops_list_update_fail));
        }
      });
    });
    showToast(getResources()
        .getQuantityString(R.plurals.task_outcome_updates_sent, tasks.size(), tasks.size()));
  }

  /** Shows a toast with the given message. */
  public void showToast(String message) {
    Snackbar.make(findViewById(R.id.bottom_navigation_view), message, Snackbar.LENGTH_LONG).show();
  }
}
