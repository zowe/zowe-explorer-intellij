/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.state.config.connection

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.zowe.explorer.testutils.MockkAwareShouldSpec
import org.zowe.explorer.v3.SupportedSchemes
import org.zowe.explorer.v3.state.config.ConfigType
import org.zowe.kotlinsdk.annotations.ZVersion

class HttpConnectionConfigTestSpec : MockkAwareShouldSpec({
  context("v3/state/config/connection module: HttpConnectionConfig") {
    fun buildHttpConnectionConfig(
      name: String = "test_name",
      host: String = "test.com",
      port: Int = 1234,
      basePath: String = "/test/base/path",
      rejectUnauthorized: Boolean = true,
      isHostnameVerified: Boolean = false
    ) = HttpConnectionConfig(
      uuid = "test_uuid",
      configType = ConfigType.HTTP_CONNECTION_CONFIG_V1,
      name = name,
      scheme = SupportedSchemes.HTTPS,
      host = host,
      port = port,
      zVersion = ZVersion.ZOS_2_4,
      ussOwner = "TSTUSR",
      basePath = basePath,
      rejectUnauthorized = rejectUnauthorized,
      isHostnameVerified = isHostnameVerified
    )

    should("equals. The configs are the same instance") {
      val connectionConfig = buildHttpConnectionConfig()

      assertSoftly { connectionConfig.equals(connectionConfig) shouldBe true }
    }

    should("equals. The other object is not an HttpConnectionConfig") {
      val connectionConfig = buildHttpConnectionConfig()

      assertSoftly { connectionConfig.equals("not_a_connection_config") shouldBe false }
      assertSoftly { connectionConfig.equals(null) shouldBe false }
    }

    should("equals. The configs are fully equal") {
      val connectionConfig = buildHttpConnectionConfig()
      val otherConnectionConfig = buildHttpConnectionConfig()

      assertSoftly { connectionConfig shouldBe otherConnectionConfig }
      assertSoftly { connectionConfig.hashCode() shouldBe otherConnectionConfig.hashCode() }
    }

    should("equals. The configs differ by a parent class field") {
      val connectionConfig = buildHttpConnectionConfig()
      val otherConnectionConfig = buildHttpConnectionConfig(host = "other.com")

      assertSoftly { connectionConfig.equals(otherConnectionConfig) shouldBe false }
    }

    should("equals. The configs differ by the 'rejectUnauthorized' field") {
      val connectionConfig = buildHttpConnectionConfig()
      val otherConnectionConfig = buildHttpConnectionConfig(rejectUnauthorized = false)

      assertSoftly { connectionConfig.equals(otherConnectionConfig) shouldBe false }
      assertSoftly { (connectionConfig.hashCode() == otherConnectionConfig.hashCode()) shouldBe false }
    }

    should("equals. The configs differ by the 'isHostnameVerified' field") {
      val connectionConfig = buildHttpConnectionConfig()
      val otherConnectionConfig = buildHttpConnectionConfig(isHostnameVerified = true)

      assertSoftly { connectionConfig.equals(otherConnectionConfig) shouldBe false }
      assertSoftly { (connectionConfig.hashCode() == otherConnectionConfig.hashCode()) shouldBe false }
    }

    should("equals. The configs differ by the 'basePath' field") {
      val connectionConfig = buildHttpConnectionConfig()
      val otherConnectionConfig = buildHttpConnectionConfig(basePath = "/other/base/path")

      assertSoftly { connectionConfig.equals(otherConnectionConfig) shouldBe false }
      assertSoftly { (connectionConfig.hashCode() == otherConnectionConfig.hashCode()) shouldBe false }
    }

    should("the default constructor provides the default field values") {
      val connectionConfig = HttpConnectionConfig()

      assertSoftly { connectionConfig.basePath shouldBe "/" }
      assertSoftly { connectionConfig.rejectUnauthorized shouldBe true }
      assertSoftly { connectionConfig.isHostnameVerified shouldBe false }
    }
  }
})