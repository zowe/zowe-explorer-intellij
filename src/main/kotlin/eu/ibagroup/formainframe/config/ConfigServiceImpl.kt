/*
 * Copyright (c) 2020-2024 IBA Group.
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
 */

package eu.ibagroup.formainframe.config

import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import com.jetbrains.rd.util.UUID
import eu.ibagroup.formainframe.config.connect.Credentials
import eu.ibagroup.formainframe.utils.castOrNull
import eu.ibagroup.formainframe.utils.crudable.AddFilter
import eu.ibagroup.formainframe.utils.crudable.ConcurrentCrudable
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.CrudableLists
import eu.ibagroup.formainframe.utils.crudable.SimpleReadWriteAdapter
import eu.ibagroup.formainframe.utils.crudable.UpdateFilter
import eu.ibagroup.formainframe.utils.loadConfigClass
import eu.ibagroup.formainframe.utils.runIfTrue
import java.time.Duration

/**
 * Configure crudable object to work with. Will create a new CrudableLists, wrapped by the ConcurrentCrudable
 * @param block the callback to initialize events handlers
 */
internal inline fun Crudable.configureCrudable(block: CrudableLists.() -> Unit): Crudable {
  this.castOrNull<ConcurrentCrudable>()?.apply {
    wrappingCrudable.castOrNull<CrudableLists>()?.apply(block)
  }
  return this
}

/** Config service state class. Describes all the interactions with the plugin configs */
@State(
  name = "eu.ibagroup.connector.services.ConfigService",
  storages = [Storage(value = "iba_connector_config_v2.xml", exportable = true)]
)
class ConfigServiceImpl : ConfigService {

  companion object {
    private val myState = ConfigStateV2()
  }

  override fun getState(): ConfigStateV2 {
    return myState
  }

  /**
   * Load current config state
   * @param state the state to load
   */
  override fun loadState(state: ConfigStateV2) {
    XmlSerializerUtil.copyBean(state, myState)
  }

  override val eventHandler = ConfigEventHandler()

  override val autoSaveDelay: Duration = Duration.ofSeconds(5)

  override val crudable = makeCrudableWithoutListeners(false) { myState }
    .configureCrudable {
      eventHandler = this@ConfigServiceImpl.eventHandler
    }

  /** List of registered config declarations */
  private val configDeclarations: List<ConfigDeclaration<out Any>> by lazy {
    ConfigDeclaration.EP.extensionList.map {
      it.buildConfigDeclaration()
    }
  }

  override var isAutoSyncEnabled: Boolean
    get() = state.settings.isAutoSyncEnabled
    set(value) {
      state.settings.isAutoSyncEnabled = value
    }

  override var batchSize: Int
    get() = state.settings.batchSize
    set(value) {
      state.settings.batchSize = value
    }

  override var successMaxCode: Int
    get() = state.settings.successMaxCode
    set(value) {
      state.settings.successMaxCode = value
    }

  override var warningMaxCode: Int
    get() = state.settings.warningMaxCode
    set(value) {
      state.settings.warningMaxCode = value
    }

  override var rateUsNotificationDelay: Long
    get() = state.settings.rateUsNotificationDelay
    set(value) {
      state.settings.rateUsNotificationDelay = value
    }

  /**
   * Finds [ConfigDeclaration] for specified class through registered extension points (see [configDeclarations]).
   * @param rowClass config class instance for which to find [ConfigDeclaration].
   * @throws IllegalArgumentException if no config declaration found for passed class.
   * @return config declaration instance.
   */
  @Suppress("UNCHECKED_CAST")
  override fun <T : Any> getConfigDeclaration(rowClass: Class<out T>): ConfigDeclaration<T> {
    val result = configDeclarations.firstOrNull { it.clazz == rowClass }
      ?: throw IllegalArgumentException("No configDeclaration found for class ${rowClass::class.java}.")
    return result as ConfigDeclaration<T>
  }

  /** Creates collection for config class in state (see [ConfigService.registerConfigClass]). */
  override fun <T> registerConfigClass(clazz: Class<out T>) {
    if (!state.collections.containsKey(clazz.name)) {
      state.collections[clazz.name] = mutableListOf<T>()
    }
    ConfigSandbox.getService().registerConfigClass(clazz)
  }

  /**
   * Creates collection for each class of registered config declarations.
   * @see ConfigService.registerAllConfigClasses
   */
  override fun registerAllConfigClasses() {
    configDeclarations.forEach { registerConfigClass(it.clazz) }
  }

  /** Returns keys of [ConfigStateV2.collections]. see [ConfigService.getRegisteredConfigClasses] */
  override fun getRegisteredConfigClasses(): List<Class<*>> {
    return state.collections.keys.mapNotNull { loadConfigClass(it) }
  }

  /** Returns registered in IoC container config declarations. see [ConfigService.getRegisteredConfigDeclarations] */
  override fun getRegisteredConfigDeclarations(): List<ConfigDeclaration<*>> {
    return configDeclarations.toList()
  }

}


/**
 * Make the raw crudable for the config service, that will give all the configs by the state getter.
 * Also, "add" and "update" filters are initialized along with the next UUID provider
 * @param withCredentials the value to check whether the crudable should work with the credentials config or not
 * @param credentialsGetter the credential getter callback. Returns empty list by default
 * @param stateGetter the current config state getter. Returns config state to work with
 */
internal fun makeCrudableWithoutListeners(
  withCredentials: Boolean,
  credentialsGetter: () -> MutableList<Credentials> = { mutableListOf() },
  stateGetter: () -> ConfigStateV2,
): Crudable {
  lateinit var crudable: Crudable

  val crudableLists = CrudableLists(
    addFilter = object : AddFilter {
      override operator fun <T : Any> invoke(clazz: Class<out T>, addingRow: T): Boolean {
        return ConfigService.getService().getConfigDeclaration(clazz).getDecider(crudable).canAdd(addingRow)
      }
    },
    updateFilter = object : UpdateFilter {
      override operator fun <T : Any> invoke(clazz: Class<out T>, currentRow: T, updatingRow: T): Boolean {
        return ConfigService.getService().getConfigDeclaration(clazz).getDecider(crudable).canUpdate(currentRow, updatingRow)
      }
    },
    nextUuidProvider = { UUID.randomUUID().toString() },
    getListByClass = {
      if (it == Credentials::class.java) {
        withCredentials.runIfTrue {
          credentialsGetter()
        }
      } else {
        stateGetter().get(it)
      }
    }
  )

  crudable = ConcurrentCrudable(crudableLists, SimpleReadWriteAdapter())
  return crudable
}
