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
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import org.zowe.explorer.common.ui.DialogMode
import org.zowe.explorer.common.ui.ValidatingTableView
import org.zowe.explorer.config.ConfigStateV2
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.makeCrudableWithoutListeners
import org.zowe.explorer.testutils.WithApplicationShouldSpec
import org.zowe.kotlinsdk.annotations.ZVersion
import org.zowe.kotlinsdk.zowe.config.DefaultKeytarWrapper
import org.zowe.kotlinsdk.zowe.config.KeytarWrapper
import org.zowe.kotlinsdk.zowe.config.ZoweConfig
import java.lang.reflect.Modifier
import java.nio.file.Path
import java.util.stream.Stream
import javax.swing.Icon
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

class ZOSMFConnectionConfigurableTest : WithApplicationShouldSpec({

  val zOSMFConnectionConfigurableMock = spyk(ZOSMFConnectionConfigurable(), recordPrivateCalls = true)
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

    fun Any.mockPrivateFields(name: String, mocks: Any?): Any? {
      javaClass.declaredFields
        .filter { it.modifiers.and(Modifier.PRIVATE) > 0 || it.modifiers.and(Modifier.PROTECTED) > 0 }
        .firstOrNull { it.name == name }
        ?.also { it.isAccessible = true }
        ?.set(this, mocks)
      return this
    }

    val connectionConfig = ConnectionConfig(
      uuid = "0000",
      name = "zowe-local-zosmf/testProj",
      url = "https://url.com",
      isAllowSelfSigned = true,
      zVersion = ZVersion.ZOS_2_1,
      zoweConfigPath = "zowe/config/path",
      owner = "owner"
    )
    state.connectionUrl = "https://testhost.com"
    state.username = "testuser"
    state.password = "testpass"
    state.owner = "owner"
    state.zoweConfigPath = "zowe/config/path"
    state.mode = DialogMode.UPDATE
    var crud = spyk(makeCrudableWithoutListeners(false) { ConfigStateV2() })
    every { crud.getAll(ConnectionConfig::class.java) } returns Stream.of(connectionConfig)
    every { crud.find(ConnectionConfig::class.java, any()) } returns Stream.of(connectionConfig)
    var connTModel = ConnectionsTableModel(crud)
    zOSMFConnectionConfigurableMock.mockPrivateFields("connectionsTableModel", connTModel)
    var valTView = spyk(ValidatingTableView<ConnectionDialogState>(connTModel, Disposer.newDisposable()))
    every { valTView.selectedRow } returns 0
    every { valTView.selectedRows } returns intArrayOf(0)
    zOSMFConnectionConfigurableMock.mockPrivateFields("connectionsTable", valTView)
    every { zOSMFConnectionConfigurableMock["showAndTestConnection"](any<ConnectionDialogState>()) } returns state

    should("editConnection/removeSelectedConnections") {
      zOSMFConnectionConfigurableMock::class.declaredMemberFunctions.find { it.name == "editConnection" }
        ?.let {
          it.isAccessible = true
          it.call(zOSMFConnectionConfigurableMock)
        }
      zOSMFConnectionConfigurableMock::class.declaredMemberProperties.find { it.name == "zoweConfigStates" }
        ?.let {
          it.isAccessible = true
          (it.getter.call(zOSMFConnectionConfigurableMock) as HashMap<*, *>).size shouldBe 1
        }
      crud = spyk(makeCrudableWithoutListeners(false) { ConfigStateV2() })
      every { crud.getAll(ConnectionConfig::class.java) } returns Stream.of(connectionConfig)
      every { crud.find(ConnectionConfig::class.java, any()) } returns Stream.of(connectionConfig)
      connTModel = ConnectionsTableModel(crud)
      zOSMFConnectionConfigurableMock.mockPrivateFields("connectionsTableModel", connTModel)
      zOSMFConnectionConfigurableMock::class.declaredMemberFunctions.find { it.name == "removeSelectedConnections" }
        ?.let {
          it.isAccessible = true
          it.call(zOSMFConnectionConfigurableMock)
        }
      zOSMFConnectionConfigurableMock::class.declaredMemberProperties.find { it.name == "zoweConfigStates" }
        ?.let {
          it.isAccessible = true
          (it.getter.call(zOSMFConnectionConfigurableMock) as HashMap<*, *>).size shouldBe 0
        }
    }

    should("removeSelectedConnections null selectedRows") {
      every { valTView.selectedRows } returns null
      zOSMFConnectionConfigurableMock::class.declaredMemberFunctions.find { it.name == "removeSelectedConnections" }
        ?.let {
          it.isAccessible = true
          it.call(zOSMFConnectionConfigurableMock)
        }
      zOSMFConnectionConfigurableMock::class.declaredMemberProperties.find { it.name == "zoweConfigStates" }
        ?.let {
          it.isAccessible = true
          (it.getter.call(zOSMFConnectionConfigurableMock) as HashMap<*, *>).size shouldBe 0
        }
    }

    should("removeSelectedConnections null connectionsTable") {
      every { valTView.selectedRows } returns intArrayOf(0)
      zOSMFConnectionConfigurableMock.mockPrivateFields("connectionsTable", null)
      zOSMFConnectionConfigurableMock::class.declaredMemberFunctions.find { it.name == "removeSelectedConnections" }
        ?.let {
          it.isAccessible = true
          it.call(zOSMFConnectionConfigurableMock)
        }
      zOSMFConnectionConfigurableMock::class.declaredMemberProperties.find { it.name == "zoweConfigStates" }
        ?.let {
          it.isAccessible = true
          (it.getter.call(zOSMFConnectionConfigurableMock) as HashMap<*, *>).size shouldBe 0
        }
      zOSMFConnectionConfigurableMock.mockPrivateFields("connectionsTable", valTView)
    }

    should("removeSelectedConnections null connectionsTableModel") {
      every { valTView.selectedRows } returns intArrayOf(0)
      zOSMFConnectionConfigurableMock.mockPrivateFields("connectionsTableModel", null)
      zOSMFConnectionConfigurableMock::class.declaredMemberFunctions.find { it.name == "removeSelectedConnections" }
        ?.let {
          it.isAccessible = true
          it.call(zOSMFConnectionConfigurableMock)
        }
      zOSMFConnectionConfigurableMock::class.declaredMemberProperties.find { it.name == "zoweConfigStates" }
        ?.let {
          it.isAccessible = true
          (it.getter.call(zOSMFConnectionConfigurableMock) as HashMap<*, *>).size shouldBe 0
        }
      zOSMFConnectionConfigurableMock.mockPrivateFields("connectionsTableModel", connTModel)
    }

  }
}
)
