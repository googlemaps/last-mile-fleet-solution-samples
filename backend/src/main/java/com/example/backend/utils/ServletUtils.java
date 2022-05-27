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
package com.example.backend.utils;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.example.backend.json.ErrorResponse;
import com.example.backend.json.GsonProvider;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import google.maps.fleetengine.delivery.v1.DeliveryVehicle;
import google.maps.fleetengine.delivery.v1.Task;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import javax.servlet.http.HttpServletResponse;

/** Util class for common functions for servlets. */
public final class ServletUtils {

  private static final JsonFormat.Printer jsonPrinter = JsonFormat.printer()
      .preservingProtoFieldNames();
  private static final JsonFormat.Parser jsonParser = JsonFormat.parser();

  private ServletUtils() {}

  /** Sets response headers with common contents in all responses */
  public static void setStandardResponseHeaders(HttpServletResponse response) {
    response.setContentType("application/json");
    response.setCharacterEncoding(UTF_8.name());
  }

  /**
   * Writes an error to the given response with the given message and status.
   *
   * @throws IOException if an error occurs when writing to response.
   */
  public static void setErrorResponse(HttpServletResponse response, String message, int status)
      throws IOException {
    ErrorResponse errorResponse = ErrorResponse.create(message, status);
    response.setStatus(status);
    response.getWriter().write(GsonProvider.get().toJson(errorResponse));
  }

  /**
   * Takes a proto message and writes its JSON representation into the response writer.
   *
   * @throws IOException if an error occurs when writing to response.
   */
  public static void writeProtoJson(PrintWriter responseWriter, Message message)
      throws IOException {
    jsonPrinter.appendTo(message, responseWriter);
  }

  public static Task readJsonProto(Reader requestReader, Task.Builder builder) throws IOException {
    jsonParser.merge(requestReader, builder);
    return builder.build();
  }

  public static DeliveryVehicle readJsonProto(Reader requestReader, DeliveryVehicle.Builder builder)
      throws IOException {
    jsonParser.merge(requestReader, builder);
    return builder.build();
  }

  /** Parse a query string for a particular name. Returns the value if found, null otherwise. */
  public static String getUrlQueryData(String queryString, String name) {
    for (String queryItem : queryString.split("&")) {
        String[] queryParts = queryItem.split("=");
        if (queryParts.length == 2 && queryParts[0].equals(name)) {
          return queryParts[1];
        }
      }
    return null;
  }
}
