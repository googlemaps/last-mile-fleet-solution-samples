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
package com.google.mapsplatform.transportation.delivery.sample.driver.adapter;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.Task;
import com.google.common.collect.ImmutableList;
import com.google.mapsplatform.transportation.delivery.sample.driver.ItineraryButtonClickListener;
import com.google.mapsplatform.transportation.delivery.sample.driver.R;
import com.google.mapsplatform.transportation.delivery.sample.driver.delivery.DeliveryManager;
import com.google.mapsplatform.transportation.delivery.sample.driver.domain.task.DeliveryTaskConfig;
import com.google.mapsplatform.transportation.delivery.sample.driver.utils.AddressUtils;
import java.util.List;

/**
 * Adapter for the delivery details recycler view.
 */
public class DeliveryDetailsListAdapter extends
    RecyclerView.Adapter<DeliveryDetailsListAdapter.DeliveryDetailsListItemViewHolder> {

  private final List<Task> tasks;
  private final boolean showAddress;
  private ItineraryButtonClickListener buttonClickListener;
  private DeliveryManager manager;

  public DeliveryDetailsListAdapter(List<Task> tasks, boolean showAddress,
      ItineraryButtonClickListener buttonClickListener) {
    this.tasks = tasks;
    this.buttonClickListener = buttonClickListener;
    this.showAddress = showAddress;
    this.manager = DeliveryManager.getInstance();
  }

  @Override
  public DeliveryDetailsListItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext()).inflate(
        R.layout.delivery_details_list_item, null);

    return new DeliveryDetailsListItemViewHolder(view, manager);
  }

  @Override
  public void onBindViewHolder(DeliveryDetailsListItemViewHolder holder, int position) {
    holder.configure(tasks.get(position), position, showAddress, buttonClickListener);
  }

  @Override
  public int getItemCount() {
    return tasks.size();
  }

  /**
   * View holder for a delivery details list item.
   */
  protected static class DeliveryDetailsListItemViewHolder extends RecyclerView.ViewHolder {

    private final TextView labelTextView;
    private final TextView titleTextView;
    private final TextView detailTextView;
    private final TextView idTextView;
    private final TextView idValueTextView;
    private final Button markSuccessfulButton;
    private final Button markUnsuccessfulButton;
    private final Button detailsButton;

    private DeliveryManager manager;
    private Task task;
    private int position;
    private boolean showAddress;
    private ItineraryButtonClickListener buttonClickListener;

    public DeliveryDetailsListItemViewHolder(View v, DeliveryManager manager) {
      super(v);

      labelTextView = v.findViewById(R.id.label_text_view);
      titleTextView = v.findViewById(R.id.title_text_view);
      detailTextView = v.findViewById(R.id.detail_text_view);
      idTextView = v.findViewById(R.id.id_text_view);
      idValueTextView = v.findViewById(R.id.id_value_text_view);
      markSuccessfulButton = v.findViewById(R.id.mark_successful_button);
      markUnsuccessfulButton = v.findViewById(R.id.mark_unsuccessful_button);
      detailsButton = v.findViewById(R.id.details_button);

      this.manager = manager;
    }

    public void configure(Task task, int position, boolean showAddress,
        ItineraryButtonClickListener buttonClickListener) {
      this.task = task;
      this.position = position;
      this.showAddress = showAddress;
      this.buttonClickListener = buttonClickListener;

      configureLabelTextView();
      configureTitleTextView();
      configureIdTextViews();
      configureDetailTextView();
      configureMarkButtons();
      configureDetailsButton();
    }

    private void configureMarkButtons() {
      markSuccessfulButton.setVisibility(isTaskDeliveryOrPickup() ? View.VISIBLE : View.GONE);
      markUnsuccessfulButton.setVisibility(isTaskDeliveryOrPickup()
          ? View.VISIBLE
          : View.GONE);

      if (isTaskDeliveryOrPickup()) {
        markSuccessfulButton.setText(task.getTaskType() == Task.TaskType.DELIVERY_PICKUP
            ? R.string.picked_up : R.string.delivered);
        markUnsuccessfulButton.setText(task.getTaskType() == Task.TaskType.DELIVERY_PICKUP
            ? R.string.could_not_pick_up : R.string.could_not_deliver);

        markSuccessfulButton.setOnClickListener(v -> {
          if (buttonClickListener != null) {
            buttonClickListener.onMarkButtonClick(ImmutableList.of(task), true);
          }
        });

        markUnsuccessfulButton.setOnClickListener(v -> {
          if (buttonClickListener != null) {
            buttonClickListener.onMarkButtonClick(ImmutableList.of(task), false);
          }
        });
      }
    }

    private boolean isTaskDeliveryOrPickup() {
      return task.getTaskType() == Task.TaskType.DELIVERY_DELIVERY
          || task.getTaskType() == Task.TaskType.DELIVERY_PICKUP;
    }

    private void configureLabelTextView() {
      if (!showAddress) {
        labelTextView.setVisibility(View.GONE);
        return;
      }

      switch (task.getTaskType()) {
        case Task.TaskType.DELIVERY_DELIVERY:
          labelTextView.setBackgroundResource(R.drawable.shape_circle_red);
          labelTextView.setTextColor(Color.WHITE);
          break;

        case Task.TaskType.DELIVERY_PICKUP:
          labelTextView.setBackgroundResource(R.drawable.shape_circle_light_red);
          labelTextView
              .setTextColor(labelTextView.getContext().getResources().getColor(R.color.darkRed));
          break;

        default:
          labelTextView.setBackgroundResource(R.drawable.shape_circle_grey);
          labelTextView.setTextColor(Color.WHITE);
          break;
      }
      labelTextView.setVisibility(View.VISIBLE);
      labelTextView.setText(String.format("%c", 'A' + position));
    }

    private void configureDetailTextView() {
      if (showAddress) {
        String address = task.getPlannedWaypoint().getTitle();
        if (address == null) {
          address = AddressUtils.longAddress(
              task.getPlannedWaypoint().getPosition(), detailTextView.getContext());
        }
        detailTextView.setText(address);
      } else {
        detailTextView.setVisibility(isTaskDeliveryOrPickup() ? View.VISIBLE : View.GONE);
        if (isTaskDeliveryOrPickup()) {
          detailTextView.setText(task.getTaskType() == Task.TaskType.DELIVERY_PICKUP
              ? R.string.pick_up : R.string.delivery);
          detailTextView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        }
      }
    }

    private void configureTitleTextView() {
      DeliveryTaskConfig taskConfig = manager.getTaskConfig(task.getTaskId());
      if (taskConfig != null && taskConfig.contactName() != null) {
        titleTextView.setText(taskConfig.contactName());
        titleTextView
            .setVisibility((!showAddress && position > 0) ? View.GONE : View.VISIBLE);
      } else {
        titleTextView.setVisibility(View.GONE);
      }
    }

    private void configureIdTextViews() {
      idTextView.setTypeface(showAddress ? Typeface.defaultFromStyle(Typeface.BOLD) :
          Typeface.defaultFromStyle(Typeface.NORMAL));
      idValueTextView.setText(task.getTaskId());
    }

    private void configureDetailsButton() {
      if (showAddress) {
        detailsButton.setVisibility(View.VISIBLE);
        detailsButton.setOnClickListener(v -> {
          if (buttonClickListener != null) {
            buttonClickListener.onDetailsButtonClick(ImmutableList.of(this.task), null);
          }
        });
      } else {
        detailsButton.setVisibility(View.GONE);
      }
    }
  }
}
