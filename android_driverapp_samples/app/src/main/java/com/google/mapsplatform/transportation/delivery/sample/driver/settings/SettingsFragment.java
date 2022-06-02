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
package com.google.mapsplatform.transportation.delivery.sample.driver.settings;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.google.mapsplatform.transportation.delivery.sample.driver.R;

/** Fragment that allows users to modify settings such as turning location tracking on/off. */
public class SettingsFragment extends Fragment {
  private Preferences preferences;

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_settings, container, false);

    preferences = Preferences.getInstance(getContext());

    Switch locationTrackingSwitch = view.findViewById(R.id.location_tracking_switch);
    locationTrackingSwitch.setChecked(preferences.getLocationTrackingSetting());
    locationTrackingSwitch.setOnCheckedChangeListener(
        (buttonView, isChecked) -> preferences.saveLocationTrackingSetting(isChecked));

    Switch simulationSwitch = view.findViewById(R.id.simulation_switch);
    simulationSwitch.setChecked(preferences.getSimulationSetting());
    simulationSwitch.setOnCheckedChangeListener(
        (buttonView, isChecked) -> preferences.saveSimulationSetting(isChecked));

    EditText backendUrlEditText = view.findViewById(R.id.backend_url_input);
    backendUrlEditText.setText(preferences.getBackendUrl(), TextView.BufferType.NORMAL);
    backendUrlEditText.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {}

          @Override
          public void afterTextChanged(Editable s) {
            preferences.saveBackendUrl(s.toString());
          }
        });

    EditText clientIdEditText = view.findViewById(R.id.client_id_input);
    clientIdEditText.setText(preferences.getClientId(), TextView.BufferType.NORMAL);
    clientIdEditText.addTextChangedListener(
        new TextWatcher() {
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {}

          @Override
          public void afterTextChanged(Editable s) {
            preferences.saveClientId(s.toString());
          }
        });

    return view;
  }
}
