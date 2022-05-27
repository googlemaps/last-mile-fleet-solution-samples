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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraFollowLocationCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.Task;
import com.google.android.libraries.navigation.SupportNavigationFragment;
import com.google.common.collect.ImmutableList;
import com.google.mapsplatform.transportation.delivery.sample.driver.ItineraryButtonClickListener;
import com.google.mapsplatform.transportation.delivery.sample.driver.MainActivity;
import com.google.mapsplatform.transportation.delivery.sample.driver.R;
import com.google.mapsplatform.transportation.delivery.sample.driver.adapter.DeliveryDetailsListAdapter;
import com.google.mapsplatform.transportation.delivery.sample.driver.domain.vehicle.AppVehicleStop;
import com.google.mapsplatform.transportation.delivery.sample.driver.utils.AddressUtils;
import com.google.mapsplatform.transportation.delivery.sample.driver.utils.SpaceItemDecoration;
import com.google.mapsplatform.transportation.delivery.sample.driver.utils.UiUtils;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fragment that shows the delivery details for a vehicle stop or a list of tasks.
 */
public class DeliveryDetailsFragment extends Fragment {

  private ImageButton backButton;
  private TextView addressTextView;
  private RecyclerView recyclerView;
  private SupportNavigationFragment navigationFragment;
  private GoogleMap googleMap;
  private LatLngBounds allMarkersBounds;

  private List<Task> tasks;
  private static final int MAP_ZOOM_LEVEL = 17;
  private AppViewModel viewModel;

  private static final Logger logger = Logger.getLogger(DeliveryDetailsFragment.class.getName());

  // Bundle key used to determine if the details for a
  // vehicle stop or a list of tasks should be shown.
  public static final String BUNDLE_KEY_SHOW_VEHICLE_STOP_DETAILS = "showVehicleStopDetails";

  // Determines if the details for a vehicle stop or a list of tasks should be shown.
  private boolean showVehicleStopDetails;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_delivery_details, container, false);

    viewModel = new ViewModelProvider(getActivity()).get(AppViewModel.class);
    showVehicleStopDetails = getArguments().getBoolean(BUNDLE_KEY_SHOW_VEHICLE_STOP_DETAILS);

    navigationFragment = (SupportNavigationFragment) getChildFragmentManager()
        .findFragmentById(R.id.navigation_fragment);

    LatLng addressLatLng = null;
    if (showVehicleStopDetails) {
      AppVehicleStop vehicleStop = viewModel.getSelectedVehicleStop().getValue();
      tasks = vehicleStop.getTasks();
      addressLatLng = vehicleStop.getWaypoint().getPosition();
    } else {
      tasks = viewModel.getSelectedTasks().getValue();
      if (tasks != null && tasks.size() > 0) {
        addressLatLng = tasks.get(0).getPlannedWaypoint().getPosition();
      }
    }

    addressTextView = view.findViewById(R.id.address_text_view);
    addressTextView.setText(AddressUtils.shortAddress(addressLatLng, getContext()));

    backButton = view.findViewById(R.id.back_button);
    backButton.setOnClickListener(v -> getActivity().onBackPressed());

    recyclerView = view.findViewById(R.id.recycler_view);
    configureRecyclerView();

    return view;
  }

  @Override
  public void onStart() {
    super.onStart();
    navigationFragment.getMapAsync(this::setGoogleMap);
  }

  @Override
  public void onStop() {
    super.onStop();
    if (googleMap != null) {
      googleMap.setOnFollowMyLocationCallback(null);
    }
  }

  private void configureRecyclerView() {
    // There is no Nav button in DeliveryDetailsFragment; onNavigateButtonClick isn't used.
    ItineraryButtonClickListener itineraryButtonClickListener = new ItineraryButtonClickListener() {
      @Override
      public void onNavigateButtonClick(AppVehicleStop vehicleStop) {
      }

      @Override
      public void onDetailsButtonClick(List<Task> tasks, AppVehicleStop vehicleStop) {
        // When the details button is clicked from DeliveryDetailsFragment, it will always be used
        // to go to the task details page of a single task, so vehicleStop should be null.
        Bundle bundle = new Bundle();
        bundle.putBoolean(DeliveryDetailsFragment.BUNDLE_KEY_SHOW_VEHICLE_STOP_DETAILS, false);
        viewModel.setSelectedTasks(ImmutableList.copyOf(tasks));
        Navigation.findNavController(getActivity(), R.id.nav_host_fragment_main)
            .navigate(R.id.details_fragment, bundle);
      }

      @Override
      public void onMarkButtonClick(List<Task> tasks, boolean success) {
        logger.log(Level.INFO, "onMarkButtonClick triggered from DeliveryDetailsFragment");
        ((MainActivity) getActivity()).markTasks(tasks, success);
      }
    };

    DeliveryDetailsListAdapter adapter = new DeliveryDetailsListAdapter(tasks,
        showVehicleStopDetails, itineraryButtonClickListener);
    recyclerView.setAdapter(adapter);
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext(),
        LinearLayoutManager.VERTICAL, false));

    recyclerView.addItemDecoration(new SpaceItemDecoration(getContext(), false, 24, 0));
  }

  private void setGoogleMap(GoogleMap googleMap) {
    this.googleMap = googleMap;
    if (UiUtils.isNightModeEnabled(getContext())) {
      googleMap
          .setMapStyle(MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.dark_map_style));
    } else {
      googleMap.setMapStyle(null);
    }

    googleMap.setOnFollowMyLocationCallback(new OnCameraFollowLocationCallback() {
      @Override
      public void onCameraStartedFollowingLocation() {
        if (allMarkersBounds != null) {
          googleMap.moveCamera(
              CameraUpdateFactory.newLatLngBounds(allMarkersBounds, AppViewModel.MAP_PADDING));
        } else {
          addTaskLocationsToMap();
        }
      }

      @Override
      public void onCameraStoppedFollowingLocation() {
      }
    });
    addTaskLocationsToMap();
  }

  private void addTaskLocationsToMap() {
    LatLngBounds.Builder boundsBuilder = LatLngBounds.builder();

    for (int i = 0; i < tasks.size(); i++) {
      Task task = tasks.get(i);
      LatLng position = task.getPlannedWaypoint().getPosition();
      boundsBuilder.include(position);

      MarkerOptions markerOptions = new MarkerOptions().position(position);
      if (showVehicleStopDetails) {
        markerOptions.title(String.format("%c", 'A' + i));
      }
      googleMap.addMarker(markerOptions);
    }

    allMarkersBounds = boundsBuilder.build();
    googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(allMarkersBounds,
        AppViewModel.MAP_PADDING));
    googleMap.moveCamera(CameraUpdateFactory.zoomTo(MAP_ZOOM_LEVEL));
  }
}
