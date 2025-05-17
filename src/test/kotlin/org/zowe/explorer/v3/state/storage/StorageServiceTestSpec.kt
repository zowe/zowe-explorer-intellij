/*
 * Copyright (c) 2024 IBA Group.
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

package org.zowe.explorer.v3.state.storage

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.ExtensionTestUtil
import com.intellij.util.xmlb.XmlSerializer
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.v3.state.config.Config
import org.zowe.explorer.v3.state.config.ConfigDeclarator
import org.zowe.explorer.v3.state.config.ConfigEventListener
import org.zowe.explorer.v3.state.config.ConfigType
import org.zowe.explorer.v3.state.config.files.FilesWorkingSetConfig
import org.zowe.explorer.v3.state.config.files.FilesWorkingSetConfigDeclarator
import org.zowe.explorer.v3.state.config.migration.doesXmlHasConfigs
import org.zowe.explorer.v3.state.config.migration.performCascadeMigration
import org.zowe.explorer.v3.state.config.migration.xmlToConfigs
import org.zowe.explorer.v3.state.settings.OtherSettingsEventListener
import org.zowe.explorer.v3.state.settings.OtherSettingsHolder
import org.zowe.explorer.v3.state.settings.OtherSettingsState
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.fail
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.jdom.Attribute
import org.jdom.Element
import org.junit.jupiter.api.assertThrows
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.testutils.getPrivateFieldValue
import org.zowe.explorer.utils.subscribe
import java.util.UUID
import kotlin.reflect.KFunction

@OptIn(StableStorage::class)
class StorageServiceTestSpec : AppInitShouldSpec("v3/state/storage/StorageService", {
  lateinit var currentTestUuid: UUID

  beforeSpec {
    currentTestUuid = AppInitShouldSpec.currentTestUuid ?: throw Exception("Test UUID must be defined before the spec run")
  }

  context("all funcitons") {
    val testAppConfigDeclaratorEpName =
      ExtensionPointName<ConfigDeclarator>("org.zowe.explorer.configDeclarator")
    var testAppConfigDeclaratorEpNameDisposable = Disposer.newDisposable()

    var registerConfigsCallCount = 0
    var didReloadAfterLoadStateHappen = false
    var addConfigCallCount = 0
    var updateConfigCallCount = 0
    var deleteConfigCallCount = 0
    var didOtherSettingsReload = false
    var didOtherSettingsChange = false
    var isErrorNotificationTriggered = false

    mockkObject(NotificationsService)
    every {
      NotificationsService.errorNotification(any())
    } answers {
      isErrorNotificationTriggered = true
    }

    mockkStatic("org.zowe.explorer.v3.state.config.migration.UtilsKt")

    val storageService = StorageService.getService()
    val state = getPrivateFieldValue(storageService, "storageState") as StorageState

    subscribe(
      StorageService.STORAGE_CONFIGS_TOPIC,
      object : ConfigEventListener {
        override fun registered(configType: ConfigType) {
          if (currentTestUuid == AppInitShouldSpec.currentTestUuid) {
            if (configType == ConfigType.FILES_WORKING_SET_CONFIG_V1) {
              registerConfigsCallCount += 1
            } else {
              fail("Wrong config type registered")
            }
          }
        }

        override fun added(config: Config) {
          if (currentTestUuid == AppInitShouldSpec.currentTestUuid) {
            if (config.configType == ConfigType.FILES_WORKING_SET_CONFIG_V1) {
              addConfigCallCount += 1
            } else {
              fail("Wrong config added")
            }
          }
        }

        override fun updated(oldConfig: Config, newConfig: Config) {
          if (currentTestUuid == AppInitShouldSpec.currentTestUuid) {
            if (
              oldConfig.configType == ConfigType.FILES_WORKING_SET_CONFIG_V1
              && oldConfig.configType == newConfig.configType
            ) {
              updateConfigCallCount += 1
            } else {
              fail("Wrong config updated")
            }
          }
        }

        override fun deleted(config: Config) {
          if (currentTestUuid == AppInitShouldSpec.currentTestUuid) {
            if (config.configType == ConfigType.FILES_WORKING_SET_CONFIG_V1) {
              deleteConfigCallCount += 1
            } else {
              fail("Wrong config deleted")
            }
          }
        }

        override fun reloaded(configType: ConfigType, reloadedConfigs: List<Config>) {
          if (currentTestUuid == AppInitShouldSpec.currentTestUuid) {
            if (configType == ConfigType.FILES_WORKING_SET_CONFIG_V1 && reloadedConfigs.size == 1) {
              didReloadAfterLoadStateHappen = true
            }
          }
        }
      }
    )

    subscribe(
      StorageService.OTHER_SETTINGS_TOPIC,
      object : OtherSettingsEventListener {
        override fun otherSettingsReloaded(newSettings: OtherSettingsHolder) {
          if (currentTestUuid == AppInitShouldSpec.currentTestUuid) {
            didOtherSettingsReload = true
          }
        }

        override fun otherSettingsChanged(newSettings: OtherSettingsHolder) {
          if (currentTestUuid == AppInitShouldSpec.currentTestUuid) {
            didOtherSettingsChange = true
          }
        }
      }
    )

    beforeEach {
      testAppConfigDeclaratorEpNameDisposable = Disposer.newDisposable()

      registerConfigsCallCount = 0
      didReloadAfterLoadStateHappen = false
      addConfigCallCount = 0
      updateConfigCallCount = 0
      deleteConfigCallCount = 0
      didOtherSettingsReload = false
      didOtherSettingsChange = false
      isErrorNotificationTriggered = false

      val testConfigDeclarator = FilesWorkingSetConfigDeclarator()
      ExtensionTestUtil
        .maskExtensions(
          testAppConfigDeclaratorEpName,
          listOf(testConfigDeclarator),
          testAppConfigDeclaratorEpNameDisposable
        )

      state.configs = mutableMapOf()
    }

    afterEach {
      StorageService.resetService()
      Disposer.dispose(testAppConfigDeclaratorEpNameDisposable)
    }

    context("loadState") {
      var isCascadeMigrationPerformed = false

      beforeEach {
        isCascadeMigrationPerformed = false
      }

      val xmlDeserializeFunState: (Element, Class<StorageState>) -> StorageState = XmlSerializer::deserialize
      mockkStatic(xmlDeserializeFunState as KFunction<*>)
      every { xmlDeserializeFunState(any<Element>(), any<Class<StorageState>>()) } returns StorageState()

      should("successfully load state from XML") {
        val stateXml = mockk<Element> {
          every { getChild("configs") } returns mockk {
            every { children } returns listOf(
              mockk {
                every { attributes } returns listOf(
                  Attribute("type", "${ConfigType.FILES_WORKING_SET_CONFIG_V1}")
                )
              }
            )
          }
          every { removeChild("configs") } returns true
        }

        every { doesXmlHasConfigs(any(), any()) } returns true
        every { xmlToConfigs(any(), any(), any()) } returns mutableListOf(mockk())
        every {
          performCascadeMigration(any(), any())
        } answers {
          isCascadeMigrationPerformed = true
          mutableMapOf(ConfigType.FILES_WORKING_SET_CONFIG_V1 to mutableListOf(mockk()))
        }

        StorageService.getService()
          .loadState(stateXml)

        assertSoftly { registerConfigsCallCount shouldBe 1 }
        assertSoftly { didReloadAfterLoadStateHappen shouldBe true }
        assertSoftly { didOtherSettingsReload shouldBe true }
        assertSoftly { isErrorNotificationTriggered shouldBe false }
        assertSoftly { isCascadeMigrationPerformed shouldBe false }
      }
      should("successfully load state from XML, performing a cascade migration") {
        val stateXml = mockk<Element> {
          every { getChild("configs") } returns mockk {
            every { children } returns listOf(
              mockk {
                every { attributes } returns listOf(
                  Attribute("type", "UnrecognizedOne")
                )
              }
            )
          }
          every { removeChild("configs") } returns true
        }

        every { doesXmlHasConfigs(any(), any()) } returns true
        every { xmlToConfigs(any(), any(), any()) } returns mutableListOf(mockk())
        every {
          performCascadeMigration(any(), any())
        } answers {
          isCascadeMigrationPerformed = true
          mutableMapOf(ConfigType.FILES_WORKING_SET_CONFIG_V1 to mutableListOf(mockk()))
        }

        StorageService.getService()
          .loadState(stateXml)

        assertSoftly { registerConfigsCallCount shouldBe 1 }
        assertSoftly { didReloadAfterLoadStateHappen shouldBe true }
        assertSoftly { didOtherSettingsReload shouldBe true }
        assertSoftly { isErrorNotificationTriggered shouldBe false }
        assertSoftly { isCascadeMigrationPerformed shouldBe true }
      }
      should("successfully load state from XML, even if there is no configs saved") {
        val stateXml = mockk<Element> {
          every { getChild("configs") } returns null
          every { removeChild("configs") } returns true
        }

        every { doesXmlHasConfigs(any(), any()) } returns false
        every { xmlToConfigs(any(), any(), any()) } returns mutableListOf()
        every {
          performCascadeMigration(any(), any())
        } answers {
          isCascadeMigrationPerformed = true
          mutableMapOf(ConfigType.FILES_WORKING_SET_CONFIG_V1 to mutableListOf(mockk()))
        }

        StorageService.getService()
          .loadState(stateXml)

        assertSoftly { registerConfigsCallCount shouldBe 1 }
        assertSoftly { didReloadAfterLoadStateHappen shouldBe false }
        assertSoftly { didOtherSettingsReload shouldBe true }
        assertSoftly { isErrorNotificationTriggered shouldBe false }
        assertSoftly { isCascadeMigrationPerformed shouldBe false }
      }
      should("not load state from XML as the service is unstable") {
        val stateXml = mockk<Element> {
          every { getChild("configs") } returns mockk {
            every { children } returns listOf(
              mockk {
                every { attributes } returns listOf(
                  Attribute("type", "${ConfigType.FILES_WORKING_SET_CONFIG_V1}")
                )
              }
            )
          }
          every { removeChild("configs") } returns true
        }

        every { doesXmlHasConfigs(any(), any()) } returns true
        every { xmlToConfigs(any(), any(), any()) } returns mutableListOf()
        every {
          performCascadeMigration(any(), any())
        } answers {
          isCascadeMigrationPerformed = true
          mutableMapOf(ConfigType.FILES_WORKING_SET_CONFIG_V1 to mutableListOf(mockk()))
        }

        StorageService.getService()
          .loadState(stateXml)

        assertSoftly { registerConfigsCallCount shouldBe 1 }
        assertSoftly { didReloadAfterLoadStateHappen shouldBe false }
        assertSoftly { didOtherSettingsReload shouldBe false }
        assertSoftly { isErrorNotificationTriggered shouldBe true }
        assertSoftly { isCascadeMigrationPerformed shouldBe false }
        assertThrows<Exception> { StorageService.getService() }
      }
    }

    context("common") {
      context("registerConfigType") {
        should("not register config types if they are already registered") {
          StorageService.getService()
            .registerConfigTypes()
          StorageService.getService()
            .registerConfigTypes()

          assertSoftly { registerConfigsCallCount shouldBe 1 }
          assertSoftly { didReloadAfterLoadStateHappen shouldBe false }
          assertSoftly { addConfigCallCount shouldBe 0 }
          assertSoftly { updateConfigCallCount shouldBe 0 }
          assertSoftly { isErrorNotificationTriggered shouldBe false }
          assertSoftly { didOtherSettingsReload shouldBe false }
          assertSoftly { didOtherSettingsChange shouldBe false }
        }
      }
      context("addConfigToStorage") {
        should("add new config to storage by specified type") {
          StorageService.getService()
            .registerConfigTypes()
          val config = FilesWorkingSetConfig()
          StorageService.getService()
            .addConfigToStorage(config)
          val actualConfigs = StorageService.getService()
            .getConfigsFromStorage(ConfigType.FILES_WORKING_SET_CONFIG_V1)

          assertSoftly { registerConfigsCallCount shouldBe 1 }
          assertSoftly { didReloadAfterLoadStateHappen shouldBe false }
          assertSoftly { addConfigCallCount shouldBe 1 }
          assertSoftly { updateConfigCallCount shouldBe 0 }
          assertSoftly { isErrorNotificationTriggered shouldBe false }
          assertSoftly { didOtherSettingsReload shouldBe false }
          assertSoftly { didOtherSettingsChange shouldBe false }
          assertSoftly { actualConfigs.toList() shouldBe listOf(config) }
        }
        should("produce an error when trying to add the same config again") {
          StorageService.getService()
            .registerConfigTypes()
          val config = FilesWorkingSetConfig()
          StorageService.getService()
            .addConfigToStorage(config)
          StorageService.getService()
            .addConfigToStorage(config)
          val actualConfigs = StorageService.getService()
            .getConfigsFromStorage(ConfigType.FILES_WORKING_SET_CONFIG_V1)

          assertSoftly { registerConfigsCallCount shouldBe 1 }
          assertSoftly { didReloadAfterLoadStateHappen shouldBe false }
          assertSoftly { addConfigCallCount shouldBe 1 }
          assertSoftly { updateConfigCallCount shouldBe 0 }
          assertSoftly { isErrorNotificationTriggered shouldBe true }
          assertSoftly { didOtherSettingsReload shouldBe false }
          assertSoftly { didOtherSettingsChange shouldBe false }
          assertSoftly { actualConfigs.toList() shouldBe listOf(config) }
        }
      }
      context("updateConfigInStorage") {
        should("update a config in the storage") {
          StorageService.getService()
            .registerConfigTypes()
          val config = FilesWorkingSetConfig()
          val newConfig = FilesWorkingSetConfig(uuid = config.uuid, name = "test")
          StorageService.getService()
            .addConfigToStorage(config)
          StorageService.getService()
            .updateConfigInStorage(newConfig)
          val actualConfigs = StorageService.getService()
            .getConfigsFromStorage(ConfigType.FILES_WORKING_SET_CONFIG_V1)

          assertSoftly { registerConfigsCallCount shouldBe 1 }
          assertSoftly { didReloadAfterLoadStateHappen shouldBe false }
          assertSoftly { addConfigCallCount shouldBe 1 }
          assertSoftly { updateConfigCallCount shouldBe 1 }
          assertSoftly { isErrorNotificationTriggered shouldBe false }
          assertSoftly { didOtherSettingsReload shouldBe false }
          assertSoftly { didOtherSettingsChange shouldBe false }
          assertSoftly { actualConfigs.toList() shouldBe listOf(newConfig) }
        }
        should("not update a config in the storage cause it does not exist") {
          StorageService.getService()
            .registerConfigTypes()
          StorageService.getService()
            .updateConfigInStorage(FilesWorkingSetConfig())
          val actualConfigs = StorageService.getService()
            .getConfigsFromStorage(ConfigType.FILES_WORKING_SET_CONFIG_V1)

          assertSoftly { registerConfigsCallCount shouldBe 1 }
          assertSoftly { didReloadAfterLoadStateHappen shouldBe false }
          assertSoftly { addConfigCallCount shouldBe 0 }
          assertSoftly { updateConfigCallCount shouldBe 0 }
          assertSoftly { isErrorNotificationTriggered shouldBe true }
          assertSoftly { didOtherSettingsReload shouldBe false }
          assertSoftly { didOtherSettingsChange shouldBe false }
          assertSoftly { actualConfigs.toList() shouldBe listOf() }
        }
      }
      context("deleteConfigFromStorage") {
        should("delete a config from the storage") {
          StorageService.getService()
            .registerConfigTypes()
          val config = FilesWorkingSetConfig()
          StorageService.getService()
            .addConfigToStorage(config)
          StorageService.getService()
            .deleteConfigFromStorage(config)
          val actualConfigs = StorageService.getService()
            .getConfigsFromStorage(ConfigType.FILES_WORKING_SET_CONFIG_V1)

          assertSoftly { registerConfigsCallCount shouldBe 1 }
          assertSoftly { didReloadAfterLoadStateHappen shouldBe false }
          assertSoftly { addConfigCallCount shouldBe 1 }
          assertSoftly { updateConfigCallCount shouldBe 0 }
          assertSoftly { deleteConfigCallCount shouldBe 1 }
          assertSoftly { isErrorNotificationTriggered shouldBe false }
          assertSoftly { didOtherSettingsReload shouldBe false }
          assertSoftly { didOtherSettingsChange shouldBe false }
          assertSoftly { actualConfigs.toList() shouldBe listOf() }
        }
        should("not delete a config from the storage cause it does not exist") {
          StorageService.getService()
            .registerConfigTypes()
          StorageService.getService()
            .deleteConfigFromStorage(FilesWorkingSetConfig())
          val actualConfigs = StorageService.getService()
            .getConfigsFromStorage(ConfigType.FILES_WORKING_SET_CONFIG_V1)

          assertSoftly { registerConfigsCallCount shouldBe 1 }
          assertSoftly { didReloadAfterLoadStateHappen shouldBe false }
          assertSoftly { addConfigCallCount shouldBe 0 }
          assertSoftly { updateConfigCallCount shouldBe 0 }
          assertSoftly { deleteConfigCallCount shouldBe 0 }
          assertSoftly { isErrorNotificationTriggered shouldBe true }
          assertSoftly { didOtherSettingsReload shouldBe false }
          assertSoftly { didOtherSettingsChange shouldBe false }
          assertSoftly { actualConfigs.toList() shouldBe listOf() }
        }
      }
      context("updateSettingsInStorage") {
        should("not delete a config from the storage cause it does not exist") {
          StorageService.getService()
            .registerConfigTypes()
          val newSettings = OtherSettingsState()
          StorageService.getService()
            .updateSettingsInStorage(newSettings)

          assertSoftly { registerConfigsCallCount shouldBe 1 }
          assertSoftly { didReloadAfterLoadStateHappen shouldBe false }
          assertSoftly { addConfigCallCount shouldBe 0 }
          assertSoftly { updateConfigCallCount shouldBe 0 }
          assertSoftly { deleteConfigCallCount shouldBe 0 }
          assertSoftly { isErrorNotificationTriggered shouldBe false }
          assertSoftly { didOtherSettingsReload shouldBe false }
          assertSoftly { didOtherSettingsChange shouldBe true }
        }
      }
    }
  }
})
