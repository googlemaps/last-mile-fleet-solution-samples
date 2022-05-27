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
package com.google.mapsplatform.transportation.delivery.sample.driver.utils;

import android.content.Context;
import android.graphics.Color;
import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.Task;
import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.Task.TaskOutcome;
import com.google.mapsplatform.transportation.delivery.sample.driver.R;
import com.google.mapsplatform.transportation.delivery.sample.driver.domain.vehicle.AppVehicleStop;
import java.util.List;
import java.util.Optional;

/**
 * Performs common functions related to the itinerary list items such as getting the title text and
 * the color for the position view.
 */
public class ItineraryListUtils {

  /**
   * Returns the title text corresponding to the stop.
   */
  public static String getTitleText(AppVehicleStop vehicleStop) {
    return String.format("%s (%s)",
        vehicleStop.getWaypoint().getTitle(), vehicleStop.getTasks().size());
  }

  /**
   * Returns the positional text corresponding to the positional parameter.
   */
  public static String getPositionText(int position) {
    return String.format("%s", (position + 1));
  }

  /**
   * Returns the color used for the position text.
   */
  public static int getPositionTextColor() {
    return Color.WHITE;
  }

  /**
   * Returns the resource ID for the background for the position text.
   */
  public static int getPositionBackgroundResourceId() {
    return R.drawable.shape_circle_blue;
  }

  /**
   * Returns the string that lists the number of pick up and delivery tasks in a list of tasks.
   */
  public static String getTasksCountString(List<Task> tasks, Context context) {
    int pickupTasksCount = 0;
    int deliveryTasksCount = 0;
    for (Task task : tasks) {
      if (task.getTaskType() == Task.TaskType.DELIVERY_PICKUP) {
        pickupTasksCount++;
      } else if (task.getTaskType() == Task.TaskType.DELIVERY_DELIVERY) {
        deliveryTasksCount++;
      }
    }
    StringBuilder stringBuilder = new StringBuilder("");
    if (pickupTasksCount > 0) {
      stringBuilder.append(context.getResources()
          .getQuantityString(R.plurals.nav_bottom_pick_ups, pickupTasksCount, pickupTasksCount));
    }
    if (deliveryTasksCount > 0) {
      if (pickupTasksCount > 0) {
        stringBuilder.append(", ");
      }
      stringBuilder.append(context.getResources()
          .getQuantityString(R.plurals.nav_bottom_deliveries, deliveryTasksCount,
              deliveryTasksCount));
    }

    return stringBuilder.toString();
  }

  /**
   * Returns true if all tasks in the list have their outcomes set.
   */
  public static boolean allTasksHaveOutcome(List<Task> tasks) {
    return !(tasks.stream().anyMatch(task -> task.getTaskOutcome() == TaskOutcome.UNSPECIFIED));
  }

  /**
   * Returns the first stop where not all tasks have outcomes set. If no stop matches this
   * condition, returns Optional.empty.
   */
  public static Optional<AppVehicleStop> getFirstAvailableStop(List<AppVehicleStop> stops) {
    return stops.stream().filter(stop -> !allTasksHaveOutcome(stop.getTasks())).findFirst();
  }
}
