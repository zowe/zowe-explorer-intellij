/*
 * Copyright (c) 2025 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */

package org.zowe.explorer.v3.diffs

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * Mock web server implementation to provide a valid case to work with for Rena.
 * To modify, it is enough to change injected endpoints in a proper way
 */
class MockWebServerActivity : ProjectActivity {
  private data class ValidationListElem(
    val name: String,
    val validator: (RecordedRequest?) -> Boolean,
    val handler: (RecordedRequest?) -> MockResponse
  )

  private class MockResponseDispatcher : Dispatcher() {

    private var validationList = mutableListOf<ValidationListElem>()

    fun injectEndpoint(
      name: String,
      validator: (RecordedRequest?) -> Boolean,
      handler: (RecordedRequest?) -> MockResponse
    ) {
      validationList.add(ValidationListElem(name, validator, handler))
    }

    fun removeEndpoint(name: String) {
      validationList.removeAll { it.name == name }
    }

    fun removeAllEndpoints() {
      validationList.clear()
    }

    override fun dispatch(request: RecordedRequest): MockResponse {
      println("Request received: ${request.requestLine}")

      val foundValidator = validationList
        .firstOrNull { it.validator(request) }

      println("Validator for the request: ${foundValidator?.name}")

      return foundValidator
        ?.handler
        ?.let { it(request) }
        ?: MockResponse().setBody("Response is not implemented").setResponseCode(404)
    }
  }

  override suspend fun execute(project: Project) {
    val localhost = withContext(Dispatchers.IO) {
      InetAddress.getByName("localhost")
    }.canonicalHostName
    val localhostCertificate = HeldCertificate.Builder()
      .addSubjectAlternativeName(localhost)
      .duration(60, TimeUnit.MINUTES)
      .build()
    val serverCertificates = HandshakeCertificates.Builder()
      .heldCertificate(localhostCertificate)
      .build()
    val mockServer = MockWebServer()
    val responseDispatcher = MockResponseDispatcher()
    mockServer.dispatcher = responseDispatcher
    mockServer.useHttps(serverCertificates.sslSocketFactory(), false)
    mockServer.start(49222)

    responseDispatcher.injectEndpoint(
      "mock:zosmf/resttopology/systems",
      { it?.requestLine?.contains("/zosmf/resttopology/systems") ?: false },
      {
        MockResponse()
          .setBody(
            "{\n" +
            "    \"numRows\": 1,\n" +
            "    \"items\": [\n" +
            "        {\n" +
            "            \"systemNickName\": \"S0W1\",\n" +
            "            \"groupNames\": \"TEST\",\n" +
            "            \"cpcSerial\": \"\",\n" +
            "            \"zosVR\": \"z/OS V2R2\",\n" +
            "            \"systemName\": \"S0W1\",\n" +
            "            \"jesType\": \"JES2\",\n" +
            "            \"sysplexName\": \"ADCDPL\",\n" +
            "            \"jesMemberName\": \"S0W1\",\n" +
            "            \"httpProxyName\": \"\",\n" +
            "            \"ftpDestinationName\": \"\",\n" +
            "            \"url\": \"https://test.com/zosmf\",\n" +
            "            \"cpcName\": \"\"\n" +
            "        }\n" +
            "    ]\n" +
            "}"
          )
      }
    )

    responseDispatcher.injectEndpoint(
      "mock:zosmf/info",
      { it?.requestLine?.contains("/zosmf/info") ?: false },
      {
        MockResponse()
          .setBody(
            "{\n" +
            "    \"zos_version\": \"04.26.00\",\n" +
            "    \"zosmf_port\": \"10443\",\n" +
            "    \"zosmf_version\": \"26\",\n" +
            "    \"zosmf_hostname\": \"TEST.COM\",\n" +
            "    \"plugins\": [\n" +
            "        {\n" +
            "            \"pluginVersion\": \"24,unknown,unknown\",\n" +
            "            \"pluginDefaultName\": \"MyPlugin\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"pluginVersion\": \"HSMA230;PH15636P;2019-09-24T08:56:36\",\n" +
            "            \"pluginDefaultName\": \"Cloud Provisioning\",\n" +
            "            \"pluginStatus\": \"ACTIVE\"\n" +
            "        }\n" +
            "    ],\n" +
            "    \"zosmf_saf_realm\": \"SAFRealm\",\n" +
            "    \"zosmf_full_version\": \"26.0\",\n" +
            "    \"api_version\": \"1\"\n" +
            "}"
          )
      }
    )

    // X-IBM-Attributes: base,total
    responseDispatcher.injectEndpoint(
      "mock:zosmf/restfiles/ds?dslevel=ZOSMF.*",
      { it?.requestLine?.contains("/zosmf/restfiles/ds?dslevel=ZOSMF.*") ?: false },
      {
        MockResponse()
          .setBody(
            "{\n" +
            "    \"items\": [\n" +
            "        {\n" +
            "            \"dsname\": \"ZOSMF.TEST1\",\n" +
            "            \"blksz\": \"27920\",\n" +
            "            \"catnm\": \"CATALOG.Z23D.MASTER\",\n" +
            "            \"cdate\": \"2023/08/28\",\n" +
            "            \"dev\": \"3390\",\n" +
            "            \"dsorg\": \"PS\",\n" +
            "            \"edate\": \"***None***\",\n" +
            "            \"extx\": \"1\",\n" +
            "            \"lrecl\": \"80\",\n" +
            "            \"migr\": \"NO\",\n" +
            "            \"mvol\": \"N\",\n" +
            "            \"ovf\": \"NO\",\n" +
            "            \"rdate\": \"2024/10/09\",\n" +
            "            \"recfm\": \"FB\",\n" +
            "            \"sizex\": \"6\",\n" +
            "            \"spacu\": \"TRACKS\",\n" +
            "            \"used\": \"16\",\n" +
            "            \"vol\": \"D3SYS1\",\n" +
            "            \"vols\": \"D3SYS1\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"dsname\": \"ZOSMF.TEST2\",\n" +
            "            \"blksz\": \"27920\",\n" +
            "            \"catnm\": \"CATALOG.Z23D.MASTER\",\n" +
            "            \"cdate\": \"2023/08/28\",\n" +
            "            \"dev\": \"3390\",\n" +
            "            \"dsorg\": \"PS\",\n" +
            "            \"edate\": \"***None***\",\n" +
            "            \"extx\": \"1\",\n" +
            "            \"lrecl\": \"80\",\n" +
            "            \"migr\": \"NO\",\n" +
            "            \"mvol\": \"N\",\n" +
            "            \"ovf\": \"NO\",\n" +
            "            \"rdate\": \"2024/10/09\",\n" +
            "            \"recfm\": \"FB\",\n" +
            "            \"sizex\": \"6\",\n" +
            "            \"spacu\": \"TRACKS\",\n" +
            "            \"used\": \"16\",\n" +
            "            \"vol\": \"D3SYS1\",\n" +
            "            \"vols\": \"D3SYS1\"\n" +
            "        }" +
            "    ],\n" +
            "    \"returnedRows\": 2,\n" +
            "    \"JSONversion\": 1\n" +
            "}"
          )
      }
    )

    var isFirstFetchTest1 = true
    responseDispatcher.injectEndpoint(
      "mock:zosmf/restfiles/ds/-(D3SYS1)/ZOSMF.TEST1",
      { it?.requestLine?.contains("/zosmf/restfiles/ds/-(D3SYS1)/ZOSMF.TEST1") ?: false },
      {
        if (isFirstFetchTest1) {
          isFirstFetchTest1 = false
          MockResponse()
            .setBody(
              "FIRST\n" +
              "FETCH\n" +
              "TEXT1"
            )
        } else {
          MockResponse()
            .setBody(
              "SECOND\n" +
              "FETCH\n" +
              "TEXT1"
            )
        }
      }
    )

    responseDispatcher.injectEndpoint(
      "mock:zosmf/restfiles/ds/-(D3SYS1)/ZOSMF.TEST2",
      { it?.requestLine?.contains("/zosmf/restfiles/ds/-(D3SYS1)/ZOSMF.TEST2") ?: false },
      {
        MockResponse()
          .setBody(
            "=======\n" +
            "FETCH\n" +
            "TEXT2\n" +
            "======="
          )
      }
    )

    println(
      "Mock web server is prepared and ready to work with for the next 60 minutes.\n" +
      "Use the next values to connect to it:\n" +
      "  URL: https://${mockServer.hostName}:${mockServer.port}\n" +
      "  Username: ZOSMF\n" +
      "  Password: ZOSMF\n" +
      "  Accept self-signed: false"
    )
  }

}
