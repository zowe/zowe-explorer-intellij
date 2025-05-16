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

package org.zowe.explorer.v3.state.config.cache

import com.intellij.openapi.application.Application
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.v3.state.config.Config
import org.zowe.explorer.v3.state.config.ConfigEventListener
import org.zowe.explorer.v3.state.config.ConfigType
import org.zowe.explorer.v3.state.config.files.DatasetMaskConfigItem
import org.zowe.explorer.v3.state.config.files.FilesWorkingSetConfig
import org.zowe.explorer.v3.state.storage.StableStorage
import org.zowe.explorer.v3.state.storage.StorageService
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.testutils.getPrivateFieldValue
import org.zowe.explorer.utils.sendTopic
import org.zowe.explorer.utils.subscribe
import java.util.UUID

@OptIn(StableStorage::class)
class ConfigCacheServiceTestSpec : AppInitShouldSpec("v3/state/settings/ConfigCacheService", {
  lateinit var currentTestUuid: UUID

  beforeSpec {
    currentTestUuid = AppInitShouldSpec.currentTestUuid ?: throw Exception("Test UUID must be defined before the spec run")
  }

  context("all functions") {
    var configTypeRegisterCount = 0
    var configsReloadCount = 0
    var configAddedCount = 0
    var configUpdatedCount = 0
    var configDeletedCount = 0
    var isErrorNotificationTriggered = false

    mockkObject(NotificationsService)
    every {
      NotificationsService.errorNotification(any())
    } answers {
      isErrorNotificationTriggered = true
    }

    mockkConstructor(Application::class)

    // Needed or companion object initialization
    StorageService.Companion
    val storageServiceMock = mockk<StorageService>()
    val configCacheService = ConfigCacheService()
    val state = getPrivateFieldValue(configCacheService, "state") as ConfigCacheState

    every { anyConstructed<Application>().getService(ConfigCacheService::class.java) } returns configCacheService
    every { anyConstructed<Application>().getService(StorageService::class.java) } returns storageServiceMock

    mockkObject(StorageService.Companion)
    every { StorageService.getService() } returns storageServiceMock

    subscribe(
      ConfigCacheService.TOPIC,
      object : ConfigEventListener {
        override fun registered(configType: ConfigType) {
          if (currentTestUuid == AppInitShouldSpec.currentTestUuid) {
            configTypeRegisterCount += 1
          }
        }
        override fun added(config: Config) {
          if (currentTestUuid == AppInitShouldSpec.currentTestUuid) {
            configAddedCount += 1
          }
        }
        override fun updated(oldConfig: Config, newConfig: Config) {
          if (currentTestUuid == AppInitShouldSpec.currentTestUuid) {
            configUpdatedCount += 1
          }
        }
        override fun deleted(config: Config) {
          if (currentTestUuid == AppInitShouldSpec.currentTestUuid) {
            configDeletedCount += 1
          }
        }
        override fun reloaded(configType: ConfigType, reloadedConfigs: List<Config>) {
          if (currentTestUuid == AppInitShouldSpec.currentTestUuid) {
            configsReloadCount += 1
          }
        }
      }
    )

    beforeEach {
      configTypeRegisterCount = 0
      configsReloadCount = 0
      configAddedCount = 0
      configUpdatedCount = 0
      configDeletedCount = 0

      isErrorNotificationTriggered = false

      state.configs = mutableMapOf()
    }

    context("init") {
      should("not register new config type cause it is already registered") {
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).registered(ConfigType.FILES_WORKING_SET_CONFIG_V1)
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).registered(ConfigType.FILES_WORKING_SET_CONFIG_V1)

        assertSoftly { configTypeRegisterCount shouldBe 1 }
        assertSoftly { configsReloadCount shouldBe 0 }
        assertSoftly { configAddedCount shouldBe 0 }
        assertSoftly { configUpdatedCount shouldBe 0 }
        assertSoftly { configDeletedCount shouldBe 0 }
        assertSoftly { isErrorNotificationTriggered shouldBe false }
      }

      should("reload configs by their type") {
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).registered(ConfigType.FILES_WORKING_SET_CONFIG_V1)
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).reloaded(ConfigType.FILES_WORKING_SET_CONFIG_V1, listOf())

        assertSoftly { configTypeRegisterCount shouldBe 1 }
        assertSoftly { configsReloadCount shouldBe 1 }
        assertSoftly { configAddedCount shouldBe 0 }
        assertSoftly { configUpdatedCount shouldBe 0 }
        assertSoftly { configDeletedCount shouldBe 0 }
        assertSoftly { isErrorNotificationTriggered shouldBe false }
      }

      should("not reload configs by their type cause the type is not registered yet") {
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).reloaded(ConfigType.FILES_WORKING_SET_CONFIG_V1, listOf())

        assertSoftly { configTypeRegisterCount shouldBe 0 }
        assertSoftly { configsReloadCount shouldBe 0 }
        assertSoftly { configAddedCount shouldBe 0 }
        assertSoftly { configUpdatedCount shouldBe 0 }
        assertSoftly { configDeletedCount shouldBe 0 }
        assertSoftly { isErrorNotificationTriggered shouldBe true }
      }

      should("add a new config") {
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).registered(ConfigType.FILES_WORKING_SET_CONFIG_V1)
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).added(FilesWorkingSetConfig())

        assertSoftly { configTypeRegisterCount shouldBe 1 }
        assertSoftly { configsReloadCount shouldBe 0 }
        assertSoftly { configAddedCount shouldBe 1 }
        assertSoftly { configUpdatedCount shouldBe 0 }
        assertSoftly { configDeletedCount shouldBe 0 }
        assertSoftly { isErrorNotificationTriggered shouldBe false }
      }

      should("not add a new config cause it is already there") {
        val config = FilesWorkingSetConfig()
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).registered(ConfigType.FILES_WORKING_SET_CONFIG_V1)
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).added(config)
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).added(config)

        assertSoftly { configTypeRegisterCount shouldBe 1 }
        assertSoftly { configsReloadCount shouldBe 0 }
        assertSoftly { configAddedCount shouldBe 1 }
        assertSoftly { configUpdatedCount shouldBe 0 }
        assertSoftly { configDeletedCount shouldBe 0 }
        assertSoftly { isErrorNotificationTriggered shouldBe false }
      }

      should("update a config") {
        val config = FilesWorkingSetConfig()
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).registered(ConfigType.FILES_WORKING_SET_CONFIG_V1)
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).reloaded(ConfigType.FILES_WORKING_SET_CONFIG_V1, listOf(config))
        val newConfig = FilesWorkingSetConfig(uuid = config.uuid, name = "test")
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).updated(config, newConfig)

        assertSoftly { configTypeRegisterCount shouldBe 1 }
        assertSoftly { configsReloadCount shouldBe 1 }
        assertSoftly { configAddedCount shouldBe 0 }
        assertSoftly { configUpdatedCount shouldBe 1 }
        assertSoftly { configDeletedCount shouldBe 0 }
        assertSoftly { isErrorNotificationTriggered shouldBe false }
      }

      should("not update a config cause it is the same as the stored one") {
        val config = FilesWorkingSetConfig()
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).registered(ConfigType.FILES_WORKING_SET_CONFIG_V1)
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).reloaded(ConfigType.FILES_WORKING_SET_CONFIG_V1, listOf(config))
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).updated(config, config)

        assertSoftly { configTypeRegisterCount shouldBe 1 }
        assertSoftly { configsReloadCount shouldBe 1 }
        assertSoftly { configAddedCount shouldBe 0 }
        assertSoftly { configUpdatedCount shouldBe 0 }
        assertSoftly { configDeletedCount shouldBe 0 }
        assertSoftly { isErrorNotificationTriggered shouldBe false }
      }

      should("delete a config") {
        val config = FilesWorkingSetConfig()
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).registered(ConfigType.FILES_WORKING_SET_CONFIG_V1)
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).reloaded(ConfigType.FILES_WORKING_SET_CONFIG_V1, listOf(config))
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).deleted(config)

        assertSoftly { configTypeRegisterCount shouldBe 1 }
        assertSoftly { configsReloadCount shouldBe 1 }
        assertSoftly { configAddedCount shouldBe 0 }
        assertSoftly { configUpdatedCount shouldBe 0 }
        assertSoftly { configDeletedCount shouldBe 1 }
        assertSoftly { isErrorNotificationTriggered shouldBe false }
      }

      should("not delete a config cause there is no such instance") {
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).registered(ConfigType.FILES_WORKING_SET_CONFIG_V1)
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).deleted(FilesWorkingSetConfig())

        assertSoftly { configTypeRegisterCount shouldBe 1 }
        assertSoftly { configsReloadCount shouldBe 0 }
        assertSoftly { configAddedCount shouldBe 0 }
        assertSoftly { configUpdatedCount shouldBe 0 }
        assertSoftly { configDeletedCount shouldBe 0 }
        assertSoftly { isErrorNotificationTriggered shouldBe false }
      }
    }

    context("getRegisteredConfigTypes") {
      should("return registered config types") {
        val registeredConfigTypes = listOf(ConfigType.FILES_WORKING_SET_CONFIG_V1)
        every { storageServiceMock.getRegisteredConfigTypes() } returns registeredConfigTypes
        val result = configCacheService.getRegisteredConfigTypes()
        assertSoftly { result shouldBe registeredConfigTypes }
      }
    }

    context("isCacheModified") {
      should("return true cause the cache is modified") {
        val config = FilesWorkingSetConfig()
        val configsInStorage = listOf(config as Config)
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).registered(ConfigType.FILES_WORKING_SET_CONFIG_V1)
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).reloaded(
          ConfigType.FILES_WORKING_SET_CONFIG_V1,
          listOf(FilesWorkingSetConfig(uuid = config.uuid, connectionConfigUuid = "test"))
        )

        every { storageServiceMock.getConfigsFromStorage(any<ConfigType>()) } returns configsInStorage.stream()

        val result = configCacheService.isCacheModified(ConfigType.FILES_WORKING_SET_CONFIG_V1)

        assertSoftly { result shouldBe true }
      }

      should("return false cause the cache is the same as the storage") {
        val config = FilesWorkingSetConfig()
        val configsInStorage = listOf(config as Config)
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).registered(ConfigType.FILES_WORKING_SET_CONFIG_V1)
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).reloaded(ConfigType.FILES_WORKING_SET_CONFIG_V1, configsInStorage)

        every { storageServiceMock.getConfigsFromStorage(any<ConfigType>()) } returns configsInStorage.stream()

        val result = configCacheService.isCacheModified(ConfigType.FILES_WORKING_SET_CONFIG_V1)

        assertSoftly { result shouldBe false }
      }
    }

    context("saveCacheToStorage") {
      var didDeleteConfigFromStorage = false
      var didUpdateConfigInStorage = false
      var didAddConfigToStorage = false

      beforeEach {
        didDeleteConfigFromStorage = false
        didUpdateConfigInStorage = false
        didAddConfigToStorage = false
      }

      should("save config state to storage state") {
        val configToDelete = FilesWorkingSetConfig()
        val configToAdd = FilesWorkingSetConfig()
        val configToUpdate = FilesWorkingSetConfig()

        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).registered(ConfigType.FILES_WORKING_SET_CONFIG_V1)
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).reloaded(
          ConfigType.FILES_WORKING_SET_CONFIG_V1,
          listOf(
            configToAdd,
            FilesWorkingSetConfig(uuid = configToUpdate.uuid, dsMasks = mutableListOf(DatasetMaskConfigItem("TEST")))
          )
        )

        every {
          storageServiceMock.getConfigsFromStorage(any<ConfigType>())
        } answers {
          listOf(configToUpdate as Config, configToDelete as Config).stream()
        }
        every {
          storageServiceMock.deleteConfigFromStorage(any<Config>())
        } answers {
          didDeleteConfigFromStorage = true
        }
        every {
          storageServiceMock.addConfigToStorage(any<Config>())
        } answers {
          didAddConfigToStorage = true
        }
        every {
          storageServiceMock.updateConfigInStorage(any<Config>())
        } answers {
          didUpdateConfigInStorage = true
        }

        configCacheService.saveCacheToStorage(ConfigType.FILES_WORKING_SET_CONFIG_V1)

        assertSoftly { didUpdateConfigInStorage shouldBe true }
        assertSoftly { didAddConfigToStorage shouldBe true }
        assertSoftly { didDeleteConfigFromStorage shouldBe true }
      }

      should("not save config state to storage state cause the cache state is the same as the storage state") {
        val config = FilesWorkingSetConfig()

        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).registered(ConfigType.FILES_WORKING_SET_CONFIG_V1)
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).reloaded(ConfigType.FILES_WORKING_SET_CONFIG_V1, listOf(config))

        every {
          storageServiceMock.getConfigsFromStorage(any<ConfigType>())
        } answers {
          listOf(config as Config).stream()
        }
        every {
          storageServiceMock.deleteConfigFromStorage(any<Config>())
        } answers {
          didDeleteConfigFromStorage = true
        }
        every {
          storageServiceMock.addConfigToStorage(any<Config>())
        } answers {
          didAddConfigToStorage = true
        }
        every {
          storageServiceMock.updateConfigInStorage(any<Config>())
        } answers {
          didUpdateConfigInStorage = true
        }

        configCacheService.saveCacheToStorage(ConfigType.FILES_WORKING_SET_CONFIG_V1)

        assertSoftly { didUpdateConfigInStorage shouldBe false }
        assertSoftly { didAddConfigToStorage shouldBe false }
        assertSoftly { didDeleteConfigFromStorage shouldBe false }
      }
    }

    context("getConfigFromCache") {
      should("get config from cache by it's UUID") {
        val config = FilesWorkingSetConfig()
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).registered(ConfigType.FILES_WORKING_SET_CONFIG_V1)
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).reloaded(ConfigType.FILES_WORKING_SET_CONFIG_V1, listOf(config))

        val result = configCacheService.getConfigFromCache(ConfigType.FILES_WORKING_SET_CONFIG_V1, config.uuid)

        assertSoftly { result shouldBe config }
      }

      should("not get config from cache cause there is no config for the provided UUID") {
        val config = FilesWorkingSetConfig()
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).registered(ConfigType.FILES_WORKING_SET_CONFIG_V1)
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).reloaded(ConfigType.FILES_WORKING_SET_CONFIG_V1, listOf(config))

        val result = configCacheService.getConfigFromCache(ConfigType.FILES_WORKING_SET_CONFIG_V1, "wrong_uuid")

        assertSoftly { result shouldBe null }
      }
    }

    context("addConfigToCache") {
      should("add a new config") {
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).registered(ConfigType.FILES_WORKING_SET_CONFIG_V1)
        configCacheService.addConfigToCache(FilesWorkingSetConfig())

        assertSoftly { configTypeRegisterCount shouldBe 1 }
        assertSoftly { configsReloadCount shouldBe 0 }
        assertSoftly { configAddedCount shouldBe 1 }
        assertSoftly { configUpdatedCount shouldBe 0 }
        assertSoftly { configDeletedCount shouldBe 0 }
        assertSoftly { isErrorNotificationTriggered shouldBe false }
      }

      should("not add a new config cause it is already there") {
        val config = FilesWorkingSetConfig()
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).registered(ConfigType.FILES_WORKING_SET_CONFIG_V1)
        configCacheService.addConfigToCache(config)
        configCacheService.addConfigToCache(config)

        assertSoftly { configTypeRegisterCount shouldBe 1 }
        assertSoftly { configsReloadCount shouldBe 0 }
        assertSoftly { configAddedCount shouldBe 1 }
        assertSoftly { configUpdatedCount shouldBe 0 }
        assertSoftly { configDeletedCount shouldBe 0 }
        assertSoftly { isErrorNotificationTriggered shouldBe true }
      }
    }

    context("updateConfigInCache") {
      should("update a config") {
        val config = FilesWorkingSetConfig()
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).registered(ConfigType.FILES_WORKING_SET_CONFIG_V1)
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).reloaded(ConfigType.FILES_WORKING_SET_CONFIG_V1, listOf(config))
        val newConfig = FilesWorkingSetConfig(uuid = config.uuid, name = "test")
        configCacheService.updateConfigInCache(newConfig)

        assertSoftly { configTypeRegisterCount shouldBe 1 }
        assertSoftly { configsReloadCount shouldBe 1 }
        assertSoftly { configAddedCount shouldBe 0 }
        assertSoftly { configUpdatedCount shouldBe 1 }
        assertSoftly { configDeletedCount shouldBe 0 }
        assertSoftly { isErrorNotificationTriggered shouldBe false }
      }

      should("not update a config cause it is the same as the stored one") {
        val config = FilesWorkingSetConfig()
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).registered(ConfigType.FILES_WORKING_SET_CONFIG_V1)
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).reloaded(ConfigType.FILES_WORKING_SET_CONFIG_V1, listOf(config))
        configCacheService.updateConfigInCache(config)

        assertSoftly { configTypeRegisterCount shouldBe 1 }
        assertSoftly { configsReloadCount shouldBe 1 }
        assertSoftly { configAddedCount shouldBe 0 }
        assertSoftly { configUpdatedCount shouldBe 0 }
        assertSoftly { configDeletedCount shouldBe 0 }
        assertSoftly { isErrorNotificationTriggered shouldBe true }
      }
    }

    context("deleteConfigFromCache") {
      should("delete a config") {
        val config = FilesWorkingSetConfig()
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).registered(ConfigType.FILES_WORKING_SET_CONFIG_V1)
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).reloaded(ConfigType.FILES_WORKING_SET_CONFIG_V1, listOf(config))
        configCacheService.deleteConfigFromCache(config)

        assertSoftly { configTypeRegisterCount shouldBe 1 }
        assertSoftly { configsReloadCount shouldBe 1 }
        assertSoftly { configAddedCount shouldBe 0 }
        assertSoftly { configUpdatedCount shouldBe 0 }
        assertSoftly { configDeletedCount shouldBe 1 }
        assertSoftly { isErrorNotificationTriggered shouldBe false }
      }

      should("not delete a config cause there is no such instance") {
        sendTopic(StorageService.STORAGE_CONFIGS_TOPIC).registered(ConfigType.FILES_WORKING_SET_CONFIG_V1)
        configCacheService.deleteConfigFromCache(FilesWorkingSetConfig())

        assertSoftly { configTypeRegisterCount shouldBe 1 }
        assertSoftly { configsReloadCount shouldBe 0 }
        assertSoftly { configAddedCount shouldBe 0 }
        assertSoftly { configUpdatedCount shouldBe 0 }
        assertSoftly { configDeletedCount shouldBe 0 }
        assertSoftly { isErrorNotificationTriggered shouldBe true }
      }
    }

    context("reloadConfigsFromStorage") {
      should("reload configs from storage") {
        var didReloadConfigsFromStorage = false

        every {
          storageServiceMock.reloadConfigsFromStorage()
        } answers {
          didReloadConfigsFromStorage = true
        }

        configCacheService.reloadConfigsFromStorage()

        assertSoftly { didReloadConfigsFromStorage shouldBe true }
      }
    }
  }
})
