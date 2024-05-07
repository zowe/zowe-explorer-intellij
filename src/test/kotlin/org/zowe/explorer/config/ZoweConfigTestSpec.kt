package org.zowe.explorer.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
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
import java.net.URI
import java.net.URL
import java.nio.file.*
import javax.swing.Icon
import kotlin.reflect.KFunction

class ZoweConfigTestSpec : WithApplicationShouldSpec({
  val baseProjPath = Path.of(System.getProperty("java.io.tmpdir")).toRealPath().toString().replace("\\", "/")
  val tmpZoweConfFile = "$baseProjPath/$ZOWE_CONFIG_NAME"
  val winTmpZoweConfFile = tmpZoweConfFile.split("/").toTypedArray().joinToString(File.separator)
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

    val mockedProject = mockk<Project>(relaxed = true)
    every { mockedProject.basePath } returns baseProjPath
    every { mockedProject.name } returns "testProj"

    val copyURLToFileMock: (
      URL?, File?
    ) -> Unit = FileUtils::copyURLToFile
    mockkStatic(copyURLToFileMock as KFunction<*>)
    every {
      copyURLToFileMock(any() as URL, any() as File)
    } just Runs

    val fs = mockk<FileSystem>(relaxed = true)
    val newFileSystemMock: (
      URI?, Map<String, *>
    ) -> FileSystem? = FileSystems::newFileSystem
    mockkStatic(newFileSystemMock as KFunction<*>)
    every {
      newFileSystemMock(any() as URI, any() as Map<String, *>)
    } returns fs
    every {
      fs.getPath(any() as String)
    } answers {
      (Paths.get(firstArg<String>()))
    }
    every {
      fs.close()
    } just Runs

    val pathsGet: (
      String, Array<out String>
    ) -> Path = Paths::get
    mockkStatic(pathsGet as KFunction<*>)
    every {
      pathsGet(match { it.startsWith("/") && System.getProperty("os.name").contains("Windows") }, arrayOf<String>())
    } answers {
      if (System.getProperty("os.name")
          .contains("Windows") && firstArg<String>().startsWith("/")
      ) Path.of(URI("file:" + firstArg<String>()))
      else Path.of(URI(firstArg<String>()))
    }

    mockkObject(ZoweConfigServiceImpl(mockedProject)) {
      every { ZoweConfigService.getInstance(mockedProject) } returns ZoweConfigServiceImpl(mockedProject)
    }

    mockkObject(ZoweConfigServiceImpl)
    every { ZoweConfigServiceImpl.Companion["getResourceUrl"](any() as String) } answers {
      val p = ZoweConfigTestSpec::class.java.classLoader?.getResource(firstArg<String>())?.path
      if (firstArg<String>().contains(ZOWE_CONFIG_NAME)) {
        URL("jar:file:/path/to/jar!" + p.toString())
      } else {
        URL("file:" + p)
      }

    }

    var checkFilePath = false
    var checkUser = false
    var checkPass = false
    fun checkSaveNewSecProp(
      filePath: String, configCredentialsMap: MutableMap<String, Any?>
    ) {
      if (filePath == tmpZoweConfFile) checkFilePath = true
      configCredentialsMap.forEach {
        if (it.key == "profiles.base.properties.user" && it.value == "testUser") checkUser = true
        if (it.key == "profiles.base.properties.password" && it.value == "testPass") checkPass = true
      }
    }
    mockkObject(ZoweConfig)
    every {
      ZoweConfig.saveNewSecureProperties(
        any() as String, any() as MutableMap<String, Any?>, any()
      )
    } answers {
      checkSaveNewSecProp(firstArg<String>(), secondArg<MutableMap<String, Any?>>())
    }

    val confMap = mutableMapOf<String, MutableMap<String, String>>()
    val configCredentialsMap = mutableMapOf<String, String>()
    configCredentialsMap["profiles.base.properties.user"] = "testUser"
    configCredentialsMap["profiles.base.properties.password"] = "testPass"
    confMap[winTmpZoweConfFile] = configCredentialsMap

    every { ZoweConfig.Companion["readZoweCredentialsFromStorage"](any() as KeytarWrapper) } returns confMap

    val zoweConnConf = connectionDialogState.connectionConfig
    zoweConnConf.zoweConfigPath = tmpZoweConfFile.replace("\\", "/")
    zoweConnConf.name = "zowe-testProj"

    val zoweConfigService = ZoweConfigService.getInstance(mockedProject)
    val crudableInst = ConfigService.instance.crudable

    var isDeleteMessageInDialogCalled = false
    val showDialogSpecificMock: (
      Project?, String, String, Array<String>, Int, Icon?
    ) -> Int = Messages::showDialog
    mockkStatic(showDialogSpecificMock as KFunction<*>)
    every {
      showDialogSpecificMock(any(), any<String>(), any<String>(), any<Array<String>>(), any<Int>(), any())
    } answers {
      isDeleteMessageInDialogCalled = true
      1
    }

    should("add zowe team config file") {

      Files.deleteIfExists(Paths.get(tmpZoweConfFile))

      var isPortAdded = false
      var isHostAdded = false
      var isSslAdded = false

      ApplicationManager.getApplication().invokeAndWait {
        zoweConfigService.addZoweConfigFile(connectionDialogState)
        VirtualFileManager.getInstance().syncRefresh()
      }

      val read = Files.readAllLines(Paths.get(tmpZoweConfFile))
      for (listItem in read) {
        if (listItem.contains("\"port\": 555")) isPortAdded = true
        if (listItem.contains("\"host\": \"111.111.111.111\"")) isHostAdded = true
        if (listItem.contains("\"rejectUnauthorized\": false")) isSslAdded = true
      }

      isPortAdded shouldBe true
      isHostAdded shouldBe true
      isSslAdded shouldBe true
      checkFilePath shouldBe true
      checkUser shouldBe true
      checkPass shouldBe true

    }

    should("get zowe team config state") {

      val run1 = zoweConfigService.getZoweConfigState(false)
      run1 shouldBeEqual ZoweConfigState.NOT_EXISTS

      val run2 = zoweConfigService.getZoweConfigState()
      run2 shouldBeEqual ZoweConfigState.NEED_TO_ADD

      crudableInst.addOrUpdate(zoweConnConf)

      val run3 = zoweConfigService.getZoweConfigState()
      run3 shouldBeEqual ZoweConfigState.NEED_TO_UPDATE

      confMap[winTmpZoweConfFile]?.set("profiles.base.properties.password", "testPassword")

      val run4 = zoweConfigService.getZoweConfigState()
      run4 shouldBeEqual ZoweConfigState.SYNCHRONIZED

    }
    val zoweConfig = zoweConfigService.zoweConfig
    val host = zoweConfig?.host
    val port = zoweConfig?.port

    should("add or update zowe team config connection") {

      zoweConnConf.url = "222.222.222.222:666"
      crudableInst.addOrUpdate(zoweConnConf)
      crudableInst.getAll<ConnectionConfig>().toList()
        .filter { it.name == zoweConnConf.name }[0].url shouldBeEqual zoweConnConf.url

      zoweConfigService.addOrUpdateZoweConfig(scanProject = true, checkConnection = false)

      crudableInst.getAll<ConnectionConfig>().toList()
        .filter { it.name == zoweConnConf.name }[0].url shouldBeEqual "https://$host:$port"
    }

    should("delete zowe team config connection") {

      crudableInst.getAll<ConnectionConfig>().toList()
        .filter { it.name == zoweConnConf.name }[0].url shouldBeEqual "https://$host:$port"

      zoweConfigService.deleteZoweConfig()

      crudableInst.getAll<ConnectionConfig>().toList().filter { it.name == zoweConnConf.name }.size shouldBeEqual 0

    }

    should("delete zowe team config file") {

      zoweConnConf.name = "zowe-zowe-explorer"
      crudableInst.addOrUpdate(zoweConnConf)

      crudableInst.getAll<ConnectionConfig>().toList()
        .filter { it.name == zoweConnConf.name }[0].url shouldBeEqual zoweConnConf.url

      ApplicationManager.getApplication().invokeAndWait {
        Files.deleteIfExists(Paths.get(tmpZoweConfFile))
        VirtualFileManager.getInstance().syncRefresh()
      }
      var t = 0
      while (!isDeleteMessageInDialogCalled && t < 5000) {
        Thread.sleep(100)
        t += 100
      }

      File(tmpZoweConfFile).exists() shouldBe false
      isDeleteMessageInDialogCalled shouldBe true
      crudableInst.getAll<ConnectionConfig>().toList()[0].name shouldBeEqual "zowe-zowe-explorer1"

    }

  }

})

