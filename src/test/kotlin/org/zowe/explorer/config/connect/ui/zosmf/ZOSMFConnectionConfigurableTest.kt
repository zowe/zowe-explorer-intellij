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
 *   Katsiaryna Tsytsenia
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.config.connect.ui.zosmf

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.showOkCancelDialog
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import org.zowe.explorer.common.ui.DialogMode
import org.zowe.explorer.common.ui.ValidatingTableView
import org.zowe.explorer.config.ConfigStateV2
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.config.makeCrudableWithoutListeners
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.testutils.*
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.kotlinsdk.annotations.ZVersion
import org.zowe.kotlinsdk.zowe.config.DefaultKeytarWrapper
import org.zowe.kotlinsdk.zowe.config.KeytarWrapper
import org.zowe.kotlinsdk.zowe.config.ZoweConfig
import java.nio.file.Path
import java.util.stream.Stream
import javax.swing.Icon
import kotlin.reflect.KFunction

class ZOSMFConnectionConfigurableTest : AppInitShouldSpec("config/connect/ui/zomsf/ZOSMFConnectionConfigurable", {
  val zOSMFConnectionConfigurableMock = spyk(ZOSMFConnectionConfigurable(), recordPrivateCalls = true)
  var isShowOkCancelDialogCalled = false
  var isFindFileByNioPathCalled = false
  var isInputStreamCalled = false
  var notified = false

  val updateZoweConfigIfNeededMethod =
    ZOSMFConnectionConfigurable::class.java.getDeclaredMethod("updateZoweConfigIfNeeded", ConnectionDialogState::class.java)
  updateZoweConfigIfNeededMethod.isAccessible = true
  val editConnectionMethod = ZOSMFConnectionConfigurable::class.java.getDeclaredMethod("editConnection")
  editConnectionMethod.isAccessible = true
  val removeSelectedConnectionsMethod =
    ZOSMFConnectionConfigurable::class.java.getDeclaredMethod("removeSelectedConnections")
  removeSelectedConnectionsMethod.isAccessible = true

  beforeEach {
    isShowOkCancelDialogCalled = false
    isFindFileByNioPathCalled = false
    isInputStreamCalled = false
    notified = false
  }

  context("updateZoweConfigIfNeeded") {
    val notificationsService = NotificationsService.getService()
    every {
      notificationsService.notifyError(any<Throwable>(), any<Project>(), any<String>(), any<String>(), any<String>())
    } answers {
      val custTitle = thirdArg<String>()
      if (custTitle == "Error with Zowe config file") {
        notified = true
      }
    }

    val showOkCancelDialogMock: (String, String, String, String, Icon?, DialogWrapper.DoNotAskOption?, Project?) -> Int =
      ::showOkCancelDialog
    mockkStatic(showOkCancelDialogMock as KFunction<*>)

    mockkConstructor(DefaultKeytarWrapper::class)
    every { anyConstructed<DefaultKeytarWrapper>().setPassword(any(), any(), any()) } just Runs
    every { anyConstructed<DefaultKeytarWrapper>().deletePassword(any(), any()) } returns true

    val vfMock = mockk<VirtualFile> {
      every { inputStream } answers {
        isInputStreamCalled = true
        val fileCont = "{\n" +
          "    \"\$schema\": \"./zowe.schema.json\",\n" +
          "    \"profiles\": {\n" +
          "        \"zosmf\": {\n" +
          "}"
        fileCont.toByteArray().inputStream()
      }
      every { path } returns "/zowe/file/path/zowe.config.json"
      every { charset } returns Charsets.UTF_8
      every { setBinaryContent(any()) } just Runs
    }
    val vfmMock: VirtualFileManager = mockk<VirtualFileManager> {
      every { findFileByNioPath(any<Path>()) } answers {
        isFindFileByNioPathCalled = true
        vfMock
      }
    }

    mockkStatic(VirtualFileManager::class)
    every { VirtualFileManager.getInstance() } returns vfmMock

    val configCredentialsMap = mutableMapOf(
      "profiles.base.properties.user" to "testUser",
      "profiles.base.properties.password" to "testPass"
    )
    val confMap = mutableMapOf("/zowe/file/path/zowe.config.json" to configCredentialsMap)

    mockkObject(ZoweConfig)
    every { ZoweConfig.Companion["readZoweCredentialsFromStorage"](any<KeytarWrapper>()) } returns confMap

    lateinit var state: ConnectionDialogState
    var dialogMessageType: Int

    beforeEach {
      state = ConnectionDialogState(
        connectionUuid = "0000",
        connectionUrl = "https://111.111.111.111:111",
        connectionName = "zowe-local-zosmf/testProj",
        zoweConfigPath = "/zowe/conf/path"
      )

      dialogMessageType = Messages.OK

      every {
        showOkCancelDialogMock(any<String>(), any<String>(), any<String>(), any<String>(), null, null, null)
      } answers {
        isShowOkCancelDialogCalled = true
        dialogMessageType
      }
    }

    should("updateZoweConfigIfNeeded null state and Ok") {
      try {
        updateZoweConfigIfNeededMethod.invoke(zOSMFConnectionConfigurableMock, null)
      } catch (t: Throwable) {
        t.cause.toString().shouldContain("Zowe config file not found")
      }

      assertSoftly {
        isShowOkCancelDialogCalled shouldBe true
        isFindFileByNioPathCalled shouldBe false
      }
    }

    should("updateZoweConfigIfNeeded null zoweConfigPath and Ok") {
      try {
        state.zoweConfigPath = null
        updateZoweConfigIfNeededMethod.invoke(zOSMFConnectionConfigurableMock, state)
      } catch (t: Throwable) {
        t.cause.toString().shouldContain("Zowe config file not found")
      }

      assertSoftly {
        isShowOkCancelDialogCalled shouldBe true
        isFindFileByNioPathCalled shouldBe false
      }
    }


    should("updateZoweConfigIfNeeded null state and Cancel") {
      dialogMessageType = Messages.CANCEL

      try {
        state.zoweConfigPath = null
        updateZoweConfigIfNeededMethod.invoke(zOSMFConnectionConfigurableMock, null)
      } catch (t: Throwable) {
        t.cause.toString().shouldContain("Zowe config file not found")
      }

      assertSoftly {
        isShowOkCancelDialogCalled shouldBe true
        isFindFileByNioPathCalled shouldBe false
      }
    }

    should("updateZoweConfigIfNeeded throw Zowe config file not found") {
      try {
        state.zoweConfigPath = null
        updateZoweConfigIfNeededMethod.invoke(zOSMFConnectionConfigurableMock, state)
      } catch (t: Throwable) {
        t.cause.toString().shouldContain("Zowe config file not found")
      }

      assertSoftly {
        isShowOkCancelDialogCalled shouldBe true
        isFindFileByNioPathCalled shouldBe false
      }
    }

    should("updateZoweConfigIfNeeded throw JsonSyntaxException") {
      updateZoweConfigIfNeededMethod.invoke(zOSMFConnectionConfigurableMock, state)

      assertSoftly { notified shouldBe true }
    }

    should("updateZoweConfigIfNeeded empty zowe config file") {
      every { vfMock.inputStream } answers {
        isInputStreamCalled = true
        "".toByteArray().inputStream()
      }

      updateZoweConfigIfNeededMethod.invoke(zOSMFConnectionConfigurableMock, state)

      assertSoftly {
        notified shouldBe true
        isShowOkCancelDialogCalled shouldBe true
        isFindFileByNioPathCalled shouldBe true
        isInputStreamCalled shouldBe true
      }
    }

    should("updateZoweConfigIfNeeded  success") {
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

      state.connectionUrl = "https://testhost.com:10443"
      updateZoweConfigIfNeededMethod.invoke(zOSMFConnectionConfigurableMock, state)

      assertSoftly {
        isShowOkCancelDialogCalled shouldBe true
        isFindFileByNioPathCalled shouldBe true
        isInputStreamCalled shouldBe true
      }
    }

    should("updateZoweConfigIfNeeded empty port") {
      state.isAllowSsl = true
      state.connectionUrl = "https://testhost.com"

      updateZoweConfigIfNeededMethod.invoke(zOSMFConnectionConfigurableMock, state)

      assertSoftly {
        isShowOkCancelDialogCalled shouldBe true
        isFindFileByNioPathCalled shouldBe true
        isInputStreamCalled shouldBe true
      }
    }

    should("updateZoweConfigIfNeeded  failed") {
      state.connectionUrl = "https://111@@@:8080"

      try {
        updateZoweConfigIfNeededMethod.invoke(zOSMFConnectionConfigurableMock, state)
      } catch (t: Throwable) {
        t.cause.toString().shouldContain("Unable to save invalid URL")
      }
    }
  }

  context("other functions") {
    val credentialService = CredentialService.getService()
    every { credentialService.getUsernameByKey(any<String>()) } returns "test"
    every { credentialService.getPasswordByKey(any<String>()) } returns "test".toCharArray()

    val connectionConfig = ConnectionConfig(
      uuid = "0000",
      name = "zowe-local-zosmf/testProj",
      url = "https://url.com",
      isAllowSelfSigned = true,
      zVersion = ZVersion.ZOS_2_1,
      zoweConfigPath = "zowe/config/path",
      owner = "owner"
    )

    val state = ConnectionDialogState(
      connectionUuid = connectionConfig.uuid,
      connectionUrl = "https://testhost.com",
      connectionName = connectionConfig.name,
      zoweConfigPath = connectionConfig.zoweConfigPath,
      username = "testuser",
      password = "testpass".toCharArray(),
      owner = connectionConfig.owner,
      mode = DialogMode.UPDATE
    )

    lateinit var crud: Crudable
    lateinit var connTModel: ConnectionsTableModel
    lateinit var valTView: ValidatingTableView<ConnectionDialogState>

    beforeEach {
      crud = spyk(makeCrudableWithoutListeners(false) { ConfigStateV2() }) {
        every { getAll(ConnectionConfig::class.java) } answers { Stream.of(connectionConfig) }
        every { find(ConnectionConfig::class.java, any()) } answers { Stream.of(connectionConfig) }
      }
      connTModel = ConnectionsTableModel(crud)
      valTView = spyk(ValidatingTableView(connTModel, Disposer.newDisposable())) {
        every { selectedRow } returns 0
        every { selectedRows } returns intArrayOf(0)
      }

      setPrivateFieldValue(zOSMFConnectionConfigurableMock, "connectionsTableModel", connTModel)
      setPrivateFieldValue(zOSMFConnectionConfigurableMock, "connectionsTable", valTView)

      every { zOSMFConnectionConfigurableMock["showAndTestConnection"](any<ConnectionDialogState>()) } returns state
    }

    should("editConnection") {
      editConnectionMethod.invoke(zOSMFConnectionConfigurableMock)

      val zoweConfigStatesStart = getPrivateFieldValue(
        zOSMFConnectionConfigurableMock,
        "zoweConfigStates"
      ) as HashMap<*, *>
      zoweConfigStatesStart.size shouldBe 1
    }

    should("removeSelectedConnections") {
      removeSelectedConnectionsMethod.invoke(zOSMFConnectionConfigurableMock)

      val zoweConfigStatesEnd = getPrivateFieldValue(
        zOSMFConnectionConfigurableMock,
        "zoweConfigStates"
      ) as HashMap<*, *>
      zoweConfigStatesEnd.size shouldBe 0
    }

    should("removeSelectedConnections null selectedRows") {
      every { valTView.selectedRows } returns null

      removeSelectedConnectionsMethod.invoke(zOSMFConnectionConfigurableMock)

      val zoweConfigStatesEnd = getPrivateFieldValue(
        zOSMFConnectionConfigurableMock,
        "zoweConfigStates"
      ) as HashMap<*, *>
      zoweConfigStatesEnd.size shouldBe 0
    }

    should("removeSelectedConnections null connectionsTable") {
      setPrivateFieldValue(zOSMFConnectionConfigurableMock, "connectionsTable", null)

      removeSelectedConnectionsMethod.invoke(zOSMFConnectionConfigurableMock)

      val zoweConfigStatesEnd = getPrivateFieldValue(
        zOSMFConnectionConfigurableMock,
        "zoweConfigStates"
      ) as HashMap<*, *>
      zoweConfigStatesEnd.size shouldBe 0
    }

    should("removeSelectedConnections null connectionsTableModel") {
      setPrivateFieldValue(zOSMFConnectionConfigurableMock, "connectionsTableModel", null)

      removeSelectedConnectionsMethod.invoke(zOSMFConnectionConfigurableMock)

      val zoweConfigStatesEnd = getPrivateFieldValue(
        zOSMFConnectionConfigurableMock,
        "zoweConfigStates"
      ) as HashMap<*, *>
      zoweConfigStatesEnd.size shouldBe 0
    }
  }
})
