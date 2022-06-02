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
package com.example.backend.auth;

import com.google.auto.value.AutoValue;

/** Representation of an Authentication token used by Fleet Engine. */
@AutoValue
public abstract class AuthToken {

  abstract long creationTimestampMs();

  abstract long expirationTimestampMs();

  public abstract String token();

  public static Builder builder() {
    return new AutoValue_AuthToken.Builder();
  }

  /**
   * Builder for AuthToken class
   */
  @AutoValue.Builder
  public abstract static class Builder {

    /**
     * Setter for creationTimestampMs.
     *
     * @param value creation timestamp in milliseconds
     */
    public abstract Builder setCreationTimestampMs(long value);

    /**
     * Setter for expirationTimestampMs.
     *
     * @param value expiration timestamp in milliseconds
     */
    public abstract Builder setExpirationTimestampMs(long value);

    /**
     * Setter for token.
     *
     * @param value JWT string
     */
    public abstract Builder setToken(String value);

    public abstract AuthToken build();
  }
}
