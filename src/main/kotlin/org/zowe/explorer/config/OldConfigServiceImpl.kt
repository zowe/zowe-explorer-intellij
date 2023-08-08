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

import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.Credentials
import org.zowe.explorer.config.ws.FilesWorkingSetConfig
import org.zowe.explorer.config.ws.JesWorkingSetConfig
import org.zowe.explorer.utils.crudable.*
import org.zowe.explorer.utils.runIfTrue
import java.util.UUID

/**
 * Implementation of [OldConfigService] to read old configs from old file with configs.
 * @author Valiantsin Krus.
 */
@State(
  name = "org.zowe.explorer.config.OldConfigService",
  storages = [Storage(value = "zowe_explorer_intellij_config.xml", exportable = true)]
)
class OldConfigServiceImpl: OldConfigService {

  companion object {
    private val myState: ConfigState = ConfigState()
  }

  override fun getState(): ConfigState {
    return myState
  }

  override val crudable = makeCrudableWithoutListenersOld(false) { myState }

  /**
   * Load current config state
   * @param state the state to load
   */
  override fun loadState(state: ConfigState) {
    XmlSerializerUtil.copyBean(state, myState)
  }
}

/**
 * Make the raw crudable for the **old** config service, that will give all the configs by the state getter.
 * Also, "add" and "update" filters are initialized along with the next UUID provider
 * @param withCredentials the value to check whether the crudable should work with the credentials config or not
 * @param credentialsGetter the credential getter callback. Returns empty list by default
 * @param stateGetter the current config state getter. Returns config state to work with
 */
@Deprecated("Use function makeCrudableWithoutListeners in ConfigServiceImpl.")
internal fun makeCrudableWithoutListenersOld(
  withCredentials: Boolean,
  credentialsGetter: () -> MutableList<Credentials> = { mutableListOf() },
  stateGetter: () -> ConfigState
): Crudable {
  val crudableLists = CrudableLists(
    addFilter = object: AddFilter {
      override operator fun <T : Any> invoke(clazz: Class<out T>, addingRow: T): Boolean {
        return ConfigService.instance.getConfigDeclaration(clazz).getDecider().canAdd(addingRow)
      }
    },
    updateFilter = object: UpdateFilter {
      override operator fun <T: Any> invoke(clazz: Class<out T>, currentRow: T, updatingRow: T): Boolean {
        return ConfigService.instance.getConfigDeclaration(clazz).getDecider().canUpdate(currentRow, updatingRow)
      }
    },
    nextUuidProvider = { UUID.randomUUID().toString() },
    getListByClass = {
      if (it == Credentials::class.java) {
        withCredentials.runIfTrue {
          credentialsGetter()
        }
      } else {
        when (it) {
          ConnectionConfig::class.java -> stateGetter().connections
          FilesWorkingSetConfig::class.java -> stateGetter().filesWorkingSets
          JesWorkingSetConfig::class.java -> stateGetter().jesWorkingSets
          else -> throw IllegalArgumentException("There is no config class ${it.name}")
        }
      }
    }
  )
  return ConcurrentCrudable(crudableLists, SimpleReadWriteAdapter())
}
