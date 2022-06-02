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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.Task;
import com.google.common.collect.ImmutableList;
import com.google.mapsplatform.transportation.delivery.sample.driver.ItineraryButtonClickListener;
import com.google.mapsplatform.transportation.delivery.sample.driver.MainActivity;
import com.google.mapsplatform.transportation.delivery.sample.driver.R;
import com.google.mapsplatform.transportation.delivery.sample.driver.adapter.ItineraryListAdapter;
import com.google.mapsplatform.transportation.delivery.sample.driver.domain.vehicle.AppVehicleStop;
import com.google.mapsplatform.transportation.delivery.sample.driver.ui.AppViewModel;
import com.google.mapsplatform.transportation.delivery.sample.driver.ui.DeliveryDetailsFragment;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Fragment that displays the vehicle stops in a list. */
public class ItineraryListFragment extends Fragment {

  private RecyclerView recyclerView;
  private List<AppVehicleStop> vehicleStops;
  private AppViewModel viewModel;
  private final Timer timer = new Timer();
  private static final long REORDER_STOPS_TIMEOUT_MS = 2000;
  private TimerTask reorderStopsTimerTask;
  private final Logger logger = Logger.getLogger(ItineraryListFragment.class.getName());
  private ItineraryListAdapter itineraryListAdapter;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_itinerary_list, container, false);

    MainActivity activity = (MainActivity) getActivity();
    viewModel = new ViewModelProvider(activity).get(AppViewModel.class);
    recyclerView = view.findViewById(R.id.recycler_view);
    viewModel.getVehicleStops().observe(this, stops -> {
      this.vehicleStops = stops;
      itineraryListAdapter.setVehicleStops(stops);
    });

    vehicleStops = viewModel.getVehicleStops().getValue();
    updateRecyclerViewAdapter();
    configureRecyclerView();
    return view;
  }

  private void configureRecyclerView() {
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext(),
        LinearLayoutManager.VERTICAL, false));
    recyclerView.addItemDecoration(
        new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));
  }

  private void updateRecyclerViewAdapter() {

    ItineraryListAdapter.VehicleStopsReorderListener reorderListener =
        this::notifyProviderOfVehicleStopsNewOrder;

    ItineraryButtonClickListener itineraryButtonClickListener = new ItineraryButtonClickListener() {
      @Override
      public void onNavigateButtonClick(AppVehicleStop vehicleStop) {
        ((MainActivity) getActivity()).launchNavigationActivity(vehicleStop);
      }

      @Override
      public void onDetailsButtonClick(List<Task> tasks, AppVehicleStop vehicleStop) {
        Bundle bundle = new Bundle();
        if (tasks != null) {
          bundle.putBoolean(DeliveryDetailsFragment.BUNDLE_KEY_SHOW_VEHICLE_STOP_DETAILS, false);
          viewModel.setSelectedTasks(ImmutableList.copyOf(tasks));
        } else {
          bundle.putBoolean(DeliveryDetailsFragment.BUNDLE_KEY_SHOW_VEHICLE_STOP_DETAILS, true);
          viewModel.setSelectedVehicleStop(vehicleStop);
        }
        Navigation.findNavController(getActivity(), R.id.nav_host_fragment_main)
            .navigate(R.id.details_fragment, bundle);
      }

      @Override
      public void onMarkButtonClick(List<Task> tasks, boolean successful) {
        logger.log(Level.INFO, "onMarkButtonClick called from ItineraryListFragment");
        ((MainActivity) getActivity()).markTasks(tasks, successful);
      }
    };

    itineraryListAdapter = new ItineraryListAdapter(vehicleStops, reorderListener,
        itineraryButtonClickListener);
    recyclerView.setAdapter(itineraryListAdapter);
  }

  private void notifyProviderOfVehicleStopsNewOrder(List<AppVehicleStop> stops) {
    logger.log(Level.INFO, "vehicle stops being reordered");
    if (reorderStopsTimerTask != null) {
      reorderStopsTimerTask.cancel();
    }
    reorderStopsTimerTask = new TimerTask() {
      @Override
      public void run() {
        getActivity().runOnUiThread(() -> {
          logger.log(Level.INFO, "Starting reorder stops handler");
          MainActivity activity = (MainActivity) getActivity();
          activity.showToast(getResources().getString(R.string.stop_list_reorder_sent));
          activity.getDeliveryManager().updateStopsList(stops, throwable -> {
            if (throwable != null) {
              logger.log(Level.SEVERE, throwable.getMessage());
              activity.showToast(getResources().getString(R.string.stop_list_reorder_failed));
            } else {
              activity.showToast(getResources().getString(R.string.stop_list_reorder_updated));
            }
          });
          ItineraryListFragment.this.reorderStopsTimerTask = null;
        });
      }
    };
    timer.schedule(reorderStopsTimerTask, REORDER_STOPS_TIMEOUT_MS);
  }
}
