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
package com.google.mapsplatform.transportation.delivery.sample.driver.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.DriverContext;
import com.google.android.libraries.mapsplatform.transportation.driver.api.delivery.DeliveryDriverApi;
import com.google.android.libraries.mapsplatform.transportation.driver.api.delivery.vehiclereporter.DeliveryVehicleReporter;
import com.google.android.libraries.navigation.ListenableResultFuture;
import com.google.android.libraries.navigation.NavigationApi;
import com.google.android.libraries.navigation.Navigator;
import com.google.android.libraries.navigation.Navigator.RouteStatus;
import com.google.android.libraries.navigation.SimulationOptions;
import com.google.android.libraries.navigation.SupportNavigationFragment;
import com.google.android.libraries.navigation.Waypoint;
import com.google.android.material.snackbar.Snackbar;
import com.google.mapsplatform.transportation.delivery.sample.driver.R;
import com.google.mapsplatform.transportation.delivery.sample.driver.auth.DeliveryDriverTokenFactory;
import com.google.mapsplatform.transportation.delivery.sample.driver.delivery.DeliveryManager;
import java.util.Map;

public class NavigationActivity extends AppCompatActivity {

  private static final String TAG = NavigationActivity.class.getName();
  private SupportNavigationFragment navigationFragment;
  private TextView addressTextView;
  private TextView detailTextView;
  private ImageButton exitButton;

  // Extra fields to be passed in via the intent.
  public static final String EXTRA_END_LOCATION = "end_location";
  public static final String EXTRA_END_LOCATION_ADDRESS = "end_location_address";
  public static final String EXTRA_START_LOCATION = "start_location";
  public static final String EXTRA_DETAIL = "detail";
  public static final String EXTRA_PROVIDER_ID = "provider_id";
  public static final String EXTRA_VEHICLE_ID = "vehicle_id";
  public static final String EXTRA_LOCATION_TRACKING = "location_tracking";
  public static final String EXTRA_SIMULATION = "simulation";

  private final DeliveryManager manager = DeliveryManager.getInstance();

  private Navigator navigator;
  private GoogleMap googleMap;
  private LatLng location;
  private String endLocationAddress;
  private LatLng startLocation;
  private ActivityResultLauncher<String[]> locationLauncher;
  private boolean simulation;
  private boolean locationTracking;

  private boolean arrived;

  private DeliveryVehicleReporter vehicleReporter;
  private Navigator.ArrivalListener arrivalListener;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_navigation);
    if (getSupportActionBar() != null) {
      getSupportActionBar().hide();
    }

    arrived = false;

    addressTextView = findViewById(R.id.address_text_view);
    detailTextView = findViewById(R.id.detail_text_view);
    exitButton = findViewById(R.id.exit_button);

    navigationFragment =
        (SupportNavigationFragment)
            getSupportFragmentManager().findFragmentById(R.id.navigation_fragment);
    navigationFragment.getMapAsync(googleMap -> NavigationActivity.this.googleMap = googleMap);

    location = getIntent().getParcelableExtra(EXTRA_END_LOCATION);
    endLocationAddress = getIntent().getStringExtra(EXTRA_END_LOCATION_ADDRESS);
    startLocation = getIntent().getParcelableExtra(EXTRA_START_LOCATION);
    addressTextView.setText(endLocationAddress);
    String details = getIntent().getStringExtra(EXTRA_DETAIL);
    simulation = getIntent().getBooleanExtra(EXTRA_SIMULATION, true);
    locationTracking = getIntent().getBooleanExtra(EXTRA_LOCATION_TRACKING, true);
    detailTextView.setText(details);
    exitButton.setOnClickListener(
        view -> {
          setResult(arrived ? Activity.RESULT_OK : Activity.RESULT_CANCELED);
          finish();
        });

    locationLauncher =
        registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            this::onLocationPermissionGrant);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    if (!permissionsAreGranted()) {
      requestLocationPermission();
    } else {
      initNavigator();
    }
  }

  private void setupDeliveryVehicleReporter(Navigator navigator) {
    // getInstance() should always be null because it is cleared in onDestroy(); check here just
    // to be sure.
    if (DeliveryDriverApi.getInstance() == null) {
      Application application = getApplication();
      DriverContext driverContext =
          DriverContext.builder(application)
              .setProviderId(getIntent().getStringExtra(EXTRA_PROVIDER_ID))
              .setVehicleId(getIntent().getStringExtra(EXTRA_VEHICLE_ID))
              .setAuthTokenFactory(DeliveryDriverTokenFactory.getInstance(manager.getBackend()))
              .setNavigator(navigator)
              .setRoadSnappedLocationProvider(
                  NavigationApi.getRoadSnappedLocationProvider(application))
              .setStatusListener(
                  (statusLevel, statusCode, statusMsg) -> {
                    Log.d(
                        TAG,
                        String.format(
                            "VehicleReporter: %s | %s | %s",
                            statusLevel.name(), statusCode.name(), statusMsg));
                  })
              .build();
      DeliveryDriverApi.createInstance(driverContext);
    }
    vehicleReporter = DeliveryDriverApi.getInstance().getDeliveryVehicleReporter();
    if (locationTracking) {
      vehicleReporter.enableLocationTracking();
      Log.i(TAG, "Started location tracking");
    } else {
      Log.i(TAG, "Location tracking was turned off in settings");
    }
  }

  /**
   * Validates if the permissions are granted after user took action. In case permissions are
   * granted, continue with initialization flow. Otherwise, display an informative dialogue and
   * request permission again.
   */
  private void onLocationPermissionGrant(Map<String, Boolean> result) {
    if (!permissionsAreGranted()) {
      new AlertDialog.Builder(this)
          .setTitle(R.string.permission_warning_title)
          .setMessage(R.string.permission_warning_message)
          .setNeutralButton(
              R.string.permission_warning_button,
              (dialogInterface, n) -> {
                requestLocationPermission();
              })
          .create()
          .show();
    } else {
      initNavigator();
    }
  }

  /** Displays the location permission request dialog. */
  private void requestLocationPermission() {
    locationLauncher.launch(new String[] {Manifest.permission.ACCESS_FINE_LOCATION});
  }

  /** Verifies if location permissions are granted to the app. */
  private boolean permissionsAreGranted() {
    int permission =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
    return permission == PackageManager.PERMISSION_GRANTED;
  }

  private void initNavigator() {
    NavigationApi.getNavigator(
        this,
        new NavigationApi.NavigatorListener() {
          @Override
          public void onNavigatorReady(Navigator navigator) {
            NavigationActivity.this.navigator = navigator;
            setupDeliveryVehicleReporter(navigator);

            ListenableResultFuture<Navigator.RouteStatus> pendingRoute =
                navigator.setDestination(
                    Waypoint.builder()
                        .setLatLng(location.latitude, location.longitude)
                        .setTitle(endLocationAddress)
                        .build());
            pendingRoute.setOnResultListener(
                routeStatus -> {
                  if (routeStatus != RouteStatus.OK) {
                    Log.e(TAG, String.format("RouteStatus is %s, not starting nav.", routeStatus));
                    return;
                  }
                  if (simulation) {
                    if (startLocation != null) {
                      Log.i(
                          TAG,
                          String.format(
                              "Nav start location is %f, %f",
                              startLocation.latitude, startLocation.longitude));
                      navigator.getSimulator().setUserLocation(startLocation);
                    }
                    navigator
                        .getSimulator()
                        .simulateLocationsAlongExistingRoute(
                            new SimulationOptions().speedMultiplier(1));
                  }
                  navigator.startGuidance();
                });

            arrivalListener = arrivalEvent -> onArrive();
            navigator.addArrivalListener(arrivalListener);
          }

          @Override
          public void onError(int errorCode) {
            Log.e(TAG, String.format("Error loading Navigator instance: %s", errorCode));
          }
        });
  }

  private void onArrive() {
    Snackbar.make(
            findViewById(android.R.id.content),
            getResources().getString(R.string.arrival_message),
            Snackbar.LENGTH_LONG)
        .show();
    arrived = true;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    googleMap.clear();
    vehicleReporter.disableLocationTracking();
    DeliveryDriverApi.clearInstance();
    navigator.stopGuidance();
    navigator.getSimulator().unsetUserLocation();
    navigator.clearDestinations();
    navigator.removeArrivalListener(arrivalListener);
    navigator.cleanup();
  }
}
