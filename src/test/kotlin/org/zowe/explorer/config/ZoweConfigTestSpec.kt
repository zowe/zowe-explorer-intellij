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

import com.google.gson.Gson
import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.Messages.showOkCancelDialog
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.Credentials
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
import org.zowe.explorer.utils.crudable.*
import org.zowe.explorer.utils.runIfTrue
import org.zowe.explorer.utils.service
import org.zowe.explorer.utils.validateForBlank
import org.zowe.explorer.zowe.ZOWE_CONFIG_NAME
import org.zowe.explorer.zowe.service.ZoweConfigServiceImpl
import org.zowe.explorer.zowe.service.ZoweConfigServiceImpl.Companion.getZoweConfigLocation
import org.zowe.explorer.zowe.service.ZoweConfigServiceImpl.Companion.getZoweConnectionName
import org.zowe.explorer.zowe.service.ZoweConfigState
import org.zowe.explorer.zowe.service.ZoweConfigType
import org.zowe.kotlinsdk.InfoResponse
import org.zowe.kotlinsdk.SystemsResponse
import org.zowe.kotlinsdk.annotations.ZVersion
import org.zowe.kotlinsdk.zowe.client.sdk.core.ZOSConnection
import org.zowe.kotlinsdk.zowe.config.KeytarWrapper
import org.zowe.kotlinsdk.zowe.config.ZoweConfig
import org.zowe.kotlinsdk.zowe.config.encodeToBase64
import org.zowe.kotlinsdk.zowe.config.parseConfigJson
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.stream.Stream
import javax.swing.Icon
import javax.swing.JPasswordField
import javax.swing.JTextField
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField

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
    var notified = false

    val mockedProject = mockk<Project>(relaxed = true)
    every { mockedProject.basePath } returns "test"
    every { mockedProject.name } returns "testProj"

    val connectionId = "000000000000"
    val connection = ConnectionConfig(
      "ID$connectionId", connectionId, "URL$connectionId", true, ZVersion.ZOS_2_4, zoweConfigPath = "/zowe/config/path"
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
      notified = false
    }

    val mockedNotification: (
      Notification
    ) -> Unit = Notifications.Bus::notify
    mockkStatic(mockedNotification as KFunction<*>)
    every {
      mockedNotification(
        any<Notification>()
      )
    } answers {
      notified = true
    }

    should("getResourceStream") {
      ZoweConfigServiceImpl.Companion::class.declaredMemberFunctions.find { it.name == "getResourceStream" }?.let {
        it.isAccessible = true
        it.call(ZoweConfigServiceImpl, "test") shouldBe null
      }
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
    every { zoweConfigMock.rejectUnauthorized } returns null
    val zossConn1 = ZOSConnection(
      "111.111.111.111",
      "10443",
      "testUser",
      "testPassword",
      rejectUnauthorized = true,
      basePath = "/base/config/path/",
      profileName = "zosmf"
    )
    val zosConnList1 = mutableListOf<ZOSConnection>(zossConn1)
    every { zoweConfigMock.getListOfZosmfConections() } returns zosConnList1

    val parseConfigJsonFun: (InputStream) -> ZoweConfig = ::parseConfigJson
    mockkStatic(parseConfigJsonFun as KFunction<*>)
    every { parseConfigJson(any<InputStream>()) } answers {
      isReturnedZoweConfig = true
      zoweConfigMock
    }

    val explorerMock = mockk<Explorer<ConnectionConfig, WorkingSet<ConnectionConfig, *>>>()
    every { explorerMock.componentManager } returns ApplicationManager.getApplication()
    var infoRes = InfoResponse(zosVersion = "04.27.00")
    val dataOpsManagerService = ApplicationManager.getApplication().service<DataOpsManager>() as TestDataOpsManagerImpl
    dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl() {
      override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
        if (operation is InfoOperation) {
          if (infoRes.zosVersion == "throw1") {
            throw Throwable("Test performInfoOperation throw")
          }
          @Suppress("UNCHECKED_CAST") return SystemsResponse(numRows = 1) as R
        }
        if (operation is ZOSInfoOperation) {
          isZOSInfoCalled = true
          if (infoRes.zosVersion == "throw2") {
            throw Throwable("Test performOperation throw")
          }
          @Suppress("UNCHECKED_CAST") return infoRes as R
        }
        @Suppress("UNCHECKED_CAST") return InfoResponse() as R
      }
    }

    mockkStatic(::whoAmI as KFunction<*>)
    every { whoAmI(any<ConnectionConfig>()) } returns "USERID"

    should("getZoweConnectionName") {
      getZoweConnectionName(mockedProject, ZoweConfigType.LOCAL) shouldBe "zowe-local-zosmf/testProj"
      getZoweConnectionName(mockedProject, ZoweConfigType.GLOBAL) shouldBe "zowe-global-zosmf"
      getZoweConnectionName(null, ZoweConfigType.GLOBAL) shouldBe "zowe-global-zosmf"
      getZoweConnectionName(null, ZoweConfigType.LOCAL) shouldBe "zowe-local-zosmf/null"
    }

    should("getZoweConfigLocation") {
      getZoweConfigLocation(mockedProject, ZoweConfigType.LOCAL) shouldBe "test/zowe.config.json"
      getZoweConfigLocation(
        mockedProject, ZoweConfigType.GLOBAL
      ) shouldBe System.getProperty("user.home").replace("((\\*)|(/*))$", "") + "/.zowe/" + ZOWE_CONFIG_NAME
      getZoweConfigLocation(null, ZoweConfigType.LOCAL) shouldBe "null/zowe.config.json"
    }

    should("notifyError") {
      mockedZoweConfigService::class.declaredMemberFunctions.find { it.name == "notifyError" }?.let {
        it.isAccessible = true
        it.call(mockedZoweConfigService, Throwable("Test throwable"), "test title")
        it.call(mockedZoweConfigService, Throwable("Test throwable"), null)
        it.call(mockedZoweConfigService, Throwable(), null)
        notified shouldBe true
      }
    }

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

    should("add zowe team config connection") {
      mockedZoweConfigService.addOrUpdateZoweConfig(
        scanProject = true,
        checkConnection = true,
        type = ZoweConfigType.LOCAL
      )
      isFindFileByNioPathCalled shouldBe true
      isInputStreamCalled shouldBe true
      isReturnedZoweConfig shouldBe true
      isScanForZoweConfigCalled shouldBe true
      isAddOrUpdateConnectionCalled shouldBe true
      isZOSInfoCalled shouldBe true
    }

    should("try to update zowe team config connection and throw") {
      mockedZoweConfigService.addOrUpdateZoweConfig(
        scanProject = true, checkConnection = false, type = ZoweConfigType.LOCAL
      )
      isFindFileByNioPathCalled shouldBe true
      isInputStreamCalled shouldBe true
      isReturnedZoweConfig shouldBe true
      isScanForZoweConfigCalled shouldBe true
      isAddOrUpdateConnectionCalled shouldBe true
      isZOSInfoCalled shouldBe false
    }

    should("delete zowe team config connection") {
      mockedZoweConfigService.deleteZoweConfig(type = ZoweConfigType.LOCAL)
      isConnectionDeleted shouldBe true
    }

    should("delete with FilesWorkingSet and JesWorkingSet with throw") {
      clearMocks(crudableMockk)
      connection.name = getZoweConnectionName(mockedProject, ZoweConfigType.LOCAL)
      every { crudableMockk.getAll<ConnectionConfig>() } answers {
        listOf(connection).stream()
      }
      every { crudableMockk.find<ConnectionConfig>(any(), any()) } answers {
        listOf(connection).stream()
      }
      var f = false
      var j = false
      val fWSConf = FilesWorkingSetConfig()
      fWSConf.connectionConfigUuid = connection.uuid
      every { crudableMockk.getAll<FilesWorkingSetConfig>() } answers {
        f = true
        listOf(fWSConf).stream()
      }
      val jWSConf = JesWorkingSetConfig()
      jWSConf.connectionConfigUuid = connection.uuid
      every { crudableMockk.getAll<JesWorkingSetConfig>() } answers {
        j = true
        listOf(jWSConf).stream()
      }
      var isShowOkCancelDialogCalled = false
      val showOkCancelDialogMock: (String, String, String, String, Icon?) -> Int = ::showOkCancelDialog
      mockkStatic(showOkCancelDialogMock as KFunction<*>)
      every {
        showOkCancelDialogMock(any<String>(), any<String>(), any<String>(), any<String>(), any())
      } answers {
        isShowOkCancelDialogCalled = true
        Messages.OK
      }
      mockedZoweConfigService.deleteZoweConfig(type = ZoweConfigType.LOCAL)

      f shouldBe true
      j shouldBe true
      isShowOkCancelDialogCalled shouldBe true
      isConnectionDeleted shouldBe false
    }

    should("checkAndRemoveOldZoweConnection") {
      var isupdateConnectionCalled = false
      every { crudableMockk.update(any<ConnectionConfig>()) } answers {
        isupdateConnectionCalled = true
        Optional.of(ConnectionConfig())
      }
      connection.zoweConfigPath = getZoweConfigLocation(mockedProject, ZoweConfigType.LOCAL)
      mockedZoweConfigService.checkAndRemoveOldZoweConnection(ZoweConfigType.LOCAL)
      isupdateConnectionCalled shouldBe true
    }

    clearMocks(crudableMockk)
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

    should("testAndPrepareConnection") {
      mockedZoweConfigService::class.declaredMemberFunctions.find { it.name == "testAndPrepareConnection" }?.let {
        it.isAccessible = true
        infoRes = InfoResponse(zosVersion = "04.25.00")
        it.call(mockedZoweConfigService, connection)
        connection.zVersion shouldBe ZVersion.ZOS_2_2
        infoRes = InfoResponse(zosVersion = "04.26.00")
        it.call(mockedZoweConfigService, connection)
        connection.zVersion shouldBe ZVersion.ZOS_2_3
        infoRes = InfoResponse(zosVersion = "04.28.00")
        it.call(mockedZoweConfigService, connection)
        connection.zVersion shouldBe ZVersion.ZOS_2_5
        infoRes = InfoResponse(zosVersion = "00.00.00")
        every { whoAmI(any<ConnectionConfig>()) } returns null
        it.call(mockedZoweConfigService, connection)
        connection.zVersion shouldBe ZVersion.ZOS_2_1
        connection.owner shouldBe ""
        infoRes = InfoResponse(zosVersion = "throw1")
        try {
          it.call(mockedZoweConfigService, connection)
        } catch (e: Throwable) {
          e.cause.toString() shouldContain "Test performInfoOperation throw"
        }
        infoRes = InfoResponse(zosVersion = "throw2")
        try {
          it.call(mockedZoweConfigService, connection)
        } catch (e: Throwable) {
          e.cause.toString() shouldContain "Test performOperation throw"
        }
      }
    }

    should("addOrUpdateZoweConfig throw Cannot get Zowe config") {
      mockedZoweConfigService.addOrUpdateZoweConfig(
        scanProject = false, checkConnection = true, type = ZoweConfigType.GLOBAL
      )
      notified shouldBe true
    }

    should("addOrUpdateZoweConfig throw Cannot get password") {
      every { zoweConfigMock.password } returns null
      mockedZoweConfigService.addOrUpdateZoweConfig(
        scanProject = false, checkConnection = true, type = ZoweConfigType.LOCAL
      )
      every { zoweConfigMock.password } returns "password"
      notified shouldBe true
    }

    should("addOrUpdateZoweConfig throw Cannot get username") {
      every { zoweConfigMock.user } returns null
      mockedZoweConfigService.addOrUpdateZoweConfig(
        scanProject = false, checkConnection = true, type = ZoweConfigType.LOCAL
      )
      every { zoweConfigMock.user } returns "ZoweUserName"
      notified shouldBe true
    }

    should("getOrCreateUuid") {
      mockedZoweConfigService::class.declaredMemberFunctions.find { it.name == "getOrCreateUuid" }?.let {
        it.isAccessible = true
        it.call(mockedZoweConfigService, ZoweConfigType.LOCAL, "zosmf") shouldBe "ID000000000000"
        every { crudableMockk.find<ConnectionConfig>(any(), any()) } answers {
          emptyList<ConnectionConfig>().stream()
        }
        val randomUUID = it.call(mockedZoweConfigService, ZoweConfigType.GLOBAL, "zosmf")
        randomUUID shouldNotBe null
        randomUUID shouldNotBe "ID000000000000"
      }
    }

    should("addOrUpdateZoweConfig New ConnectionConfig throw on check") {
      mockedZoweConfigService.addOrUpdateZoweConfig(
        scanProject = false, checkConnection = true, type = ZoweConfigType.LOCAL
      )
      notified shouldBe true
    }

    should("scanForZoweConfig global throw") {
      mockedZoweConfigService::class.declaredMemberFunctions.find { it.name == "scanForZoweConfig" }?.let {
        it.isAccessible = true
        try {
          it.call(mockedZoweConfigService, ZoweConfigType.GLOBAL)
        } catch (e: Exception) {
          e.cause.toString() shouldContain "Cannot parse ${ZoweConfigType.GLOBAL} Zowe config file"
        }
      }
    }

    should("scanForZoweConfig throw") {
      clearMocks(zoweConfigMock)
      every { zoweConfigMock.extractSecureProperties(any<Array<String>>(), any<KeytarWrapper>()) } answers {
        throw Exception("Test exception")
      }
      mockedZoweConfigService::class.declaredMemberFunctions.find { it.name == "scanForZoweConfig" }?.let {
        it.isAccessible = true
        try {
          it.call(mockedZoweConfigService, ZoweConfigType.LOCAL)
        } catch (e: Exception) {
          e.cause.toString() shouldContain "Cannot parse ${ZoweConfigType.LOCAL} Zowe config file"
        }
      }
    }

    should("getZoweConfigState throw") {
      mockedZoweConfigService.getZoweConfigState(true, ZoweConfigType.LOCAL)
      notified shouldBe true
    }

    should("getZoweConfigState") {
      val zosConnList = mutableListOf<ZOSConnection>()
      for (type in ZoweConfigType.entries) {
        connection.name = getZoweConnectionName(mockedProject, type)//"zowe-$type-zosmf/untitled"
        mockedZoweConfigService.globalZoweConfig = null
        mockedZoweConfigService.localZoweConfig = null
        mockedZoweConfigService.getZoweConfigState(false, type) shouldBe ZoweConfigState.NOT_EXISTS
        mockedZoweConfigService.globalZoweConfig = zoweConfigMock
        mockedZoweConfigService.localZoweConfig = zoweConfigMock
        clearMocks(crudableMockk)
        every { crudableMockk.find<ConnectionConfig>(any(), any()) } answers {
          emptyList<ConnectionConfig>().stream()
        }
        mockedZoweConfigService.getZoweConfigState(false, type) shouldBe ZoweConfigState.NEED_TO_ADD
        every { zoweConfigMock.user } returns "testUser"
        every { zoweConfigMock.password } returns "testPassword"
        every { zoweConfigMock.basePath } returns "/base/config/path/"
        every { zoweConfigMock.port } returns 10443
        every { zoweConfigMock.host } returns "111.111.111.111"
        every { zoweConfigMock.protocol } returns "https"
        every { zoweConfigMock.rejectUnauthorized } returns true
        val conn = ZOSConnection(
          "111.111.111.111",
          "10443",
          "testUser",
          "testPassword",
          rejectUnauthorized = true,
          basePath = "/base/config/path/",
          profileName = "zosmf"
        )
        zosConnList.clear()
        zosConnList.add(conn)
        every { zoweConfigMock.getListOfZosmfConections() } returns zosConnList
        clearMocks(crudableMockk)
        every { crudableMockk.find<ConnectionConfig>(any(), any()) } answers {
          listOf(connection).stream()
        }
        mockedZoweConfigService.getZoweConfigState(false, type) shouldBe ZoweConfigState.NEED_TO_UPDATE
        connection.name = getZoweConnectionName(mockedProject, type)
        connection.url = "https://111.111.111.111:10443/base/config/path"
        connection.isAllowSelfSigned = false
        connection.zVersion = ZVersion.ZOS_2_1
        connection.zoweConfigPath = getZoweConfigLocation(mockedProject, type)
        connection.owner = ""
        mockedZoweConfigService.getZoweConfigState(false, type) shouldBe ZoweConfigState.SYNCHRONIZED
        every { zoweConfigMock.password } returns "wrongPass"
        zosConnList.clear()
        zosConnList.add(
          ZOSConnection(
            "111.111.111.111",
            "10443",
            "testUser",
            "wrongPass",
            rejectUnauthorized = true,
            basePath = "/base/config/path/",
            profileName = "zosmf"
          )
        )
        mockedZoweConfigService.getZoweConfigState(false, type) shouldBe ZoweConfigState.NEED_TO_UPDATE
        every { zoweConfigMock.password } returns "testPassword"
        every { zoweConfigMock.user } returns "wrongUser"
        zosConnList.clear()
        zosConnList.add(
          ZOSConnection(
            "111.111.111.111",
            "10443",
            "wrongUser",
            "testPassword",
            rejectUnauthorized = true,
            basePath = "/base/config/path/",
            profileName = "zosmf"
          )
        )
        mockedZoweConfigService.getZoweConfigState(false, type) shouldBe ZoweConfigState.NEED_TO_UPDATE
        every { zoweConfigMock.user } returns "testUser"
      }
    }

    should("addZoweConfigFile") {
      every { crudableMockk.getAll<ConnectionConfig>() } returns Stream.of()
      clearMocks(mockedZoweConfigInputStream)
      every { mockedZoweConfigInputStream.readAllBytes() } returns null
      every { mockedZoweConfigInputStream.close() } returns Unit
      try {
        mockedZoweConfigService.addZoweConfigFile(connectionDialogState)
      } catch (e: Exception) {
        e.message shouldContain "zowe.config.json is not found"
      }
    }

    should("deleteZoweConfig throw") {
      every { crudableMockk.find<ConnectionConfig>(any(), any()) } returns emptyList<ConnectionConfig>().stream()
      mockedZoweConfigService.deleteZoweConfig(type = ZoweConfigType.LOCAL)
      notified shouldBe true
    }

    fun createSinglePassword(filePath: String, user: String, password: String): String {
      val credentialsMap = mutableMapOf<String, Map<String, Any?>>(
        Pair(
          filePath, mapOf(
            Pair("profiles.base.properties.user", user),
            Pair("profiles.base.properties.password", password)
          )
        )
      )
      return Gson().toJson(credentialsMap).encodeToBase64()
    }

    fun makeCrudableWithoutListeners(
      withCredentials: Boolean,
      credentialsGetter: () -> MutableList<Credentials> = { mutableListOf() },
      stateGetter: () -> ConfigStateV2
    ): Crudable {
      val crudableLists = CrudableLists(addFilter = object : AddFilter {
        override operator fun <T : Any> invoke(clazz: Class<out T>, addingRow: T): Boolean {
          return ConfigService.instance.getConfigDeclaration(clazz).getDecider().canAdd(addingRow)
        }
      }, updateFilter = object : UpdateFilter {
        override operator fun <T : Any> invoke(clazz: Class<out T>, currentRow: T, updatingRow: T): Boolean {
          return ConfigService.instance.getConfigDeclaration(clazz).getDecider().canUpdate(currentRow, updatingRow)
        }
      }, nextUuidProvider = { UUID.randomUUID().toString() }, getListByClass = {
        if (it == Credentials::class.java) {
          withCredentials.runIfTrue {
            credentialsGetter()
          }
        } else {
          stateGetter().get(it)
        }
      })
      return ConcurrentCrudable(crudableLists, SimpleReadWriteAdapter())
    }

    should("findAllZosmfExistingConnection") {
      val configCollections: MutableMap<String, MutableList<*>> = mutableMapOf(
        Pair(ConnectionConfig::class.java.name, mutableListOf<ConnectionConfig>(connection)),
        Pair(FilesWorkingSetConfig::class.java.name, mutableListOf<ConnectionConfig>()),
        Pair(JesWorkingSetConfig::class.java.name, mutableListOf<ConnectionConfig>()),
      )
      val sandboxState = SandboxState(ConfigStateV2(configCollections))
      val crudable = org.zowe.explorer.config.makeCrudableWithoutListeners(true,
        { sandboxState.credentials }) { sandboxState.configState }

      mockedZoweConfigService::class.declaredMemberProperties.find { it.name == "configCrudable" }?.let {
        it.isAccessible = true
        it.javaField?.set(mockedZoweConfigService, crudable)
      }

      mockedZoweConfigService::class.declaredMemberFunctions.find { it.name == "findAllZosmfExistingConnection" }?.let {
        it.isAccessible = true
        (it.call(mockedZoweConfigService, ZoweConfigType.LOCAL) as List<ConnectionConfig>).size shouldBe 1
        it.call(mockedZoweConfigService, ZoweConfigType.GLOBAL) shouldBe null
        connection.name = "zowe-global-zosmf"
        it.call(mockedZoweConfigService, ZoweConfigType.GLOBAL) shouldBe null
        connection.zoweConfigPath = getZoweConfigLocation(mockedProject, ZoweConfigType.GLOBAL)
        (it.call(mockedZoweConfigService, ZoweConfigType.GLOBAL) as List<ConnectionConfig>).size shouldBe 1
      }
    }

    should("findExistingConnection") {
      mockedZoweConfigService::class.declaredMemberFunctions.find { it.name == "findExistingConnection" }?.let {
        it.isAccessible = true
        connection.name = "zowe-local-zosmf/testProj"
        connection.zoweConfigPath = getZoweConfigLocation(mockedProject, ZoweConfigType.LOCAL)
        (it.call(
          mockedZoweConfigService,
          ZoweConfigType.LOCAL,
          "zosmf"
        ) as ConnectionConfig).name shouldBe connection.name
        connection.name = "zowe-global-zosmf"
        connection.zoweConfigPath = getZoweConfigLocation(mockedProject, ZoweConfigType.LOCAL)
        it.call(mockedZoweConfigService, ZoweConfigType.LOCAL, "zosmf") shouldBe null
        it.call(mockedZoweConfigService, ZoweConfigType.GLOBAL, "zosmf") shouldBe null
      }
    }

    should("validateForBlank") {
      validateForBlank(JPasswordField())?.message shouldBe "This field must not be blank"
      validateForBlank(JTextField())?.message shouldBe "This field must not be blank"
    }

    should("getZoweConfigState NEED_TO_ADD") {
      every { mockedZoweConfigService["findAllZosmfExistingConnection"](any<ZoweConfigType>()) } returns listOf<ConnectionConfig>(
        connection
      )
      every { zoweConfigMock.getListOfZosmfConections() } returns zosConnList1
      every { mockedZoweConfigService["findExistingConnection"](any<ZoweConfigType>(), any<String>()) } returns null
      mockedZoweConfigService.getZoweConfigState(false, ZoweConfigType.LOCAL) shouldBe ZoweConfigState.NEED_TO_ADD
      val zossConn2 = ZOSConnection(
        "222.222.222.222",
        "10443",
        "testUser",
        "testPassword",
        rejectUnauthorized = true,
        basePath = "/base/config/path/",
        profileName = "zosmf1"
      )
      zosConnList1.clear()
      zosConnList1.add(zossConn2)
      connection.name = "zowe-global-lpar.zosmf"
      every {
        mockedZoweConfigService["findExistingConnection"](
          any<ZoweConfigType>(), any<String>()
        )
      } returns connection
      mockedZoweConfigService.getZoweConfigState(false, ZoweConfigType.LOCAL) shouldBe ZoweConfigState.NEED_TO_ADD
    }

  }

})