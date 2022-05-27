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
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;

/** RecyclerView item decoration which adds horizontal or vertical spacing between items. */
public class SpaceItemDecoration extends RecyclerView.ItemDecoration {
    private final int spaceBetweenItems;
    private final int marginSpace;
    private final boolean horizontalSpacing;

    public SpaceItemDecoration(Context context, boolean horizontalSpacing, int spaceBetweenItems, int marginSpace) {
        int dpToPixelRatio = context.getResources()
                .getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT;
        this.spaceBetweenItems = spaceBetweenItems * dpToPixelRatio;
        this.marginSpace = marginSpace * dpToPixelRatio;
        this.horizontalSpacing = horizontalSpacing;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int currentPosition = parent.getChildAdapterPosition(view);
        if (currentPosition == 0) {
            if (horizontalSpacing) {
                outRect.left = marginSpace;
            } else {
                outRect.top = marginSpace;
            }
        }

        int lastPosition = parent.getAdapter().getItemCount() - 1;
        if (horizontalSpacing) {
            outRect.right = (currentPosition == lastPosition)
                    ? marginSpace
                    : spaceBetweenItems;
        } else {
            outRect.bottom = spaceBetweenItems;
        }
    }
}
