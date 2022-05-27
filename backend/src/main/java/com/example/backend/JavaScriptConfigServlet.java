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
package com.example.backend;

import com.example.backend.utils.SampleBackendUtils;
import com.example.backend.utils.ServletUtils;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.inject.Singleton;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet for retrieving and assigning delivery vehicles. */
@Singleton
public final class JavaScriptConfigServlet extends HttpServlet {

  private static final Logger logger = Logger.getLogger(JavaScriptConfigServlet.class.getName());

  /**
   * Fetches JavaScript config.
   *
   * <p>GET /config.js
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("text/javascript");
    response.setCharacterEncoding("UTF-8");

    // Do not let the browser cache js credentials.
    response.setHeader("Cache-Control", "no-store");
    
    PrintWriter responseWriter = response.getWriter();

    // A Google API key must be 39 characters long; start with "AIza"; and consist of upper- and
    // lower-case letters, numbers, hyphens, and underscores.
    String apiKey = SampleBackendUtils.backendProperties.apiKey();
    if (!Pattern.matches("AIza[0-9A-Za-z-_]{35}", apiKey)) {
      logger.log(Level.WARNING, "Invalid API key provided in configuration.");
      ServletUtils.setErrorResponse(response, "Invalid API key provided in configuration.", 500);
      return;
    }

    // Validate that the host is valid.
    String backendHost = SampleBackendUtils.backendProperties.backendHost();
    try {
      new URI(backendHost);
    } catch (URISyntaxException e) {
      logger.log(Level.WARNING, "Invalid backend host provided in configuration.");
      ServletUtils.setErrorResponse(
          response, "Invalid backend host provided in configuration.", 500);
      return;
    }
    String projectId = SampleBackendUtils.backendProperties.providerId();

    // A Cloud project ID must be 6-30 characters long; consist of lower-case letters, numbers and
    // hyphens; start with a letter; and not end with a hyphen.
    if (!Pattern.matches("[a-z][a-z0-9-]{4,28}[a-z0-9]", projectId)) {
      logger.log(Level.WARNING, "Invalid project ID provided in configuration.");
      ServletUtils.setErrorResponse(response, "Invalid project ID provided in configuration.", 500);
      return;
    }

    responseWriter.printf(
        "const API_KEY = '%s'; const BACKEND_HOST = '%s'; const PROJECT_ID = '%s';",
        apiKey, backendHost, projectId);
    responseWriter.flush();
  }
}
