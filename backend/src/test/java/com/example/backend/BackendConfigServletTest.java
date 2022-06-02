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

import static com.google.common.truth.Truth.assertThat;

import com.example.backend.auth.grpcservice.GrpcServiceModule;
import com.google.inject.Guice;
import java.io.IOException;
import java.io.InputStream;
import javax.inject.Inject;
import javax.servlet.ServletException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Tests for exercising the backend config endpoint. */
@RunWith(JUnit4.class)
public class BackendConfigServletTest {
  @Inject BackendConfigServlet servlet;

  MockHttpServletRequest request;
  MockHttpServletResponse response;

  @Before
  public void setUp() {
    Guice.createInjector(new GrpcServiceModule()).injectMembers(this);
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
  }

  @Test
  public void doesNotImplementGet() throws ServletException, IOException {
    servlet.service(request, response);
    assertThat(response.getStatus()).isEqualTo(501);
  }

  @Test
  public void handlesBackendConfigUpload() throws ServletException, IOException {
    try (InputStream testStream =
        this.getClass().getClassLoader().getResourceAsStream("test.json")) {
      servlet.serveUpload(testStream, response);
    }
    assertThat(response.getStatus()).isEqualTo(200);
  }
}
