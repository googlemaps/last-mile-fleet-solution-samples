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
package com.google.mapsplatform.transportation.delivery.sample.driver.ui.itinerary;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraFollowLocationCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.Task;
import com.google.android.libraries.navigation.SupportNavigationFragment;
import com.google.android.libraries.navigation.Waypoint;
import com.google.common.collect.ImmutableList;
import com.google.mapsplatform.transportation.delivery.sample.driver.ItineraryButtonClickListener;
import com.google.mapsplatform.transportation.delivery.sample.driver.MainActivity;
import com.google.mapsplatform.transportation.delivery.sample.driver.R;
import com.google.mapsplatform.transportation.delivery.sample.driver.adapter.ItineraryMapListAdapter;
import com.google.mapsplatform.transportation.delivery.sample.driver.domain.vehicle.AppVehicleStop;
import com.google.mapsplatform.transportation.delivery.sample.driver.ui.AppViewModel;
import com.google.mapsplatform.transportation.delivery.sample.driver.ui.DeliveryDetailsFragment;
import com.google.mapsplatform.transportation.delivery.sample.driver.utils.ItineraryListUtils;
import com.google.mapsplatform.transportation.delivery.sample.driver.utils.SpaceItemDecoration;
import com.google.mapsplatform.transportation.delivery.sample.driver.utils.UiUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fragments that displays the vehicle stops on a map.
 */
public class ItineraryMapFragment extends Fragment {

  private RecyclerView recyclerView;

  /**
   * Used to get access to the Google Map.
   */
  private SupportNavigationFragment navigationFragment;

  /**
   * Used to display the location of the vehicle stops.
   */
  private GoogleMap googleMap;

  private List<Marker> markers = new ArrayList<>();
  private List<AppVehicleStop> vehicleStops;
  private AppViewModel viewModel;
  private ItineraryMapListAdapter itineraryMapListAdapter;
  private LatLngBounds allMarkersBounds;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_itinerary_map, container, false);

    MainActivity activity = (MainActivity) getActivity();
    viewModel = new ViewModelProvider(activity).get(AppViewModel.class);
    recyclerView = view.findViewById(R.id.recycler_view);

    this.vehicleStops = ImmutableList.of();

    navigationFragment = (SupportNavigationFragment)
        getChildFragmentManager().findFragmentById(R.id.navigation_fragment);
    updateRecyclerViewAdapter();
    configureRecyclerView();

    viewModel.getVehicleStops().observe(this, stops -> {
      this.vehicleStops = stops;
      itineraryMapListAdapter.setVehicleStops(stops);
      if (googleMap != null) {
        addVehicleStopsToMap();
      }
    });

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
      googleMap.setOnMarkerClickListener(null);
      googleMap.setOnFollowMyLocationCallback(null);
    }
  }

  private void setGoogleMap(GoogleMap googleMap) {
    this.googleMap = googleMap;
    if (UiUtils.isNightModeEnabled(getContext())) {
      googleMap
          .setMapStyle(MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.dark_map_style));
    } else {
      googleMap.setMapStyle(null);
    }

    googleMap.setOnMarkerClickListener(marker -> {
      int markerPosition = markers.indexOf(marker);
      if (markerPosition > -1) {
        recyclerView.scrollToPosition(markerPosition);
      }
      return false;
    });

    googleMap.setOnFollowMyLocationCallback(new OnCameraFollowLocationCallback() {
      @Override
      public void onCameraStartedFollowingLocation() {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "started following");
        // Forcibly stop following my location.
        if (allMarkersBounds != null) {
          googleMap.moveCamera(
              CameraUpdateFactory.newLatLngBounds(allMarkersBounds, AppViewModel.MAP_PADDING));
        } else {
          addVehicleStopsToMap();
        }
      }

      @Override
      public void onCameraStoppedFollowingLocation() {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "ended following");
      }
    });
    addVehicleStopsToMap();
  }

  private void addVehicleStopsToMap() {

    // Clean up the map.
    for (Marker marker : markers) {
      marker.remove();
    }
    markers.clear();

    if (vehicleStops.isEmpty()) {
      return;
    }

    LatLngBounds.Builder boundsBuilder = LatLngBounds.builder();

    // Set the options for painting the text in the pins.
    Paint paint = new Paint();
    paint.setTextSize(
        getContext().getResources().getDimensionPixelSize(R.dimen.map_view_pin_text_size));
    paint.setSubpixelText(true);
    paint.setHinting(Paint.HINTING_ON);
    paint.setTypeface(Typeface.DEFAULT_BOLD);
    paint.setColor(getContext().getResources().getColor(R.color.lightBlue, null));
    paint.setTextAlign(Align.CENTER);

    for (int i = 0; i < vehicleStops.size(); i++) {
      AppVehicleStop vehicleStop = vehicleStops.get(i);
      Waypoint waypoint = vehicleStop.getWaypoint();
      LatLng position = waypoint.getPosition();
      boundsBuilder.include(position);

      Bitmap pinBitmap =
          BitmapFactory.decodeResource(getResources(), R.drawable.deep_blue_pin)
              .copy(Config.ARGB_8888, true);
      Canvas canvas = new Canvas(pinBitmap);
      canvas.drawText(
          ItineraryListUtils.getPositionText(i),
          (float) (canvas.getWidth() / 2),
          (float) (canvas.getHeight() / 2.4),
          paint);
      MarkerOptions markerOptions =
          new MarkerOptions()
              .icon(BitmapDescriptorFactory.fromBitmap(pinBitmap))
              .alpha(1)
              .position(position);
      Marker marker = googleMap.addMarker(markerOptions);
      markers.add(marker);
    }

    allMarkersBounds = boundsBuilder.build();
    googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(allMarkersBounds,
        AppViewModel.MAP_PADDING));
  }

  private void configureRecyclerView() {
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext(),
        LinearLayoutManager.HORIZONTAL, false));
    SpaceItemDecoration itemDecoration = new SpaceItemDecoration(getContext(), true, 8, 14);
    recyclerView.addItemDecoration(itemDecoration);
  }

  private void updateRecyclerViewAdapter() {

    ItineraryButtonClickListener itineraryButtonClickListener = new ItineraryButtonClickListener() {
      @Override
      public void onNavigateButtonClick(AppVehicleStop vehicleStop) {
        ((MainActivity) getActivity()).launchNavigationActivity(vehicleStop);
      }

      @Override
      public void onDetailsButtonClick(List<Task> tasks, AppVehicleStop vehicleStop) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(DeliveryDetailsFragment.BUNDLE_KEY_SHOW_VEHICLE_STOP_DETAILS, true);
        viewModel.setSelectedVehicleStop(vehicleStop);
        Navigation.findNavController(getActivity(), R.id.nav_host_fragment_main)
            .navigate(R.id.details_fragment, bundle);
      }

      @Override
      public void onMarkButtonClick(List<Task> tasks, boolean successful) {
        // There is no mark button for the map screen.
      }
    };

    itineraryMapListAdapter = new ItineraryMapListAdapter(vehicleStops,
        itineraryButtonClickListener);
    recyclerView.setAdapter(itineraryMapListAdapter);
  }
}
