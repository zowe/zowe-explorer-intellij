/*
 * Copyright (c) 2020-2024 IBA Group.
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

package org.zowe.explorer.config.connect.ui.zosmf

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.showOkCancelDialog
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import org.zowe.explorer.testutils.WithApplicationShouldSpec
import org.zowe.kotlinsdk.zowe.config.DefaultKeytarWrapper
import org.zowe.kotlinsdk.zowe.config.KeytarWrapper
import org.zowe.kotlinsdk.zowe.config.ZoweConfig
import java.nio.file.Path
import javax.swing.Icon
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.jvm.isAccessible

class ZOSMFConnectionConfigurableTest : WithApplicationShouldSpec({

  val zOSMFConnectionConfigurableMock = spyk<ZOSMFConnectionConfigurable>()
  var isShowOkCancelDialogCalled = false
  var isFindFileByNioPathCalled = false
  var isInputStreamCalled = false

  afterSpec {
    clearAllMocks()
    unmockkAll()
  }

  beforeEach {
    isShowOkCancelDialogCalled = false
    isFindFileByNioPathCalled = false
    isInputStreamCalled = false
  }

  context("ZOSMFConnectionConfigurable:") {

    val state = ConnectionDialogState(
      connectionUuid = "0000",
      connectionUrl = "https://111.111.111.111:111",
      connectionName = "zowe-local-zosmf/testProj",
      zoweConfigPath = "/zowe/conf/path"
    )

    val ret = mutableListOf<Int>(Messages.OK)
    val showOkCancelDialogMock: (String, String, String, String, Icon?, DialogWrapper.DoNotAskOption?, Project?) -> Int =
      ::showOkCancelDialog
    mockkStatic(showOkCancelDialogMock as KFunction<*>)
    every {
      showOkCancelDialogMock(any<String>(), any<String>(), any<String>(), any<String>(), null, null, null)
    } answers {
      isShowOkCancelDialogCalled = true
      ret[0]
    }

    mockkConstructor(DefaultKeytarWrapper::class)
    every { anyConstructed<DefaultKeytarWrapper>().setPassword(any(), any(), any()) } just Runs
    every { anyConstructed<DefaultKeytarWrapper>().deletePassword(any(), any()) } returns true

    should("updateZoweConfigIfNeeded null state and Ok") {
      zOSMFConnectionConfigurableMock::class.declaredMemberFunctions.find { it.name == "updateZoweConfigIfNeeded" }
        ?.let {
          it.isAccessible = true
          try {
            it.call(zOSMFConnectionConfigurableMock, null)
          } catch (t: Throwable) {
            t.cause.toString().shouldContain("Zowe config file not found")
          }
        }
      isShowOkCancelDialogCalled shouldBe true
      isFindFileByNioPathCalled shouldBe false
    }

    should("updateZoweConfigIfNeeded null zoweConfigPath and Ok") {
      zOSMFConnectionConfigurableMock::class.declaredMemberFunctions.find { it.name == "updateZoweConfigIfNeeded" }
        ?.let {
          it.isAccessible = true
          try {
            state.zoweConfigPath = null
            it.call(zOSMFConnectionConfigurableMock, state)
          } catch (t: Throwable) {
            t.cause.toString().shouldContain("Zowe config file not found")
          }
        }
      isShowOkCancelDialogCalled shouldBe true
      isFindFileByNioPathCalled shouldBe false
    }

    ret[0] = Messages.CANCEL

    should("updateZoweConfigIfNeeded null state and Cancel") {
      zOSMFConnectionConfigurableMock::class.declaredMemberFunctions.find { it.name == "updateZoweConfigIfNeeded" }
        ?.let {
          it.isAccessible = true
          try {
            state.zoweConfigPath = null
            it.call(zOSMFConnectionConfigurableMock, null)
          } catch (t: Throwable) {
            t.cause.toString().shouldContain("Zowe config file not found")
          }
        }
      isShowOkCancelDialogCalled shouldBe true
      isFindFileByNioPathCalled shouldBe false
    }

    state.zoweConfigPath = "/zowe/conf/path"
    ret[0] = Messages.OK

    should("updateZoweConfigIfNeeded throw Zowe config file not found") {
      zOSMFConnectionConfigurableMock::class.declaredMemberFunctions.find { it.name == "updateZoweConfigIfNeeded" }
        ?.let {
          it.isAccessible = true
          try {
            it.call(zOSMFConnectionConfigurableMock, state)
          } catch (t: Throwable) {
            t.cause.toString().shouldContain("Zowe config file not found")
          }
        }
      isShowOkCancelDialogCalled shouldBe true
      isFindFileByNioPathCalled shouldBe false
    }

    val vfMock = mockk<VirtualFile>()
    val vfmMock: VirtualFileManager = mockk<VirtualFileManager>()
    mockkStatic(VirtualFileManager::class)
    every { VirtualFileManager.getInstance() } returns vfmMock
    every { vfmMock.findFileByNioPath(any<Path>()) } answers {
      isFindFileByNioPathCalled = true
      vfMock
    }
    every { vfMock.inputStream } answers {
      isInputStreamCalled = true
      val fileCont = "{\n" +
          "    \"\$schema\": \"./zowe.schema.json\",\n" +
          "    \"profiles\": {\n" +
          "        \"zosmf\": {\n" +
          "            \"type\": \"zosmf\",\n" +
          "            \"properties\": {\n" +
          "                \"port\": 443\n" +
          "            },\n" +
          "            \"secure\": []\n" +
          "        },\n" +
          "        \"tso\": {\n" +
          "            \"type\": \"tso\",\n" +
          "            \"properties\": {\n" +
          "                \"account\": \"\",\n" +
          "                \"codePage\": \"1047\",\n" +
          "                \"logonProcedure\": \"IZUFPROC\"\n" +
          "            },\n" +
          "            \"secure\": []\n" +
          "        },\n" +
          "        \"ssh\": {\n" +
          "            \"type\": \"ssh\",\n" +
          "            \"properties\": {\n" +
          "                \"port\": 22\n" +
          "            },\n" +
          "            \"secure\": []\n" +
          "        },\n" +
          "        \"base\": {\n" +
          "            \"type\": \"base\",\n" +
          "            \"properties\": {\n" +
          "                \"host\": \"example.host\",\n" +
          "                \"rejectUnauthorized\": true\n" +
          "            },\n" +
          "            \"secure\": [\n" +
          "                \"user\",\n" +
          "                \"password\"\n" +
          "            ]\n" +
          "        }\n" +
          "    },\n" +
          "    \"defaults\": {\n" +
          "        \"zosmf\": \"zosmf\",\n" +
          "        \"tso\": \"tso\",\n" +
          "        \"ssh\": \"ssh\",\n" +
          "        \"base\": \"base\"\n" +
          "    }\n" +
          "}"
      fileCont.toByteArray().inputStream()
    }
    every { vfMock.path } returns "/zowe/file/path/zowe.config.json"
    every { vfMock.charset } returns Charsets.UTF_8
    every { vfMock.setBinaryContent(any()) } just Runs

    mockkObject(ZoweConfig)
    val confMap = mutableMapOf<String, MutableMap<String, String>>()
    val configCredentialsMap = mutableMapOf<String, String>()
    configCredentialsMap["profiles.base.properties.user"] = "testUser"
    configCredentialsMap["profiles.base.properties.password"] = "testPass"
    confMap.clear()
    confMap["/zowe/file/path/zowe.config.json"] = configCredentialsMap
    every { ZoweConfig.Companion["readZoweCredentialsFromStorage"](any<KeytarWrapper>()) } returns confMap

    should("updateZoweConfigIfNeeded  success") {
      zOSMFConnectionConfigurableMock::class.declaredMemberFunctions.find { it.name == "updateZoweConfigIfNeeded" }
        ?.let {
          it.isAccessible = true
          state.connectionUrl = "https://testhost.com:10443"
          it.call(zOSMFConnectionConfigurableMock, state)
        }
      isShowOkCancelDialogCalled shouldBe true
      isFindFileByNioPathCalled shouldBe true
      isInputStreamCalled shouldBe true
    }

    should("updateZoweConfigIfNeeded empty port") {
      zOSMFConnectionConfigurableMock::class.declaredMemberFunctions.find { it.name == "updateZoweConfigIfNeeded" }
        ?.let {
          it.isAccessible = true
          state.isAllowSsl = true
          state.connectionUrl = "https://testhost.com"
          it.call(zOSMFConnectionConfigurableMock, state)
        }
      isShowOkCancelDialogCalled shouldBe true
      isFindFileByNioPathCalled shouldBe true
      isInputStreamCalled shouldBe true
    }

    should("updateZoweConfigIfNeeded  failed") {
      zOSMFConnectionConfigurableMock::class.declaredMemberFunctions.find { it.name == "updateZoweConfigIfNeeded" }
        ?.let {
          it.isAccessible = true
          state.connectionUrl = "https://111@@@:8080"
          try {
            it.call(zOSMFConnectionConfigurableMock, state)
          } catch (t: Throwable) {
            t.cause.toString().shouldContain("Unable to save invalid URL")
          }
        }
    }

  }
}
)