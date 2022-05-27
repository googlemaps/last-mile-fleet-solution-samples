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

/** Possible task types according to Fleet Engine. */
public enum TaskType {
  UNSPECIFIED {
    @Override
    public int getCode() {
      return UNSPECIFIED_CODE;
    }
  },
  PICKUP {
    @Override
    public int getCode() {
      return PICKUP_CODE;
    }
  },
  DELIVERY {
    @Override
    public int getCode() {
      return DELIVERY_CODE;
    }
  },
  SCHEDULED_STOP {
    @Override
    public int getCode() {
      return SCHEDULED_STOP_CODE;
    }
  },
  UNAVAILABLE {
    @Override
    public int getCode() {
      return UNAVAILABLE_CODE;
    }
  };

  private static final int UNSPECIFIED_CODE = Task.TaskType.UNSPECIFIED;
  private static final int PICKUP_CODE = Task.TaskType.DELIVERY_PICKUP;
  private static final int DELIVERY_CODE = Task.TaskType.DELIVERY_DELIVERY;
  private static final int SCHEDULED_STOP_CODE = Task.TaskType.DELIVERY_SCHEDULED_STOP;
  private static final int UNAVAILABLE_CODE = Task.TaskType.DELIVERY_UNAVAILABLE;

  /** Returns the FleetEngine numerical code for the given task type. */
  public abstract int getCode();

  /**
   * Parses the type in string to get the type code.
   *
   * @param type task type in string format.
   * @return task type code
   */
  public static final int parse(String type) {
    TaskType statusEnum = valueOf(type);
    return statusEnum.getCode();
  }

  /**
   * Parses the Fleet Engine type code into the corresponding task type.
   *
   * @param code task type code.
   * @return task type
   */
  public static final TaskType parseCode(int code) {
    for (TaskType type : TaskType.values()) {
      if (type.getCode() == code) {
        return type;
      }
    }

    throw new InvalidParameterException(String.format("Invalid task type code: %s", code));
  }
}
