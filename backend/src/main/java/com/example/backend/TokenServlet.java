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

import com.example.backend.auth.AuthToken;
import com.example.backend.auth.AuthTokenUtils;
import com.example.backend.utils.ServletUtils;
import com.google.common.base.Ascii;
import com.google.fleetengine.auth.token.factory.signer.SigningTokenException;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet for token endpoints to get signed tokens.
 *
 * <p>GET /token/:tokenType
 *
 * <p>tokenType: "delivery_driver", "delivery_consumer", or "fleet_reader"
 */
@Singleton
public class TokenServlet extends HttpServlet {

  private static final Logger logger = Logger.getLogger(TokenServlet.class.getName());

  @Inject
  public TokenServlet() {
    super();
  }

  private enum TokenType {
    DELIVERY_DRIVER,
    DELIVERY_CONSUMER,
    FLEET_READER,
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    ServletUtils.setStandardResponseHeaders(response);

    String tokenType = request.getPathInfo().substring(1);
    String tokenId = null;
    if (tokenType.contains("/")) {
      String[] tokenParams = tokenType.split("/", 2);
      tokenType = tokenParams[0];
      tokenId = tokenParams[1].trim();
    }
    AuthToken authToken;
    TokenType tokenTypeEnum;
    try {
      tokenTypeEnum = TokenType.valueOf(Ascii.toUpperCase(tokenType));
    } catch (IllegalArgumentException e) {
      logger.warning(
          String.format("Requested token for tokenType [%s], but did not find token.", tokenType));
      response.sendError(
          HttpServletResponse.SC_BAD_REQUEST,
          String.format("Could not find token for the given type: %s", tokenType));
      return;
    }
    try {
      switch (tokenTypeEnum) {
        case DELIVERY_DRIVER:
          if (!validateTokenId(tokenType, tokenId, response)) {
            return;
          }
          authToken = AuthTokenUtils.getDeliveryDriverToken(tokenId);
          break;
        case DELIVERY_CONSUMER:
          if (!validateTokenId(tokenType, tokenId, response)) {
            return;
          }
          authToken = AuthTokenUtils.getDeliveryConsumerToken(tokenId);
          break;
        case FLEET_READER:
          authToken = AuthTokenUtils.getDeliveryFleetReaderToken();
          break;
        default:
          logger.severe(
              String.format(
                  "Requested token for tokenType [%s], but it should not get here.", tokenType));
          response.sendError(
              HttpServletResponse.SC_BAD_REQUEST,
              String.format("Could not find token for the given type: %s", tokenType));
          return;
      }
    } catch (SigningTokenException e) {
      logger.severe(e.getMessage());
      response.sendError(
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          String.format("Finding token for tokenType [%s] failed", tokenType));
      return;
    }

    logger.info(String.format("Found token for type %s: %s", tokenType, authToken.token()));

    // Do not cache this endpoint at all.
    response.setHeader("cache-control", "no-store");
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
    response.getWriter().print(gsonBuilder.create().toJson(authToken));
    response.getWriter().flush();
  }

  /**
   * Validates that the given ID is valid. The ID should not be null or blank, and it should not be
   * the wildcard character *.
   */
  private static boolean validateTokenId(String tokenType, String id, HttpServletResponse response)
      throws IOException {
    if (id == null || id.equals("")) {
      logger.severe(
          String.format("Requested token for tokenType [%s], but no ID was supplied.", tokenType));
      response.sendError(
          HttpServletResponse.SC_BAD_REQUEST,
          String.format("Token type %s must include an ID to the claim.", tokenType));
      return false;
    } else if (id.equals("*")) {
      logger.severe(
          String.format(
              "Requested token for tokenType [%s], but a wildcard ID was supplied.", tokenType));
      response.sendError(
          HttpServletResponse.SC_BAD_REQUEST,
          String.format("Token type %s disallows a wildcard ID for the claim.", tokenType));
      return false;
    }
    return true;
  }
}
