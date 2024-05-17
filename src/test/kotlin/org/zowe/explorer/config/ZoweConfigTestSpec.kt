/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.ui.zosmf.ConnectionDialogState
import org.zowe.explorer.config.connect.whoAmI
import org.zowe.explorer.config.ws.FilesWorkingSetConfig
import org.zowe.explorer.config.ws.JesWorkingSetConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.Operation
import org.zowe.explorer.dataops.operations.InfoOperation
import org.zowe.explorer.dataops.operations.ZOSInfoOperation
import org.zowe.explorer.explorer.Explorer
import org.zowe.explorer.explorer.WorkingSet
import org.zowe.explorer.testutils.WithApplicationShouldSpec
import org.zowe.explorer.testutils.testServiceImpl.TestDataOpsManagerImpl
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.utils.crudable.getAll
import org.zowe.explorer.utils.service
import org.zowe.explorer.zowe.ZOWE_CONFIG_NAME
import org.zowe.explorer.zowe.service.ZoweConfigServiceImpl
import org.zowe.kotlinsdk.*
import org.zowe.kotlinsdk.annotations.ZVersion
import org.zowe.kotlinsdk.zowe.config.KeytarWrapper
import org.zowe.kotlinsdk.zowe.config.ZoweConfig
import org.zowe.kotlinsdk.zowe.config.parseConfigJson
import java.io.InputStream
import java.nio.file.*
import java.util.*
import java.util.stream.Stream
import kotlin.reflect.KFunction

class ZoweConfigTestSpec : WithApplicationShouldSpec({
  val tmpZoweConfFile = "test/$ZOWE_CONFIG_NAME"
  val connectionDialogState = ConnectionDialogState(
    connectionName = "a",
    connectionUrl = "https://111.111.111.111:555",
    username = "testUser",
    password = "testPass",
    isAllowSsl = true
  )

  afterSpec {
    clearAllMocks()
    unmockkAll()
  }

  context("config module: zowe config file") {
    var isFilesWriteTriggered = false
    var isRunWriteActionCalled = false
    var isSaveNewSecurePropertiesCalled = false
    var isFindFileByNioPathCalled = false
    var isInputStreamCalled = false
    var isReturnedZoweConfig = false
    var isAddOrUpdateConnectionCalled = false
    var isZOSInfoCalled = false
    var isScanForZoweConfigCalled = false
    var isConnectionDeleted = false

    val mockedProject = mockk<Project>(relaxed = true)
    every { mockedProject.basePath } returns "test"
    every { mockedProject.name } returns "testProj"

    val connectionId = "000000000000"
    val connection = ConnectionConfig(
      "ID$connectionId",
      connectionId,
      "URL$connectionId",
      true,
      ZVersion.ZOS_2_4,
      zoweConfigPath = "/zowe/config/path"
    )
    val crudableMockk = mockk<Crudable>()
    every { crudableMockk.getAll<ConnectionConfig>() } returns Stream.of()
    every { crudableMockk.getAll<FilesWorkingSetConfig>() } returns Stream.of()
    every { crudableMockk.getAll<JesWorkingSetConfig>() } returns Stream.of()
    mockkObject(ConfigService)
    every { ConfigService.instance.crudable } returns crudableMockk
    every { crudableMockk.find<ConnectionConfig>(any(), any()) } answers {
      listOf(connection).stream()
    }
    every { crudableMockk.delete(any()) } answers {
      isConnectionDeleted = true
      Optional.of(ConnectionConfig())
    }
    every { crudableMockk.addOrUpdate(any<ConnectionConfig>()) } answers {
      isAddOrUpdateConnectionCalled = true
      Optional.of(ConnectionConfig())
    }

    val mockedZoweConfigService = spyk(ZoweConfigServiceImpl(mockedProject), recordPrivateCalls = true)
    every { mockedZoweConfigService["createZoweSchemaJsonIfNotExists"]() } returns Unit

    val mockedZoweConfigInputStream = mockk<InputStream>()
    every { mockedZoweConfigInputStream.readAllBytes() } returns "<HOST>:<PORT>;SSL".toByteArray()
    every { mockedZoweConfigInputStream.close() } returns Unit

    mockkObject(ZoweConfigServiceImpl, recordPrivateCalls = true)
    every { ZoweConfigServiceImpl["getResourceStream"](any<String>()) } returns mockedZoweConfigInputStream

    val mockedRunWriteAction: KFunction<Unit> = ::runWriteAction
    mockkStatic(mockedRunWriteAction)
    every {
      mockedRunWriteAction.call(any<() -> Unit>())
    } answers {
      isRunWriteActionCalled = true
      firstArg<() -> Unit>().invoke()
    }

    mockkObject(ZoweConfig)
    every {
      ZoweConfig.saveNewSecureProperties(any<String>(), any<MutableMap<String, Any?>>(), any())
    } answers {
      isSaveNewSecurePropertiesCalled = true
    }

    val confMap = mutableMapOf<String, MutableMap<String, String>>()
    val configCredentialsMap = mutableMapOf<String, String>()
    configCredentialsMap["profiles.base.properties.user"] = "testUser"
    configCredentialsMap["profiles.base.properties.password"] = "testPass"
    every { ZoweConfig.Companion["readZoweCredentialsFromStorage"](any<KeytarWrapper>()) } returns confMap

    val zoweConnConf = connectionDialogState.connectionConfig
    zoweConnConf.zoweConfigPath = tmpZoweConfFile.replace("\\", "/")
    zoweConnConf.name = "zowe-testProj"

    afterEach {
      isFilesWriteTriggered = false
      isRunWriteActionCalled = false
      isSaveNewSecurePropertiesCalled = false

      isFindFileByNioPathCalled = false
      isInputStreamCalled = false
      isReturnedZoweConfig = false
      isAddOrUpdateConnectionCalled = false
      isZOSInfoCalled = false
      isScanForZoweConfigCalled = false

      isConnectionDeleted = false
    }

    val vfMock = mockk<VirtualFile>()
    val isMock = mockk<InputStream>()
    val vfmMock: VirtualFileManager = mockk<VirtualFileManager>()
    mockkStatic(VirtualFileManager::class)
    every { VirtualFileManager.getInstance() } returns vfmMock
    every { vfmMock.findFileByNioPath(any<Path>()) } answers {
      isFindFileByNioPathCalled = true
      vfMock
    }
    every { vfMock.path } returns "/zowe/file/path"
    every { vfMock.inputStream } answers {
      isInputStreamCalled = true
      isMock
    }
    every { isMock.close() } just Runs
    val zoweConfigMock = mockk<ZoweConfig>()
    every { zoweConfigMock.extractSecureProperties(any<Array<String>>(), any<KeytarWrapper>()) } answers {
      isScanForZoweConfigCalled = true
      Unit
    }
    every { zoweConfigMock.user } returns "ZoweUserName"
    every { zoweConfigMock.password } returns "ZoweUserPass"
    every { zoweConfigMock.basePath } returns "/base/config/path"
    every { zoweConfigMock.port } returns 555
    every { zoweConfigMock.host } returns "111.111.111.111"
    every { zoweConfigMock.protocol } returns "https"
    every { zoweConfigMock.rejectUnauthorized } returns false

    val parseConfigJsonFun: (InputStream) -> ZoweConfig = ::parseConfigJson
    mockkStatic(parseConfigJsonFun as KFunction<*>)
    every { parseConfigJson(any<InputStream>()) } answers {
      isReturnedZoweConfig = true
      zoweConfigMock
    }

    val explorerMock = mockk<Explorer<ConnectionConfig, WorkingSet<ConnectionConfig, *>>>()
    every { explorerMock.componentManager } returns ApplicationManager.getApplication()
    val dataOpsManagerService = ApplicationManager.getApplication().service<DataOpsManager>() as TestDataOpsManagerImpl
    dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl(explorerMock.componentManager) {
      override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
        if (operation is InfoOperation) {
          @Suppress("UNCHECKED_CAST")
          return SystemsResponse(numRows = 1) as R
        }
        if (operation is ZOSInfoOperation) {
          isZOSInfoCalled = true
          @Suppress("UNCHECKED_CAST")
          return InfoResponse(zosVersion = "04.27.00") as R
        }
        @Suppress("UNCHECKED_CAST")
        return InfoResponse() as R
      }
    }

    mockkStatic(::whoAmI as KFunction<*>)
    every { whoAmI(any<ConnectionConfig>()) } returns "USERID"

    should("add zowe team config file") {
      mockkStatic(Files::class) {
        every { Files.write(any<Path>(), any<ByteArray>()) } answers {
          isFilesWriteTriggered = true
          Path.of("")
        }
        mockedZoweConfigService.addZoweConfigFile(connectionDialogState)
      }
      isFilesWriteTriggered shouldBe true
      isRunWriteActionCalled shouldBe true
      isSaveNewSecurePropertiesCalled shouldBe true
    }

    should("add or update zowe team config connection") {
      mockedZoweConfigService.addOrUpdateZoweConfig(scanProject = true, checkConnection = true)
      isFindFileByNioPathCalled shouldBe true
      isInputStreamCalled shouldBe true
      isReturnedZoweConfig shouldBe true
      isScanForZoweConfigCalled shouldBe true
      isAddOrUpdateConnectionCalled shouldBe true
      isZOSInfoCalled shouldBe true
    }

    should("delete zowe team config connection") {
      mockedZoweConfigService.deleteZoweConfig()
      isConnectionDeleted shouldBe true
    }

  }

})
