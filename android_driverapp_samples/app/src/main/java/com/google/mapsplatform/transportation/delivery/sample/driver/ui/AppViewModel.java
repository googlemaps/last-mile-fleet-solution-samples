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

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.Task;
import com.google.common.collect.ImmutableList;
import com.google.mapsplatform.transportation.delivery.sample.driver.domain.vehicle.AppVehicleStop;

/** View model for the app. */
public class AppViewModel extends ViewModel {

  /** Padding for the map. */
  public static final int MAP_PADDING = 100;

  /** Current list of vehicle stops. */
  private MutableLiveData<ImmutableList<AppVehicleStop>> vehicleStops = new MutableLiveData<>();

  /** Selected tasks to show details for. */
  private MutableLiveData<ImmutableList<Task>> selectedTasks = new MutableLiveData<>();

  /** Selected vehicle stop to show details for. */
  private MutableLiveData<AppVehicleStop> selectedVehicleStop = new MutableLiveData<>();

  public AppViewModel() {
    vehicleStops.setValue(ImmutableList.of());
  }

  public LiveData<ImmutableList<AppVehicleStop>> getVehicleStops() {
    return vehicleStops;
  }

  public LiveData<ImmutableList<Task>> getSelectedTasks() {
    return selectedTasks;
  }

  public LiveData<AppVehicleStop> getSelectedVehicleStop() {
    return selectedVehicleStop;
  }

  public void setVehicleStops(ImmutableList<AppVehicleStop> vehicleStops) {
    this.vehicleStops.setValue(vehicleStops);
  }

  public void setSelectedTasks(ImmutableList<Task> tasks) {
    this.selectedTasks.setValue(tasks);
  }

  public void setSelectedVehicleStop(AppVehicleStop vehicleStop) {
    this.selectedVehicleStop.setValue(vehicleStop);
  }
}
