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

package org.zowe.explorer.v3.state.config

import org.zowe.explorer.v3.state.config.connection.HttpConnectionConfig
import org.zowe.explorer.v3.state.config.files.FilesWorkingSetConfig
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.zowe.explorer.testutils.MockkAwareShouldSpec

class ConfigStateManagerTestSpec : MockkAwareShouldSpec({
  context("v3/state/config/ConfigStateManager") {
    context("addConfig") {
      should("not add a config to the state manager when the state does not have the configs list by the type") {
        val state = object : ConfigsHolder {
          override var configs: MutableMap<ConfigType, MutableList<Config>> = mutableMapOf()
        }
        val configStateManager = ConfigStateManager(state)

        val result = configStateManager.addConfig(FilesWorkingSetConfig())

        assertSoftly { result.isFailure shouldBe true }
        assertSoftly { (result.exceptionOrNull()?.message ?: "") shouldContain "not registered" }
      }
    }
    context("replaceConfigByType") {
      should("replace configs by the config type") {
        val state = object : ConfigsHolder {
          override var configs: MutableMap<ConfigType, MutableList<Config>> = mutableMapOf(
            ConfigType.FILES_WORKING_SET_CONFIG_V1 to mutableListOf()
          )
        }
        val newConfigsList = listOf(FilesWorkingSetConfig())
        val configStateManager = ConfigStateManager(state)

        val result = configStateManager.replaceConfigsByType(ConfigType.FILES_WORKING_SET_CONFIG_V1, newConfigsList)

        assertSoftly { result.isSuccess shouldBe true }
        assertSoftly { result.getOrNull() shouldBe newConfigsList }
      }
      should("not replace configs by the config type when the state does not have the configs list by the type") {
        val state = object : ConfigsHolder {
          override var configs: MutableMap<ConfigType, MutableList<Config>> = mutableMapOf()
        }
        val newConfigsList = listOf(FilesWorkingSetConfig())
        val configStateManager = ConfigStateManager(state)

        val result = configStateManager.replaceConfigsByType(ConfigType.FILES_WORKING_SET_CONFIG_V1, newConfigsList)

        assertSoftly { result.isFailure shouldBe true }
        assertSoftly { (result.exceptionOrNull()?.message ?: "") shouldContain "not registered" }
      }
    }
    context("updateConfig") {
      should("not update a config by the config type cause it is the same as the stored one") {
        val config = FilesWorkingSetConfig()
        val state = object : ConfigsHolder {
          override var configs: MutableMap<ConfigType, MutableList<Config>> = mutableMapOf(
            ConfigType.FILES_WORKING_SET_CONFIG_V1 to mutableListOf(config)
          )
        }
        val configStateManager = ConfigStateManager(state)

        val result = configStateManager.updateConfig(config)

        assertSoftly { result.isFailure shouldBe true }
        assertSoftly { (result.exceptionOrNull()?.message ?: "") shouldContain "the same as the provided instance" }
      }
      should("not update a config by the config type when the state does not have the configs list by the type") {
        val state = object : ConfigsHolder {
          override var configs: MutableMap<ConfigType, MutableList<Config>> = mutableMapOf()
        }
        val configStateManager = ConfigStateManager(state)

        val result = configStateManager.updateConfig(FilesWorkingSetConfig())

        assertSoftly { result.isFailure shouldBe true }
        assertSoftly { (result.exceptionOrNull()?.message ?: "") shouldContain "not registered" }
      }
    }
    context("deleteConfig") {
      should("not delete a config by the config type when the state does not have the configs list by the type") {
        val state = object : ConfigsHolder {
          override var configs: MutableMap<ConfigType, MutableList<Config>> = mutableMapOf()
        }
        val configStateManager = ConfigStateManager(state)

        val result = configStateManager.deleteConfig(HttpConnectionConfig())

        assertSoftly { result.isFailure shouldBe true }
        assertSoftly { (result.exceptionOrNull()?.message ?: "") shouldContain "not registered" }
      }
    }
  }
})
