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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.Task;
import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.Task.TaskOutcome;
import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.Task.TaskType;
import com.google.mapsplatform.transportation.delivery.sample.driver.ItineraryButtonClickListener;
import com.google.mapsplatform.transportation.delivery.sample.driver.R;
import com.google.mapsplatform.transportation.delivery.sample.driver.adapter.TaskInfoAdapter.TaskInfoListItemViewHolder;
import com.google.mapsplatform.transportation.delivery.sample.driver.delivery.DeliveryManager;
import com.google.mapsplatform.transportation.delivery.sample.driver.domain.task.DeliveryTaskConfig;
import java.util.Arrays;
import java.util.List;

/** Adapter for the task info recycler view. */
class TaskInfoAdapter extends RecyclerView.Adapter<TaskInfoListItemViewHolder> {
  private List<Task> tasks;
  private ItineraryButtonClickListener buttonClickListener;
  private DeliveryManager manager;

  public TaskInfoAdapter(List<Task> tasks, ItineraryButtonClickListener buttonClickListener) {
    this.tasks = tasks;
    this.buttonClickListener = buttonClickListener;

    // Fetch the global instance of delivery manager.
    // Be sure not to call any state-modifying methods here.
    this.manager = DeliveryManager.getInstance();
  }

  @Override
  public TaskInfoListItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View view =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.task_info_list_item, null);

    LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT,
        LayoutParams.WRAP_CONTENT);
    view.setLayoutParams(layoutParams);

    return new TaskInfoListItemViewHolder(view);
  }

  @Override
  public void onBindViewHolder(TaskInfoListItemViewHolder holder, int position) {
    holder.configure(tasks.get(position), buttonClickListener);
  }

  @Override
  public int getItemCount() {
    return tasks.size();
  }

  /** View holder for the task info recycler view row. */
  protected class TaskInfoListItemViewHolder extends RecyclerView.ViewHolder {
    private final TextView descriptionTextView;
    private final TextView addressTextView;
    private final TextView idsTextView;
    private final Button detailsButton;
    private final Button markButton;

    private Task task;
    private ItineraryButtonClickListener buttonClickListener;

    public TaskInfoListItemViewHolder(View v) {
      super(v);

      descriptionTextView = v.findViewById(R.id.description_text_view);
      addressTextView = v.findViewById(R.id.address_text_view);
      idsTextView = v.findViewById(R.id.ids_text_view);
      detailsButton = v.findViewById(R.id.details_button);
      markButton = v.findViewById(R.id.mark_button);
    }

    public void configure(Task task, ItineraryButtonClickListener buttonClickListener) {
      this.task = task;
      this.buttonClickListener = buttonClickListener;

      DeliveryTaskConfig taskConfig = manager.getTaskConfig(task.getTaskId());
      if (taskConfig != null && taskConfig.contactName() != null) {
        descriptionTextView.setText(taskConfig.contactName());
      } else {
        descriptionTextView.setVisibility(View.GONE);
      }
      if (task.getPlannedWaypoint().getTitle() != null) {
        addressTextView.setText(task.getPlannedWaypoint().getTitle());
      } else {
        addressTextView.setVisibility(View.GONE);
      }
      idsTextView.setText(String.format("ID: %s", task.getTaskId()));

      setDetailsButtonClickListener();
      configureMarkButton();
    }

    private void setDetailsButtonClickListener() {
      detailsButton.setOnClickListener(v -> {
        if (buttonClickListener != null) {
          buttonClickListener.onDetailsButtonClick(Arrays.asList(task), null);
        }
      });
    }

    private void configureMarkButton() {
      if (task.getTaskType() == TaskType.DELIVERY_UNAVAILABLE
          || task.getTaskType() == TaskType.DELIVERY_SCHEDULED_STOP) {
        markButton.setVisibility(View.GONE);
        return;
      }

      markButton.setVisibility(View.VISIBLE);
      setMarkButtonText();

      markButton.setOnClickListener(v -> {
        if (buttonClickListener != null) {
          buttonClickListener.onMarkButtonClick(Arrays.asList(task), true);
        }
      });
    }

    private void setMarkButtonText() {
      switch (task.getTaskOutcome()) {
        case TaskOutcome.SUCCEEDED:
          markButton.setText(R.string.completed);
          markButton.setEnabled(false);
          break;
        case TaskOutcome.FAILED:
          markButton.setText(R.string.attempted);
          markButton.setEnabled(false);
          break;
        default:
          markButton.setText(R.string.complete);
          markButton.setEnabled(true);
          break;
      }
    }
  }
}
