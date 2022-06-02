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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.google.mapsplatform.transportation.delivery.sample.driver.ItineraryButtonClickListener;
import com.google.mapsplatform.transportation.delivery.sample.driver.R;
import com.google.mapsplatform.transportation.delivery.sample.driver.domain.vehicle.AppVehicleStop;
import com.google.mapsplatform.transportation.delivery.sample.driver.utils.ItineraryListUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adapter for the recycler view on the {@link
 * com.google.mapsplatform.transportation.delivery.sample.driver.ui.itinerary.ItineraryMapFragment}.
 */
public class ItineraryMapListAdapter
    extends RecyclerView.Adapter<ItineraryMapListAdapter.ItineraryMapListItemViewHolder> {

  private List<AppVehicleStop> vehicleStops;
  private Optional<AppVehicleStop> firstAvailableStop = Optional.empty();
  private final ItineraryButtonClickListener buttonClickListener;
  private Context context;

  public ItineraryMapListAdapter(
      List<AppVehicleStop> vehicleStops, ItineraryButtonClickListener buttonClickListener) {
    this.vehicleStops = new ArrayList<>(vehicleStops);
    this.buttonClickListener = buttonClickListener;
  }

  @Override
  public ItineraryMapListItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    context = parent.getContext();
    View view = LayoutInflater.from(context).inflate(R.layout.itinerary_map_list_item, null);

    LayoutParams layoutParams =
        new LayoutParams(
            context.getResources().getDimensionPixelSize(R.dimen.itinerary_map_item_width),
            context.getResources().getDimensionPixelSize(R.dimen.itinerary_map_item_height));
    view.setLayoutParams(layoutParams);
    updateFirstAvailableStop();
    return new ItineraryMapListItemViewHolder(view);
  }

  public void setVehicleStops(List<AppVehicleStop> stops) {
    vehicleStops = new ArrayList<>(stops);
    updateFirstAvailableStop();
    notifyDataSetChanged();
  }

  private void updateFirstAvailableStop() {
    Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Updating first available stop...");
    firstAvailableStop = ItineraryListUtils.getFirstAvailableStop(vehicleStops);
  }

  @Override
  public void onBindViewHolder(ItineraryMapListItemViewHolder holder, int position) {

    AppVehicleStop vehicleStop = vehicleStops.get(position);
    Logger.getLogger(this.getClass().getName()).log(Level.INFO, firstAvailableStop.toString());
    Logger.getLogger(this.getClass().getName()).log(Level.INFO, vehicleStop.toString());
    boolean showNavigateButton =
        firstAvailableStop.isPresent() && firstAvailableStop.get().equals(vehicleStop);
    holder.configure(vehicleStop, position, context, buttonClickListener, showNavigateButton);
  }

  @Override
  public int getItemCount() {
    return vehicleStops.size();
  }

  /** View holder for the itinerary map list item. */
  protected static class ItineraryMapListItemViewHolder extends RecyclerView.ViewHolder {

    private final TextView positionTextView;
    private final TextView titleTextView;
    private final Button detailsButton;
    private final Button navigateButton;

    private int position;
    private AppVehicleStop vehicleStop;
    private boolean showNavigateButton;
    private ItineraryButtonClickListener buttonClickListener;
    private Context context;

    public ItineraryMapListItemViewHolder(View v) {
      super(v);

      positionTextView = v.findViewById(R.id.position_text_view);
      titleTextView = v.findViewById(R.id.title_text_view);
      detailsButton = v.findViewById(R.id.details_button);
      navigateButton = v.findViewById(R.id.navigate_button);
    }

    public void configure(
        AppVehicleStop vehicleStop,
        int position,
        Context context,
        ItineraryButtonClickListener buttonClickListener,
        boolean showNavigateButton) {
      this.vehicleStop = vehicleStop;
      this.position = position;
      this.buttonClickListener = buttonClickListener;
      this.context = context;
      this.showNavigateButton = showNavigateButton;

      titleTextView.setText(ItineraryListUtils.getTitleText(vehicleStop));
      configurePositionTextView();
      configureNavigateButton();
      setButtonClickListeners();
    }

    private void configurePositionTextView() {
      positionTextView.setText(ItineraryListUtils.getPositionText(position));
      positionTextView.setTextColor(ItineraryListUtils.getPositionTextColor());
      positionTextView.setBackgroundResource(ItineraryListUtils.getPositionBackgroundResourceId());
    }

    private void configureNavigateButton() {
      navigateButton.setVisibility(showNavigateButton ? View.VISIBLE : View.GONE);
    }

    private void setButtonClickListeners() {
      detailsButton.setOnClickListener(
          v -> {
            if (buttonClickListener != null) {
              buttonClickListener.onDetailsButtonClick(null, vehicleStop);
            }
          });

      navigateButton.setOnClickListener(
          v -> {
            if (buttonClickListener != null) {
              buttonClickListener.onNavigateButtonClick(vehicleStop);
            }
          });
    }
  }
}
