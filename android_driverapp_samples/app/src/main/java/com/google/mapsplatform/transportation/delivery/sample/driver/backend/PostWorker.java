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
package com.google.mapsplatform.transportation.delivery.sample.driver.backend;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.io.IOException;

/**
 * This worker class will execute the post request, and upon success, return the data contained in
 * the request.
 */
public class PostWorker extends Worker {

  public PostWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
    super(context, workerParams);
  }

  @NonNull
  @Override
  public Result doWork() {
    String url = getInputData().getString("url");
    boolean ignoreOutput = getInputData().getBoolean("ignoreOutput", false);
    if (url == null) {
      return Result.failure(new Data.Builder().putString("error", "url is not defined").build());
    }
    byte[] payload = getInputData().getByteArray("payload");

    try {
      // Only using postWithPayload here, because post() is only used in closeTask() which is
      // to be removed.
      Data.Builder resultData = new Data.Builder();
      String output = SampleBackend.postWithPayload(url, payload);
      if (!ignoreOutput) {
        resultData.putString("json", output);
      }
      return Result.success(resultData.build());
    } catch (IOException e) {
      return Result.failure(new Data.Builder().putString("error", e.getMessage()).build());
    }
  }
}
