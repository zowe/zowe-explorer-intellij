/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   Uladzislau Kalesnikau
 */

package tests.utils

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import java.util.concurrent.TimeUnit

/** Mock web server manager. Provides functionality to work with the plugin without the need to have the real server */
class MockWebServerManager private constructor() {

  /** Mock response dispatcher functionality for the mock web server */
  private class MockResponseDispatcher : Dispatcher() {
    private var endpoints = mutableListOf<MockEndpointDefinition>()

    private fun getResourceText(resourcePath: String): String {
      return javaClass.classLoader.getResource(resourcePath)?.readText() ?: ""
    }

    /**
     * Read mock JSON file by the provided [mockFilePath]
     * @param mockFilePath the mock file path to read
     * @return the contents of the file
     */
    fun readMockJson(mockFilePath: String): String {
      return getResourceText("mock/${mockFilePath}.json")
    }

    /**
     * Inject the mock server endpoint. The name of the endpoint must be unique.
     * If the response should be a JSON from a mock file, the [jsonMock] file name must be provided.
     * If the [customHandler] is not provided, the default response is used with the 503 HTTP error
     * @param name the name of the endpoint
     * @param jsonMock the JSON file mock
     * @param endpointResolver the function to recognize the request to provide a corresponding endpoint
     * @param customHandler the function to provide a custom behavior of the endpoint response
     */
    fun injectEndpoint(
      name: String,
      jsonMock: String,
      endpointResolver: (RecordedRequest?) -> Boolean,
      customHandler: (RecordedRequest?) -> MockResponse
    ) {
      if (endpoints.find { it.name == name } != null) {
        throw Exception("Injected endpoint must have a unique name. Provided endpoint name \"$name\" already injected")
      }

      val customHandlerOrJson = if (jsonMock.isNotEmpty()) {
        {
          val jsonResponse = readMockJson(jsonMock)
          if (jsonResponse.isNotEmpty())
            MockResponse().setBody(jsonResponse)
          else
            MockResponse()
              .setBody("The endpoint request is recognized, but the JSON response mock is empty")
              .setResponseCode(503)
        }
      } else {
        customHandler
      }

      endpoints.add(MockEndpointDefinition(name, endpointResolver, customHandlerOrJson))
    }

    /** Remove an injected endpoint by the [name] */
    fun removeEndpoint(name: String) {
      endpoints.removeAll { it.name == name }
    }

    /** Clean the response dispatcher's endpoints */
    fun removeAllEndpoints() {
      endpoints.clear()
    }

    // TODO: logs
    /**
     * Dispatch a request with a response if it could be handled with the previously injected endpoint
     * @param request the request to dispatch
     * @return a mock response or a response with 404 HTTP code
     */
    override fun dispatch(request: RecordedRequest): MockResponse {
      return endpoints
        .firstOrNull { it.endpointResolver(request) }
        ?.customHandler
        ?.let { it(request) }
        ?: MockResponse().setBody("Response is not implemented").setResponseCode(404)
    }
  }

  companion object {
    private val mockServer by lazy { MockWebServer() }
    private val responseDispatcher by lazy { MockResponseDispatcher() }
    private var isMockServerStarted = false

    /** Prepare a mock web server instance if it is not ready yet. Will return the prepared mock web server */
    fun prepareMockServer(): MockWebServer {
      if (isMockServerStarted) return mockServer

      val localhostCertificate = HeldCertificate.Builder()
        .addSubjectAlternativeName("localhost")
        .addSubjectAlternativeName("127.0.0.1")
        .duration(30, TimeUnit.MINUTES)
        .build()
      val serverCertificates = HandshakeCertificates.Builder()
        .heldCertificate(localhostCertificate)
        .build()
      mockServer.dispatcher = responseDispatcher
      mockServer.useHttps(serverCertificates.sslSocketFactory(), false)
      mockServer.start()
      isMockServerStarted = true

      return mockServer
    }

    val url by lazy {
      val mockServer = prepareMockServer()
      "https://127.0.0.1:${mockServer.port}"
    }

    /** @see MockResponseDispatcher.injectEndpoint */
    fun injectEndpoint(
      name: String,
      jsonMock: String = "",
      endpointResolver: (RecordedRequest?) -> Boolean = { false },
      customHandler: (RecordedRequest?) -> MockResponse = {
        MockResponse()
          .setBody("The endpoint request is recognized, but the response is not implemented")
          .setResponseCode(503)
      }
    ) {
      prepareMockServer()
      responseDispatcher.injectEndpoint(name, jsonMock, endpointResolver, customHandler)
    }

    fun removeAllEndpoints() {
      prepareMockServer()
      responseDispatcher.removeAllEndpoints()
    }
  }
}
