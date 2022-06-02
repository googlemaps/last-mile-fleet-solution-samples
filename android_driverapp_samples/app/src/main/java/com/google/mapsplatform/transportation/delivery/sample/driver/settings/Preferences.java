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

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import java.util.UUID;

/** Reads and writes data to and from SharedPreferences. */
public class Preferences {
  public static final String LOCATION_TRACKING_KEY = "location_tracking";
  public static final String SIMULATION_KEY = "simulation";
  public static final String PROVIDER_ID_KEY = "provider_id";
  public static final String BACKEND_URL_KEY = "backend_url";
  public static final String CLIENT_ID_KEY = "client_id";

  private static volatile Preferences preferences;
  private final SharedPreferences sharedPreferences;

  public static Preferences getInstance(Context context) {
    if (preferences == null) {
      synchronized (Preferences.class) {
        if (preferences == null) {
          preferences = new Preferences(context);
        }
      }
    }
    return preferences;
  }

  private Preferences(Context context) {
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
  }

  public void saveLocationTrackingSetting(boolean enabled) {
    sharedPreferences.edit().putBoolean(LOCATION_TRACKING_KEY, enabled).apply();
  }

  public boolean getLocationTrackingSetting() {
    return sharedPreferences.getBoolean(LOCATION_TRACKING_KEY, true);
  }

  public void saveSimulationSetting(boolean enabled) {
    sharedPreferences.edit().putBoolean(SIMULATION_KEY, enabled).apply();
  }

  public boolean getSimulationSetting() {
    return sharedPreferences.getBoolean(SIMULATION_KEY, true);
  }

  public void saveBackendUrl(String backendUrl) {
    sharedPreferences.edit().putString(BACKEND_URL_KEY, backendUrl).apply();
  }

  public String getBackendUrl() {
    return sharedPreferences.getString(BACKEND_URL_KEY, "http://localhost:8080");
  }

  public void saveClientId(String clientId) {
    sharedPreferences.edit().putString(CLIENT_ID_KEY, clientId).apply();
  }

  public String getClientId() {
    String clientId = sharedPreferences.getString(CLIENT_ID_KEY, null);
    if (clientId == null) {
      clientId = UUID.randomUUID().toString();
      saveClientId(clientId);
    }
    return clientId;
  }
}
