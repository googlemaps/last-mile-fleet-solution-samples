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

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.Task.TaskOutcome;
import com.google.mapsplatform.transportation.delivery.sample.driver.ItineraryButtonClickListener;
import com.google.mapsplatform.transportation.delivery.sample.driver.R;
import com.google.mapsplatform.transportation.delivery.sample.driver.adapter.ItineraryListAdapter.ItineraryListItemViewHolder;
import com.google.mapsplatform.transportation.delivery.sample.driver.domain.vehicle.AppVehicleStop;
import com.google.mapsplatform.transportation.delivery.sample.driver.utils.ItineraryListUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Adapter for the recycler view on the {@link
 * com.google.mapsplatform.transportation.delivery.sample.driver.ui.itinerary.ItineraryListFragment}.
 */
public class ItineraryListAdapter extends RecyclerView.Adapter<ItineraryListItemViewHolder> {

  private List<AppVehicleStop> vehicleStops;
  private Optional<AppVehicleStop> firstAvailableStop = Optional.empty();
  private Context context;
  private ItemTouchHelper itemTouchHelper;
  private final VehicleStopsReorderListener vehicleStopsReorderListener;
  private final ItineraryButtonClickListener buttonClickListener;
  private Set<Integer> expandedPositions;

  private final ItemTouchHelper.SimpleCallback itemTouchHelperCallback =
      new SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
            RecyclerView.ViewHolder target) {
          int fromPosition = viewHolder.getAdapterPosition();
          int toPosition = target.getAdapterPosition();
          notifyItemMoved(fromPosition, toPosition);
          Collections.swap(vehicleStops, fromPosition, toPosition);
          ((ItineraryListItemViewHolder) viewHolder).updatePosition(toPosition);
          ((ItineraryListItemViewHolder) target).updatePosition(fromPosition);

          vehicleStopsReorderListener.vehicleStopsReordered(vehicleStops);
          return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
          // Not used.
        }

        @Override
        public boolean isLongPressDragEnabled() {
          return false;
        }
      };

  public ItineraryListAdapter(List<AppVehicleStop> vehicleStops,
      VehicleStopsReorderListener vehicleStopsReorderListener,
      ItineraryButtonClickListener buttonClickListener) {
    this.vehicleStops = new ArrayList<>(vehicleStops);
    updateFirstAvailableStop();
    this.vehicleStopsReorderListener = vehicleStopsReorderListener;
    this.buttonClickListener = buttonClickListener;
    this.expandedPositions = new HashSet<Integer>();
  }

  public interface VehicleStopsReorderListener {
    void vehicleStopsReordered(List<AppVehicleStop> vehicleStops);
  }

  @Override
  public int getItemCount() {
    return vehicleStops.size();
  }

  @Override
  public ItineraryListItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    context = parent.getContext();
    View view = LayoutInflater.from(context).inflate(R.layout.itinerary_list_item, null);

    LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT,
        LayoutParams.WRAP_CONTENT);
    view.setLayoutParams(layoutParams);

    return new ItineraryListItemViewHolder(view);
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onBindViewHolder(ItineraryListItemViewHolder holder, int position) {
    AppVehicleStop vehicleStop = vehicleStops.get(position);
    boolean showNavigateButton =
        firstAvailableStop.isPresent() && firstAvailableStop.get().equals(vehicleStop);
    holder.configure(vehicleStop, position, this, buttonClickListener, showNavigateButton);

    holder.dragIndicator.setOnTouchListener((v, e) -> {
      itemTouchHelper.startDrag(holder);
      return false;
    });
  }

  @Override
  public void onAttachedToRecyclerView(RecyclerView recyclerView) {
    super.onAttachedToRecyclerView(recyclerView);
    itemTouchHelper = new ItemTouchHelper(itemTouchHelperCallback);
    itemTouchHelper.attachToRecyclerView(recyclerView);
  }

  public void setVehicleStops(List<AppVehicleStop> stops) {
    vehicleStops = new ArrayList<>(stops);
    updateFirstAvailableStop();
    notifyDataSetChanged();
  }

  private void updateFirstAvailableStop() {
    firstAvailableStop = ItineraryListUtils.getFirstAvailableStop(vehicleStops);
  }

  public void addExpandedPosition(int position) {
    expandedPositions.add(position);
  }

  public void removeExpandedPosition(int position) {
    expandedPositions.remove(position);
  }

  public boolean expandedPositionsContain(int position) {
    return expandedPositions.contains(position);
  }

  /**
   * View holder for an itinerary list item.
   */
  protected static class ItineraryListItemViewHolder extends RecyclerView.ViewHolder {

    private final ImageView dragIndicator;
    private final ImageView statusImageView;
    private final TextView positionTextView;
    private final TextView titleTextView;
    private final ImageButton expandButton;
    private final LinearLayout expandableView;
    private final Button detailsButton;
    private final Button navigateButton;
    private final RecyclerView tasksInfoRecyclerView;

    private boolean isExpanded = false;
    private AppVehicleStop vehicleStop;
    private int position;
    private boolean showNavigateButton;
    private ItineraryButtonClickListener buttonClickListener;
    private ItineraryListAdapter adapter;

    public ItineraryListItemViewHolder(View v) {
      super(v);

      statusImageView = v.findViewById(R.id.status_image_view);
      titleTextView = v.findViewById(R.id.title_text_view);
      positionTextView = v.findViewById(R.id.position_text_view);

      expandableView = v.findViewById(R.id.expandable_view);
      expandableView.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
      expandButton = v.findViewById(R.id.expand_button);
      dragIndicator = v.findViewById(R.id.drag_indicator);

      detailsButton = v.findViewById(R.id.details_button);
      navigateButton = v.findViewById(R.id.navigate_button);

      tasksInfoRecyclerView = v.findViewById(R.id.tasks_info_recycler_view);
      tasksInfoRecyclerView.setLayoutManager(new LinearLayoutManager(v.getContext()));
    }

    public void configure(AppVehicleStop vehicleStop, int position, ItineraryListAdapter adapter,
        ItineraryButtonClickListener buttonClickListener, boolean showNavigateButton) {
      this.vehicleStop = vehicleStop;
      this.position = position;
      this.showNavigateButton = showNavigateButton;
      this.buttonClickListener = buttonClickListener;
      this.adapter = adapter;

      isExpanded = adapter.expandedPositionsContain(position);
      setExpanded();

      tasksInfoRecyclerView.setAdapter(new TaskInfoAdapter(vehicleStop.getTasks(),
          buttonClickListener));

      titleTextView.setText(ItineraryListUtils.getTitleText(vehicleStop));
      if (ItineraryListUtils.allTasksHaveOutcome(vehicleStop.getTasks())) {
        configureStatusImageView();
      } else {
        configurePositionTextView();
      }
      configureNavigateButton();
      setButtonClickListeners();
    }

    public void updatePosition(int position) {
      this.position = position;
      configurePositionTextView();
    }

    private void configureStatusImageView() {
      positionTextView.setVisibility(View.GONE);
      statusImageView.setVisibility(View.VISIBLE);
      if (vehicleStop.getTasks().stream()
          .anyMatch(task -> task.getTaskOutcome() == TaskOutcome.FAILED)) {
        statusImageView.setImageResource(R.drawable.ic_error_circle);
      } else {
        statusImageView.setImageResource(R.drawable.ic_check_circle_green);
      }
    }

    private void configurePositionTextView() {
      statusImageView.setVisibility(View.GONE);
      positionTextView.setVisibility(View.VISIBLE);
      positionTextView.setText(ItineraryListUtils.getPositionText(position));
      positionTextView.setTextColor(ItineraryListUtils.getPositionTextColor());
      positionTextView.setBackgroundResource(ItineraryListUtils.getPositionBackgroundResourceId());
    }

    private void configureNavigateButton() {
      navigateButton.setVisibility(showNavigateButton ? View.VISIBLE : View.GONE);
    }

    private void setButtonClickListeners() {
      detailsButton.setOnClickListener(view -> {
        if (buttonClickListener != null) {
          buttonClickListener.onDetailsButtonClick(null, vehicleStop);
        }
      });
      navigateButton.setOnClickListener(view -> {
        if (buttonClickListener != null) {
          buttonClickListener.onNavigateButtonClick(vehicleStop);
        }
      });

      expandButton.setOnClickListener(v1 -> {
        isExpanded = !isExpanded;
        setExpanded();
        if (isExpanded) {
          adapter.addExpandedPosition(position);
        } else {
          adapter.removeExpandedPosition(position);
        }
      });
    }

    private void setExpanded() {
      expandableView.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
      expandButton.setRotation(isExpanded ? 180 : 0);
      dragIndicator.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
    }
  }
}
