package org.zowe.explorer.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.apache.commons.io.FileUtils
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.ui.zosmf.ConnectionDialogState
import org.zowe.explorer.testutils.WithApplicationShouldSpec
import org.zowe.explorer.utils.crudable.getAll
import org.zowe.explorer.zowe.ZOWE_CONFIG_NAME
import org.zowe.explorer.zowe.service.ZoweConfigService
import org.zowe.explorer.zowe.service.ZoweConfigServiceImpl
import org.zowe.explorer.zowe.service.ZoweConfigState
import org.zowe.kotlinsdk.zowe.config.KeytarWrapper
import org.zowe.kotlinsdk.zowe.config.ZoweConfig
import java.io.File
import java.net.URL
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.reflect.KFunction

class ZoweConfigTestSpec : WithApplicationShouldSpec({

  val baseProjPath = System.getProperty("java.io.tmpdir")
  val tmpZoweConfFile = baseProjPath + File.separator + ZOWE_CONFIG_NAME
  val connectionDialogState = ConnectionDialogState(
    connectionName = "a",
    connectionUrl = "https://111.111.111.111:555",
    username = "testUser",
    password = "testPass",
    isAllowSsl = true
  )

  afterSpec {
    clearAllMocks()
  }

  context("config module: zowe config file") {

    val mockedProject = mockk<Project>()
    every { mockedProject.basePath } returns baseProjPath
    every { mockedProject.name } returns "testProj"

    val copyURLToFileMock: (
      URL?, File?
    ) -> Unit = FileUtils::copyURLToFile
    mockkStatic(copyURLToFileMock as KFunction<*>)
    every {
      copyURLToFileMock(any() as URL, any() as File)
    } just Runs

    mockkObject(ZoweConfigServiceImpl(mockedProject)) {
      every { ZoweConfigService.getInstance(mockedProject) } returns ZoweConfigServiceImpl(mockedProject)
    }

    mockkObject(ZoweConfigServiceImpl)
    every { ZoweConfigServiceImpl.Companion["getResourceContent"](any() as String, any() as Charset) } answers {
      String(
        Files.readAllBytes(
          Paths.get(
            this::class.java.classLoader?.getResource(firstArg<String>())?.path.toString()
          )
        ), secondArg<Charset>()
      )
    }

    var checkFilePath = false
    var checkUser = false
    var checkPass = false
    fun checkSaveNewSecProp(
      filePath: String,
      configCredentialsMap: MutableMap<String, Any?>
    ) {
      if (filePath == tmpZoweConfFile)
        checkFilePath = true
      configCredentialsMap.forEach {
        if (it.key == "profiles.base.properties.user" && it.value == "testUser")
          checkUser = true
        if (it.key == "profiles.base.properties.password" && it.value == "testPass")
          checkPass = true
      }
    }
    mockkObject(ZoweConfig)
    every {
      ZoweConfig.saveNewSecureProperties(
        any() as String,
        any() as MutableMap<String, Any?>, any()
      )
    } answers {
      checkSaveNewSecProp(firstArg<String>(), secondArg<MutableMap<String, Any?>>())
    }

    val confMap = mutableMapOf<String, MutableMap<String, String>>()
    val configCredentialsMap = mutableMapOf<String, String>()
    configCredentialsMap["profiles.base.properties.user"] = "testUser"
    configCredentialsMap["profiles.base.properties.password"] = "testPass"
    confMap[tmpZoweConfFile] = configCredentialsMap

    every { ZoweConfig.Companion["readZoweCredentialsFromStorage"](any() as KeytarWrapper) } returns confMap

    val zoweConnConf = connectionDialogState.connectionConfig
    zoweConnConf.zoweConfigPath = tmpZoweConfFile
    zoweConnConf.name = "zowe-testProj"


    should("add zowe team config file") {

      Files.deleteIfExists(Paths.get(tmpZoweConfFile))

      var isPortAdded = false
      var isHostAdded = false
      var isSslAdded = false

      ApplicationManager.getApplication().invokeAndWait {
        ZoweConfigService.getInstance(mockedProject).addZoweConfigFile(connectionDialogState)
        VirtualFileManager.getInstance().syncRefresh()
      }

      val read = Files.readAllLines(Paths.get(tmpZoweConfFile))
      for (listItem in read) {
        if (listItem.contains("\"port\": 555"))
          isPortAdded = true
        if (listItem.contains("\"host\": \"111.111.111.111\""))
          isHostAdded = true
        if (listItem.contains("\"rejectUnauthorized\": false"))
          isSslAdded = true
      }

      isPortAdded shouldBe true
      isHostAdded shouldBe true
      isSslAdded shouldBe true
      checkFilePath shouldBe true
      checkUser shouldBe true
      checkPass shouldBe true

    }

    should("get zowe team config state") {

      val run1 = ZoweConfigService.getInstance(mockedProject).getZoweConfigState(false)
      run1 shouldBeEqual ZoweConfigState.NOT_EXISTS

      val run2 = ZoweConfigService.getInstance(mockedProject).getZoweConfigState()
      run2 shouldBeEqual ZoweConfigState.NEED_TO_ADD

      ConfigService.instance.crudable.addOrUpdate(zoweConnConf)

      val run3 = ZoweConfigService.getInstance(mockedProject).getZoweConfigState()
      run3 shouldBeEqual ZoweConfigState.NEED_TO_UPDATE

      confMap[tmpZoweConfFile]?.set("profiles.base.properties.password", "testPassword")

      val run4 = ZoweConfigService.getInstance(mockedProject).getZoweConfigState()
      run4 shouldBeEqual ZoweConfigState.SYNCHRONIZED

    }

    should("add or update zowe team config connection") {

      zoweConnConf.url = "222.222.222.222:666"
      ConfigService.instance.crudable.addOrUpdate(zoweConnConf)
      ConfigService.instance.crudable.getAll<ConnectionConfig>().toList()
        .filter { it.name == zoweConnConf.name }[0].url shouldBeEqual zoweConnConf.url

      ZoweConfigService.getInstance(mockedProject).addOrUpdateZoweConfig(scanProject = true, checkConnection = false)

      ConfigService.instance.crudable.getAll<ConnectionConfig>().toList()
        .filter { it.name == zoweConnConf.name }[0].url shouldBeEqual "https://${
        ZoweConfigService.getInstance(
          mockedProject
        ).zoweConfig?.host
      }:${ZoweConfigService.getInstance(mockedProject).zoweConfig?.port}"

    }

    should("delete zowe team config connection") {

      Files.deleteIfExists(Paths.get(tmpZoweConfFile))

      ConfigService.instance.crudable.getAll<ConnectionConfig>().toList()
        .filter { it.name == zoweConnConf.name }[0].url shouldBeEqual "https://${
        ZoweConfigService.getInstance(
          mockedProject
        ).zoweConfig?.host
      }:${ZoweConfigService.getInstance(mockedProject).zoweConfig?.port}"

      ZoweConfigService.getInstance(mockedProject).deleteZoweConfig()

      ConfigService.instance.crudable.getAll<ConnectionConfig>().toList()
        .filter { it.name == zoweConnConf.name }.size shouldBeEqual 0

    }

  }

})

