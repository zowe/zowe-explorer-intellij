/*
 * Copyright (c) 2025 IBA Group.
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
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.v3.state.config.migration

import com.intellij.openapi.application.Application
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.FilesWorkingSetConfig
import org.zowe.explorer.config.ws.JesWorkingSetConfig
import org.zowe.explorer.config.ws.JobsFilter
import org.zowe.explorer.tso.config.TSOSessionConfig
import org.zowe.explorer.v3.state.config.Config
import org.zowe.explorer.v3.state.config.ConfigType
import org.zowe.explorer.v3.state.config.cache.ConfigCacheService
import org.zowe.explorer.v3.state.settings.OtherSettingsHolder
import org.zowe.explorer.v3.state.settings.OtherSettingsService
import org.zowe.explorer.v3.state.storage.StableStorage
import org.zowe.explorer.v3.state.storage.StorageService
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.jdom.Attribute
import org.jdom.Element
import org.zowe.explorer.testutils.AppInitShouldSpec
import java.util.stream.Stream

@OptIn(StableStorage::class)
class UtilsTestSpec : AppInitShouldSpec("v3/state/config/migration/utils", {
  context("all functions") {
    context("doesXmlHasConfigs") {
      should("return true if the structure contains at least one config") {
        val savedConfigsRoot = Element("test_root")

        val configTypeStr = "TestConfig"
        val configsListElem = Element("test_configs_list_tag")
        configsListElem.attributes = listOf(Attribute("type", configTypeStr))
        val wrongConfigsListElem1 = Element("test_configs_list_tag")
        wrongConfigsListElem1.attributes = listOf(
          Attribute("type", "OtherTestConfig"),
            Attribute("not_type", "some_other_attribute")
        )
        val wrongConfigsListElem2 = Element("test_configs_list_tag")
        wrongConfigsListElem2.attributes = listOf(Attribute("not_type", "some_attr"))

        val listTagElem = Element("test_list_tag")

        val configElem = Element("test_config_elem")

        savedConfigsRoot.addContent(listOf(configsListElem, wrongConfigsListElem1, wrongConfigsListElem2))
        configsListElem.addContent(listTagElem)
        listTagElem.addContent(configElem)

        val result = doesXmlHasConfigs(savedConfigsRoot, configTypeStr)

        assertSoftly { result shouldBe true }
      }

      should("return false if the structure does not contain any configs") {
        val savedConfigsRoot = Element("test_root")

        val configTypeStr = "TestConfig"
        val configsList = Element("test_configs_list_tag")
        configsList.attributes = listOf(Attribute("type", configTypeStr))

        val listTagElem = Element("test_list_tag")

        savedConfigsRoot.addContent(configsList)
        configsList.addContent(listTagElem)

        val result = doesXmlHasConfigs(savedConfigsRoot, configTypeStr)

        assertSoftly { result shouldBe false }
      }

      should("return false if the structure does not contain the necessary list tag") {
        val savedConfigsRoot = Element("test_root")

        val configTypeStr = "TestConfig"
        val configsList = Element("test_configs_list_tag")
        configsList.attributes = listOf(Attribute("type", configTypeStr))

        savedConfigsRoot.addContent(configsList)

        val result = doesXmlHasConfigs(savedConfigsRoot, configTypeStr)

        assertSoftly { result shouldBe false }
      }

      should("return false if the structure is incorrect") {
        val savedConfigsRoot = Element("test_root")

        val configTypeStr = "TestConfig"
        val configsList = Element("test_configs_list_tag")

        val listTagElem = Element("test_list_tag")

        savedConfigsRoot.addContent(configsList)
        configsList.addContent(listTagElem)

        val result = doesXmlHasConfigs(savedConfigsRoot, configTypeStr)

        assertSoftly { result shouldBe false }
      }

      should("return false if there is no saved structures") {
        val configTypeStr = "TestConfig"

        val result = doesXmlHasConfigs(null, configTypeStr)

        assertSoftly { result shouldBe false }
      }
    }

    context("xmlToConfigs") {
      var didCallDeserializer = false

      beforeEach {
        didCallDeserializer = false
      }

      val deserializedConfig = object : Config(configType = ConfigType.FILES_WORKING_SET_CONFIG_V1) { }

      val deserializerFun: (Element) -> Config = {
        didCallDeserializer = true
        deserializedConfig
      }

      should("convert the provided XML structure to a configs list") {
        val savedConfigsRoot = Element("test_root")

        val configTypeStr = ConfigType.FILES_WORKING_SET_CONFIG_V1.toString()
        val configsList = Element("test_configs_list_tag")
        configsList.attributes = listOf(Attribute("type", configTypeStr))

        val listTagElem = Element("test_list_tag")

        val configElem = Element("test_config_elem")

        savedConfigsRoot.addContent(configsList)
        configsList.addContent(listTagElem)
        listTagElem.addContent(configElem)

        val result = xmlToConfigs(savedConfigsRoot, configTypeStr, deserializerFun)

        assertSoftly { didCallDeserializer shouldBe true }
        assertSoftly { result shouldBe mutableListOf(deserializedConfig) }
      }

      should("return empty list cause the structure does not have configs") {
        val savedConfigsRoot = Element("test_root")

        val configTypeStr = "TestConfig"
        val configsList = Element("test_configs_list_tag")

        val listTagElem = Element("test_list_tag")

        savedConfigsRoot.addContent(configsList)
        configsList.addContent(listTagElem)

        val result = xmlToConfigs(savedConfigsRoot, configTypeStr, deserializerFun)

        assertSoftly { didCallDeserializer shouldBe false }
        assertSoftly { result shouldBe mutableListOf() }
      }
    }

    context("performCascadeMigration") {
      should("perform cascade migration of the old working set config version to the files working set config") {
        val savedConfigsRoot = Element("test_root")

        val configTypeStr = "WORKING_SET_CONFIG_V1"
        val configsList = Element("test_configs_list_tag")
        configsList.attributes = listOf(Attribute("type", configTypeStr))

        val listTagElem = Element("test_list_tag")

        val configElem = Element("test_config_elem")
        configElem.attributes = listOf(Attribute("uuid", "test_uuid"))

        savedConfigsRoot.addContent(configsList)
        configsList.addContent(listTagElem)
        listTagElem.addContent(configElem)

        val result = performCascadeMigration(
          mutableMapOf(
            ConfigType.FILES_WORKING_SET_CONFIG_V1 to mutableListOf(),
            ConfigType.HTTP_CONNECTION_CONFIG_V1 to mutableListOf()
          ),
          savedConfigsRoot
        )

        assertSoftly { result[ConfigType.FILES_WORKING_SET_CONFIG_V1]?.size shouldBe 1 }
        assertSoftly { result[ConfigType.FILES_WORKING_SET_CONFIG_V1]?.get(0)?.uuid shouldBe "test_uuid" }
      }
    }

    context("performOldConfigStorageMigration") {
      var didCallUpdateOtherSettings = false
      var didCallSaveOtherSettings = false
      var addConfigToCacheCallCount = 0
      var updateConfigInCacheCallCount = 0
      var saveCacheToStorageCallCount = 0

      // Needed or companion object initialization
      StorageService.Companion
      val storageServiceMock = mockk<StorageService>()
      val otherSettingsServiceMock = mockk<OtherSettingsService>()
      val configCacheServiceMock = mockk<ConfigCacheService>()
      val configServiceMock = mockk<ConfigService>()

      mockkConstructor(Application::class)
      mockkObject(StorageService)
      mockkObject(OtherSettingsService)
      mockkObject(ConfigCacheService)
      mockkObject(ConfigService)

      beforeEach {
        didCallUpdateOtherSettings = false
        didCallSaveOtherSettings = false
        addConfigToCacheCallCount = 0
        updateConfigInCacheCallCount = 0
        saveCacheToStorageCallCount = 0

        every { otherSettingsServiceMock.getOtherSettings() } returns mockk {
          every { batchSize } returns 0
          every { batchSize = any() } just Runs
          every { isAutoSyncEnabled } returns true
          every { isAutoSyncEnabled = any() } just Runs
        }
        every {
          otherSettingsServiceMock.updateOtherSettings(any<OtherSettingsHolder>())
        } answers {
          didCallUpdateOtherSettings = true
        }
        every {
          otherSettingsServiceMock.saveOtherSettings()
        } answers {
          didCallSaveOtherSettings = true
        }
        every { configCacheServiceMock.getConfigsFromCache(any<ConfigType>()) } answers { Stream.empty() }
        every {
          configCacheServiceMock.updateConfigInCache(any<Config>())
        } answers {
          updateConfigInCacheCallCount += 1
        }
        every {
          configCacheServiceMock.addConfigToCache(any<Config>())
        } answers {
          addConfigToCacheCallCount += 1
        }
        every {
          configCacheServiceMock.saveCacheToStorage(any<ConfigType>())
        } answers {
          saveCacheToStorageCallCount += 1
        }
        every { configServiceMock.batchSize } returns 0
        every { configServiceMock.isAutoSyncEnabled } returns true
        every { configServiceMock.crudable } returns mockk {
          every { getAll(any<Class<*>>()) } answers { Stream.empty() }
        }

        every { anyConstructed<Application>().getService(StorageService::class.java) } returns storageServiceMock
        every { anyConstructed<Application>().getService(OtherSettingsService::class.java) } returns otherSettingsServiceMock
        every { anyConstructed<Application>().getService(ConfigCacheService::class.java) } returns configCacheServiceMock
        every { anyConstructed<Application>().getService(ConfigService::class.java) } returns configServiceMock

        every { StorageService.getService() } returns storageServiceMock
        every { OtherSettingsService.getService() } returns otherSettingsServiceMock
        every { ConfigCacheService.getService() } returns configCacheServiceMock
        every { ConfigService.getService() } returns configServiceMock
      }

      should("not perform any migrations as there are no configs and the settings are the same") {
        performOldConfigStorageMigration()

        assertSoftly { didCallUpdateOtherSettings shouldBe false }
        assertSoftly { didCallSaveOtherSettings shouldBe false }
        assertSoftly { addConfigToCacheCallCount shouldBe 0 }
        assertSoftly { updateConfigInCacheCallCount shouldBe 0 }
        assertSoftly { saveCacheToStorageCallCount shouldBe 0 }
      }

      should("perform settings update when the batch size is changed") {
        val getOtherSettingsMock = otherSettingsServiceMock.getOtherSettings()
        every { getOtherSettingsMock.batchSize } returns 1

        performOldConfigStorageMigration()

        assertSoftly { didCallUpdateOtherSettings shouldBe true }
        assertSoftly { didCallSaveOtherSettings shouldBe true }
        assertSoftly { addConfigToCacheCallCount shouldBe 0 }
        assertSoftly { updateConfigInCacheCallCount shouldBe 0 }
        assertSoftly { saveCacheToStorageCallCount shouldBe 0 }
      }

      should("perform settings update when the isAutoSyncEnabled is changed") {
        val getOtherSettingsMock = otherSettingsServiceMock.getOtherSettings()
        every { getOtherSettingsMock.isAutoSyncEnabled } returns false

        performOldConfigStorageMigration()

        assertSoftly { didCallUpdateOtherSettings shouldBe true }
        assertSoftly { didCallSaveOtherSettings shouldBe true }
        assertSoftly { addConfigToCacheCallCount shouldBe 0 }
        assertSoftly { updateConfigInCacheCallCount shouldBe 0 }
        assertSoftly { saveCacheToStorageCallCount shouldBe 0 }
      }

      should("perform configs migration only") {
        every { configServiceMock.crudable } returns mockk {
          every {
            getAll(any<Class<*>>())
          } answers {
            val theClass = firstArg<Class<*>>()
            val connectionConfig = ConnectionConfig()
            connectionConfig.url = "https://test.com"
            val jesWorkingSetConfig = JesWorkingSetConfig()
            jesWorkingSetConfig.jobsFilters = mutableListOf(JobsFilter("TSTOWNR", "TSTPFX", "TSTJID"))
            when (theClass) {
              ConnectionConfig::class.java -> Stream.of(connectionConfig)
              FilesWorkingSetConfig::class.java -> Stream.of(FilesWorkingSetConfig())
              JesWorkingSetConfig::class.java -> Stream.of(jesWorkingSetConfig)
              TSOSessionConfig::class.java -> Stream.of(TSOSessionConfig())
              else -> Stream.empty()
            }
          }
        }

        performOldConfigStorageMigration()

        assertSoftly { didCallUpdateOtherSettings shouldBe false }
        assertSoftly { didCallSaveOtherSettings shouldBe false }
        assertSoftly { addConfigToCacheCallCount shouldBe 4 }
        assertSoftly { updateConfigInCacheCallCount shouldBe 0 }
        assertSoftly { saveCacheToStorageCallCount shouldBe 4 }
      }

      should("perform configs migration, ignoring already migrated configs") {
        val connectionConfigUuid = "test_uuid_1"
        val filesWorkingSetConfigUuid1 = "test_uuid_2"
        val filesWorkingSetConfigUuid2 = "test_uuid_3"
        val jesWorkingSetConfigUuid = "test_uuid_4"

        every { configServiceMock.crudable } returns mockk {
          every {
            getAll(any<Class<*>>())
          } answers {
            val theClass = firstArg<Class<*>>()
            val connectionConfig = ConnectionConfig()
            connectionConfig.uuid = connectionConfigUuid
            connectionConfig.url = "http://test.com"
            val filesWorkingSetConfig = FilesWorkingSetConfig()
            filesWorkingSetConfig.uuid = filesWorkingSetConfigUuid1
            val otherFilesWorkingSetConfig = FilesWorkingSetConfig()
            otherFilesWorkingSetConfig.uuid = filesWorkingSetConfigUuid2
            val jesWorkingSetConfig = JesWorkingSetConfig()
            jesWorkingSetConfig.uuid = jesWorkingSetConfigUuid
            jesWorkingSetConfig.jobsFilters = mutableListOf(JobsFilter("TSTOWNR", "TSTPFX", "TSTJID"))
            when (theClass) {
              ConnectionConfig::class.java -> Stream.of(connectionConfig)
              FilesWorkingSetConfig::class.java -> Stream.of(filesWorkingSetConfig, otherFilesWorkingSetConfig)
              JesWorkingSetConfig::class.java -> Stream.of(jesWorkingSetConfig)
              else -> Stream.empty()
            }
          }
        }
        every {
          configCacheServiceMock.getConfigsFromCache(any<ConfigType>())
        } answers {
          val configType = firstArg<ConfigType>()
          when (configType) {
            ConfigType.FILES_WORKING_SET_CONFIG_V1 ->
              Stream.of(
                org.zowe.explorer.v3.state.config.files.FilesWorkingSetConfig(uuid = filesWorkingSetConfigUuid1, name = "test_name"),
                org.zowe.explorer.v3.state.config.files.FilesWorkingSetConfig(uuid = filesWorkingSetConfigUuid2)
              )
            else -> Stream.empty()
          }
        }

        performOldConfigStorageMigration()

        assertSoftly { didCallUpdateOtherSettings shouldBe false }
        assertSoftly { didCallSaveOtherSettings shouldBe false }
        assertSoftly { addConfigToCacheCallCount shouldBe 2 }
        assertSoftly { updateConfigInCacheCallCount shouldBe 1 }
        assertSoftly { saveCacheToStorageCallCount shouldBe 3 }
      }
    }
  }
})
