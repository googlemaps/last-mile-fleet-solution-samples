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

import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.Task;
import com.google.mapsplatform.transportation.delivery.sample.driver.domain.vehicle.AppVehicleStop;
import java.util.List;

/** Listens to button click events from the itinerary list and map list items. */
public interface ItineraryButtonClickListener {
    void onNavigateButtonClick(AppVehicleStop vehicleStop);
    void onMarkButtonClick(List<Task> tasks, boolean success);
    void onDetailsButtonClick(List<Task> tasks, AppVehicleStop vehicleStop);
}
