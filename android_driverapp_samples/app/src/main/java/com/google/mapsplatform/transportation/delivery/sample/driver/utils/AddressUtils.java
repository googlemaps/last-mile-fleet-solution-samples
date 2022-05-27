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
import android.location.Address;
import android.location.Geocoder;
import com.google.android.gms.maps.model.LatLng;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/** Converts a latitude and longitude into an address string. */
public class AddressUtils {

    public static String shortAddress(LatLng latLng, Context context) {
        Address address = address(latLng, context);
        return (address != null)
                ? String.format("%s %s", address.getSubThoroughfare(), address.getThoroughfare())
                : "";
    }

    public static String longAddress(LatLng latLng, Context context) {
        Address address = address(latLng, context);
        return (address != null)
                ? String.format("%s %s\n%s, %s %s", address.getSubThoroughfare(),
                    address.getThoroughfare(),
                    address.getLocality(),
                    address.getAdminArea(),
                    address.getPostalCode())
                : "";
    }

    private static Address address(LatLng latLng, Context context) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());

        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude,
                    1);
            if (addresses.size() < 1) {
                return null;
            }

            return addresses.get(0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
