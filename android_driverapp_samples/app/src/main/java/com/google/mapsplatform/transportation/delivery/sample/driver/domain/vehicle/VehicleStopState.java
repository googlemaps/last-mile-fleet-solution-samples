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
package com.google.mapsplatform.transportation.delivery.sample.driver.domain.vehicle;

import java.security.InvalidParameterException;

/** State of a vehicle stop. */
public enum VehicleStopState {
  UNSPECIFIED {
    @Override
    public int getCode() {
      return UNSPECIFIED_CODE;
    }

    @Override
    public VehicleStopState nextState() {
      return UNSPECIFIED;
    }
  },
  NEW {
    @Override
    public int getCode() {
      return NEW_CODE;
    }

    @Override
    public VehicleStopState nextState() {
      return ENROUTE;
    }
  },
  ENROUTE {
    @Override
    public int getCode() {
      return ENROUTE_CODE;
    }

    @Override
    public VehicleStopState nextState() {
      return ARRIVED;
    }
  },
  ARRIVED {
    @Override
    public int getCode() {
      return ARRIVED_CODE;
    }

    @Override
    public VehicleStopState nextState() {
      return COMPLETED;
    }
  },
  COMPLETED {
    @Override
    public int getCode() {
      return COMPLETED_CODE;
    }

    @Override
    public VehicleStopState nextState() {
      return COMPLETED;
    }
  };

  private static final int UNSPECIFIED_CODE = 0;
  private static final int NEW_CODE = 1;
  private static final int ENROUTE_CODE = 2;
  private static final int ARRIVED_CODE = 3;
  // NOTE: this code does not exist yet on Fleet Engine.
  private static final int COMPLETED_CODE = 4;

  /** Returns the FleetEngine numerical code for the given task state. */
  public abstract int getCode();

  /** Returns the next logical stop state. */
  public abstract VehicleStopState nextState();

  /**
   * Parse the state in string to get the state code.
   *
   * @param state task state in string format
   * @return task state code
   */
  public static final int parse(String state) {
    VehicleStopState statusEnum = valueOf(state);
    return statusEnum.getCode();
  }

  public static VehicleStopState parseCode(int code) {
    for (VehicleStopState state : VehicleStopState.values()) {
      if (state.getCode() == code) {
        return state;
      }
    }

    throw new InvalidParameterException(String.format("Invalid stop state code: %s", code));
  }
}
