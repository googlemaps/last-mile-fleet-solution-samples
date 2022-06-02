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
package com.google.mapsplatform.transportation.delivery.sample.driver.domain.task;

import com.google.android.libraries.mapsplatform.transportation.driver.api.base.data.Task;
import java.security.InvalidParameterException;

/** Possible task outcomes according to Fleet Engine. */
public enum TaskOutcome {
  UNSPECIFIED {
    @Override
    public int getCode() {
      return UNSPECIFIED_CODE;
    }
  },
  FAILED {
    @Override
    public int getCode() {
      return FAILED_CODE;
    }
  },
  SUCCEEDED {
    @Override
    public int getCode() {
      return SUCCEEDED_CODE;
    }
  };

  private static final int UNSPECIFIED_CODE = Task.TaskOutcome.UNSPECIFIED;
  private static final int FAILED_CODE = Task.TaskOutcome.FAILED;
  private static final int SUCCEEDED_CODE = Task.TaskOutcome.SUCCEEDED;

  /** Returns the FleetEngine numerical code for the given task state. */
  public abstract int getCode();


  /**
   * Parses the state in string to get the state code.
   *
   * @param state task state in string format
   * @return task state code
   */
  public static final int parse(String state) {
    TaskOutcome statusEnum = valueOf(state);
    return statusEnum.getCode();
  }

  /**
   * Parses the Fleet Engine state code into the corresponding task state.
   *
   * @param code task state code
   * @return task state
   */
  public static final TaskOutcome parseCode(int code) {
    for (TaskOutcome state : TaskOutcome.values()) {
      if (state.getCode() == code) {
        return state;
      }
    }

    throw new InvalidParameterException(String.format("Invalid task state code: %s", code));
  }
}
