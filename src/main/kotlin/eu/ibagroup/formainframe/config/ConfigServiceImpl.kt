/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import com.jetbrains.rd.util.UUID
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.Credentials
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.JesWorkingSetConfig
import eu.ibagroup.formainframe.utils.castOrNull
import eu.ibagroup.formainframe.utils.crudable.*
import eu.ibagroup.formainframe.utils.runIfTrue
import java.nio.file.Paths
import java.time.Duration
import javax.xml.parsers.DocumentBuilderFactory

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
  name = "by.iba.connector.services.ConfigService",
  storages = [Storage(value = "iba_connector_config.xml", exportable = true)]
)
class ConfigServiceImpl : ConfigService {

  companion object {
    private val myState = ConfigState()
  }

  override fun getState(): ConfigState {
    return myState
  }

  /**
   * Load current config state
   * @param state the state to load
   */
  override fun loadState(state: ConfigState) {
    XmlSerializerUtil.copyBean(state, myState)
    acceptOldConfigs()
  }

  override val eventHandler = ConfigEventHandler()

  override val autoSaveDelay: Duration = Duration.ofSeconds(5)

  override val crudable = makeCrudableWithoutListeners(false) { myState }
    .configureCrudable {
      eventHandler = this@ConfigServiceImpl.eventHandler
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

  /**
   * Adapt all configs in old style to the new one and update config file.
   * Notice: update of the file happens only when the config is changed by user (e.g.: mask change, job filter change, mask add)
   */
  private fun acceptOldConfigs() {
    myState.connections = myState.connections.toMutableList()
    myState.jesWorkingSets = myState.jesWorkingSets.toMutableList()
    myState.filesWorkingSets = myState.filesWorkingSets.toMutableList()

    val configLocation =
      Paths.get(PathManager.getConfigPath(), PathManager.OPTIONS_DIRECTORY, "iba_connector_config.xml")
    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(configLocation.toFile())
    val oldConfigsAdapters = OldConfigAdapter.EP.extensions.map { it.buildAdapter(document) }
    oldConfigsAdapters.forEach { adapter ->
      adapter.getOldConfigsIds().forEach { crudable.deleteByUniqueKey(adapter.configClass, it) }
      adapter.castOldConfigs().forEach { crudable.addOrUpdate(it) }
    }
    ApplicationManager.getApplication().saveSettings()
  }
}

/**
 * Filter decider class, that provides the count of rows by a specific column value, provided by the row parameter
 * @param crudable the crudable object to get the rows from
 * @param row the row parameter with the column value to get rows by
 */
private class FilterDecider(
  private val crudable: Crudable,
  private val row: Any
) : ConfigClassActionDecider<Long>() {

  /** Get the count of the ConnectionConfig rows by the filtering row name */
  override fun onConnectionConfig(): Long {
    return crudable.getByColumnLambda(row as ConnectionConfig) { it.name }.count()
  }

  /** Get the count of the FilesWorkingSetConfig rows by the filtering row name */
  override fun onFilesWorkingSetConfig(): Long {
    return crudable.getByColumnLambda(row as FilesWorkingSetConfig) { it.name }.count()
  }

  /** Get the count of the JesWorkingSetConfig rows by the filtering row UUID */
  override fun onJesWorkingSetConfig(): Long {
    return crudable.getByColumnLambda(row as JesWorkingSetConfig) { it.uuid }.count()
  }

  override fun onCredentials(): Long {
    return 0
  }

  override fun onElse(): Long {
    return -1
  }
}

/**
 * Update filter decider to check whether the row can be updated, corresponding to the conditions for the specific class
 * @param crudable the crudable object to get rows from
 * @param currentRow the row to update
 * @param updatingRow the row object with the updates
 */
private class UpdateFilterDecider(
  crudable: Crudable,
  private val currentRow: Any,
  private val updatingRow: Any
) : ConfigClassActionDecider<Boolean>() {
  private val filterSwitcher = FilterDecider(crudable, updatingRow)

  /**
   * Check if the ConnectionConfig row update can be proceeded.
   * The update is possible either when any of the properties are equal to the same current row property,
   * or if there is no any rows in the config
   */
  override fun onConnectionConfig(): Boolean {
    return if (currentRow is ConnectionConfig && updatingRow is ConnectionConfig) {
      filterSwitcher.onConnectionConfig() == 0L
        || updatingRow.name == currentRow.name
        || updatingRow.zVersion == currentRow.zVersion
        || updatingRow.url == currentRow.url
        || updatingRow.isAllowSelfSigned == currentRow.isAllowSelfSigned
    } else false
  }

  /**
   * Check if the FilesWorkingSetConfig row update can be proceeded.
   * The update is possible either when names of the updating row and the current row are the same,
   * or if there is no any rows in the config
   */
  override fun onFilesWorkingSetConfig(): Boolean {
    return if (currentRow is FilesWorkingSetConfig && updatingRow is FilesWorkingSetConfig) {
      filterSwitcher.onFilesWorkingSetConfig() == 0L || updatingRow.name == currentRow.name
    } else false
  }

  /**
   * Check if the JesWorkingSetConfig row update can be proceeded.
   * The update is possible either when UUIDs of the updating row and the current row are the same,
   * or if there is no any rows in the config
   */
  override fun onJesWorkingSetConfig(): Boolean {
    return if (currentRow is JesWorkingSetConfig && updatingRow is JesWorkingSetConfig) {
      filterSwitcher.onJesWorkingSetConfig() == 0L || updatingRow.uuid == currentRow.uuid
    } else false
  }

  override fun onCredentials(): Boolean {
    return true
  }

  override fun onElse(): Boolean {
    return false
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
  stateGetter: () -> ConfigState,
): Crudable {
  return CrudableListsBuilder {
    object : ConfigClassActionDecider<MutableList<*>?>() {
      override fun onConnectionConfig(): MutableList<*> {
        return stateGetter().connections
      }

      override fun onFilesWorkingSetConfig(): MutableList<*> {
        return stateGetter().filesWorkingSets
      }

      override fun onCredentials(): MutableList<*>? {
        return withCredentials.runIfTrue {
          credentialsGetter()
        }
      }

      override fun onJesWorkingSetConfig(): MutableList<*> {
        return stateGetter().jesWorkingSets
      }

      override fun onElse(): MutableList<*>? {
        return null
      }
    }(it)
  }
    .withNextUuidProvider { UUID.randomUUID().toString() }
    .apply crudable@{
      addFilter = AddFilter { rowClass, row ->
        FilterDecider(this, row)(rowClass) == 0L
      }
      updateFilter = UpdateFilter { clazz, currentRow, updatingRow ->
        UpdateFilterDecider(this, currentRow, updatingRow)(clazz)
      }
    }
    .let { ConcurrentCrudable(it, SimpleReadWriteAdapter()) }
}
