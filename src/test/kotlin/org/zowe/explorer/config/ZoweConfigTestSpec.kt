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

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.ui.zosmf.ConnectionDialogState
import org.zowe.explorer.testutils.WithApplicationShouldSpec
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.utils.crudable.getAll
import org.zowe.explorer.zowe.ZOWE_CONFIG_NAME
import org.zowe.explorer.zowe.service.ZoweConfigServiceImpl
import org.zowe.kotlinsdk.zowe.config.KeytarWrapper
import org.zowe.kotlinsdk.zowe.config.ZoweConfig
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
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

    val mockedProject = mockk<Project>(relaxed = true)
    every { mockedProject.basePath } returns "test"
    every { mockedProject.name } returns "testProj"

    val crudableMockk = mockk<Crudable>()
    every { crudableMockk.getAll<ConnectionConfig>() } returns Stream.of()
    mockkObject(ConfigService)
    every { ConfigService.instance.crudable } returns crudableMockk

    val filesWrite: (Path, ByteArray, Array<out OpenOption>) -> Path = Files::write
    mockkStatic(filesWrite as KFunction<*>)
    every {
      filesWrite(any<Path>(), any<ByteArray>(), anyVararg())
    } answers {
      isFilesWriteTriggered = true
      Path.of("")
    }

    val mockedZoweConfig = spyk(ZoweConfigServiceImpl(mockedProject), recordPrivateCalls = true)
    every { mockedZoweConfig["createZoweSchemaJsonIfNotExists"]() } returns Unit

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
    }

    should("add zowe team config file") {
      mockedZoweConfig.addZoweConfigFile(connectionDialogState)

      isFilesWriteTriggered shouldBe true
      isRunWriteActionCalled shouldBe true
      isSaveNewSecurePropertiesCalled shouldBe true
    }

//    should("get zowe team config state") {
//
//      val run1 = zoweConfigService.getZoweConfigState(false)
//      run1 shouldBeEqual ZoweConfigState.NOT_EXISTS
//
//      val run2 = zoweConfigService.getZoweConfigState()
//      run2 shouldBeEqual ZoweConfigState.NEED_TO_ADD
//
//      crudableInst.addOrUpdate(zoweConnConf)
//
//      val run3 = zoweConfigService.getZoweConfigState()
//      run3 shouldBeEqual ZoweConfigState.NEED_TO_UPDATE
//
////      confMap[winTmpZoweConfFile]?.set("profiles.base.properties.password", "testPassword")
//
//      val run4 = zoweConfigService.getZoweConfigState()
//      run4 shouldBeEqual ZoweConfigState.SYNCHRONIZED
//
//    }
//    val zoweConfig = zoweConfigService.zoweConfig
//    val host = zoweConfig?.host
//    val port = zoweConfig?.port
//
//    should("add or update zowe team config connection") {
//
//      zoweConnConf.url = "222.222.222.222:666"
//      crudableInst.addOrUpdate(zoweConnConf)
//      crudableInst.getAll<ConnectionConfig>().toList()
//        .filter { it.name == zoweConnConf.name }[0].url shouldBeEqual zoweConnConf.url
//
//      zoweConfigService.addOrUpdateZoweConfig(scanProject = true, checkConnection = false)
//
//      crudableInst.getAll<ConnectionConfig>().toList()
//        .filter { it.name == zoweConnConf.name }[0].url shouldBeEqual "https://$host:$port"
//    }
//
//    should("delete zowe team config connection") {
//      var isDeleteMessageInDialogCalled = false
//      val showDialogSpecificMock: (
//        Project?, String, String, Array<String>, Int, Icon?
//      ) -> Int = Messages::showDialog
//      mockkStatic(showDialogSpecificMock as KFunction<*>)
//      every {
//        showDialogSpecificMock(any(), any<String>(), any<String>(), any<Array<String>>(), any<Int>(), any())
//      } answers {
//        isDeleteMessageInDialogCalled = true
//        1
//      }
//
////      crudableInst.getAll<ConnectionConfig>().toList()
////        .filter { it.name == zoweConnConf.name }[0].url shouldBeEqual "https://$host:$port"
//
//      zoweConfigService.deleteZoweConfig()
//
////      crudableInst.getAll<ConnectionConfig>().toList().filter { it.name == zoweConnConf.name }.size shouldBeEqual 0
//
//    }

  }

})
