/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.config.connect

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.zowe.explorer.testutils.MockkAwareShouldSpec
import org.zowe.kotlinsdk.annotations.ZVersion

class ConnectionConfigTestSpec : MockkAwareShouldSpec({
  context("config/connect module: ConnectionConfig") {
    fun buildConnectionConfig(
      name: String = "test_name",
      url: String = "https://test.com:1234",
      isAllowSelfSigned: Boolean = true,
      zVersion: ZVersion = ZVersion.ZOS_2_4,
      zoweConfigPath: String? = null,
      owner: String = "TSTUSR",
      isHostnameVerified: Boolean = false
    ) = ConnectionConfig(
      "test_uuid", name, url, isAllowSelfSigned, zVersion, zoweConfigPath, owner, isHostnameVerified
    )

    should("equals. The configs are the same instance") {
      val connectionConfig = buildConnectionConfig()

      assertSoftly { connectionConfig.equals(connectionConfig) shouldBe true }
    }

    should("equals. The configs are of different classes") {
      val connectionConfig = buildConnectionConfig()

      assertSoftly { connectionConfig.equals("not_a_connection_config") shouldBe false }
      assertSoftly { connectionConfig.equals(null) shouldBe false }
    }

    should("equals. The configs are fully equal") {
      val connectionConfig = buildConnectionConfig()
      val otherConnectionConfig = buildConnectionConfig()

      assertSoftly { connectionConfig shouldBe otherConnectionConfig }
      assertSoftly { connectionConfig.hashCode() shouldBe otherConnectionConfig.hashCode() }
    }

    should("equals. The configs differ by the 'name' field") {
      val connectionConfig = buildConnectionConfig()
      val otherConnectionConfig = buildConnectionConfig(name = "other_name")

      assertSoftly { connectionConfig.equals(otherConnectionConfig) shouldBe false }
    }

    should("equals. The configs differ by the 'url' field") {
      val connectionConfig = buildConnectionConfig()
      val otherConnectionConfig = buildConnectionConfig(url = "https://other.com:1234")

      assertSoftly { connectionConfig.equals(otherConnectionConfig) shouldBe false }
    }

    should("equals. The configs differ by the 'isAllowSelfSigned' field") {
      val connectionConfig = buildConnectionConfig()
      val otherConnectionConfig = buildConnectionConfig(isAllowSelfSigned = false)

      assertSoftly { connectionConfig.equals(otherConnectionConfig) shouldBe false }
    }

    should("equals. The configs differ by the 'isHostnameVerified' field") {
      val connectionConfig = buildConnectionConfig()
      val otherConnectionConfig = buildConnectionConfig(isHostnameVerified = true)

      assertSoftly { connectionConfig.equals(otherConnectionConfig) shouldBe false }
      assertSoftly { (connectionConfig.hashCode() == otherConnectionConfig.hashCode()) shouldBe false }
    }

    should("equals. The configs differ by the 'zoweConfigPath' field") {
      val connectionConfig = buildConnectionConfig()
      val otherConnectionConfig = buildConnectionConfig(zoweConfigPath = "test/path/zowe.config.json")

      assertSoftly { connectionConfig.equals(otherConnectionConfig) shouldBe false }
    }

    should("equals. The configs differ by the 'zVersion' field") {
      val connectionConfig = buildConnectionConfig()
      val otherConnectionConfig = buildConnectionConfig(zVersion = ZVersion.ZOS_2_5)

      assertSoftly { connectionConfig.equals(otherConnectionConfig) shouldBe false }
    }

    should("equals. The configs differ by the 'owner' field") {
      val connectionConfig = buildConnectionConfig()
      val otherConnectionConfig = buildConnectionConfig(owner = "OTHUSR")

      assertSoftly { connectionConfig.equals(otherConnectionConfig) shouldBe false }
    }

    should("toString. The 'isHostnameVerified' field is represented in the string") {
      val connectionConfig = buildConnectionConfig(isHostnameVerified = true)

      assertSoftly { connectionConfig.toString() shouldContain "isHostnameVerified=true" }
    }

    should("the default constructor provides the default field values") {
      val connectionConfig = ConnectionConfig()

      assertSoftly { connectionConfig.name shouldBe "" }
      assertSoftly { connectionConfig.url shouldBe "" }
      assertSoftly { connectionConfig.isAllowSelfSigned shouldBe true }
      assertSoftly { connectionConfig.isHostnameVerified shouldBe false }
      assertSoftly { connectionConfig.zVersion shouldBe ZVersion.ZOS_2_1 }
      assertSoftly { connectionConfig.zoweConfigPath shouldBe null }
      assertSoftly { connectionConfig.owner shouldBe "" }
    }
  }
})