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
import eu.ibagroup.formainframe.config.ws.JobsWorkingSetConfig
import eu.ibagroup.formainframe.utils.castOrNull
import eu.ibagroup.formainframe.utils.crudable.*
import eu.ibagroup.formainframe.utils.runIfTrue
import org.jetbrains.annotations.ApiStatus
import java.nio.file.Paths
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import javax.xml.parsers.DocumentBuilderFactory

/** Config service state class. Describes all the interactions with the plugin config */
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

  override var isAutoSyncEnabled = AtomicBoolean(false)

  /** Adapt all configs in old style to the new one and updates config file. */
  @ApiStatus.ScheduledForRemoval(inVersion = "0.7")
  private fun acceptOldConfigs() {
    myState.connections = myState.connections.toMutableList()
    myState.jobsWorkingSets = myState.jobsWorkingSets.toMutableList()
    myState.workingSets = myState.workingSets.toMutableList()

    val configLocation =
      Paths.get(PathManager.getConfigPath(), PathManager.OPTIONS_DIRECTORY, "iba_connector_config.xml")
    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(configLocation?.toFile())
    val oldConfigsAdapters = OldConfigAdapter.EP.extensions.map { it.buildAdapter(document) }
    oldConfigsAdapters.forEach { adapter ->
      adapter.getOldConfigsIds().forEach { crudable.deleteByUniqueKey(adapter.configClass, it) }
      adapter.castOldConfigs().forEach { crudable.addOrUpdate(it) }
    }
    ApplicationManager.getApplication().saveSettings()
  }
}

// TODO: doc
internal abstract class ClassCaseSwitcher<R> {

  abstract fun onConnectionConfig(): R

  abstract fun onWorkingSetConfig(): R

  abstract fun onJobsWorkingSetConfig(): R

  open fun onCredentials(): R {
    return onElse()
  }

  abstract fun onElse(): R

  operator fun invoke(clazz: Class<*>): R {
    return when (clazz) {
      ConnectionConfig::class.java -> onConnectionConfig()
      FilesWorkingSetConfig::class.java -> onWorkingSetConfig()
      JobsWorkingSetConfig::class.java -> onJobsWorkingSetConfig()
      Credentials::class.java -> onCredentials()
      else -> onElse()
    }
  }

}

private class FilterSwitcher(
  private val crudable: Crudable,
  private val row: Any
) : ClassCaseSwitcher<Long>() {
  override fun onConnectionConfig(): Long {
    return crudable.getByColumnLambda(row as ConnectionConfig) { it.name }.count()
  }


  override fun onWorkingSetConfig(): Long {
    return crudable.getByColumnLambda(row as FilesWorkingSetConfig) { it.name }.count()
  }

  override fun onJobsWorkingSetConfig(): Long {
    return crudable.getByColumnLambda(row as JobsWorkingSetConfig) { it.uuid }.count()
  }

  override fun onCredentials(): Long {
    return 0
  }

  override fun onElse(): Long {
    return -1
  }
}

private class UpdateFilterSwitcher(
  crudable: Crudable,
  private val currentRow: Any,
  private val updatingRow: Any
) : ClassCaseSwitcher<Boolean>() {
  private val filterSwitcher = FilterSwitcher(crudable, updatingRow)

  override fun onConnectionConfig(): Boolean {
    return if (currentRow is ConnectionConfig && updatingRow is ConnectionConfig) {
      filterSwitcher.onConnectionConfig() == 0L || updatingRow.name == currentRow.name || updatingRow.zVersion == currentRow.zVersion || updatingRow.url == currentRow.url || updatingRow.codePage == currentRow.codePage || updatingRow.isAllowSelfSigned == updatingRow.isAllowSelfSigned
    } else false
  }


  override fun onWorkingSetConfig(): Boolean {
    return if (currentRow is FilesWorkingSetConfig && updatingRow is FilesWorkingSetConfig) {
      filterSwitcher.onWorkingSetConfig() == 0L || updatingRow.name == currentRow.name
    } else false
  }

  override fun onJobsWorkingSetConfig(): Boolean {
    return if (currentRow is JobsWorkingSetConfig && updatingRow is JobsWorkingSetConfig) {
      filterSwitcher.onJobsWorkingSetConfig() == 0L || updatingRow.uuid == currentRow.uuid
    } else false
  }

  override fun onCredentials(): Boolean {
    return true
  }

  override fun onElse(): Boolean {
    return false
  }

}

internal fun makeCrudableWithoutListeners(
  withCredentials: Boolean,
  credentialsGetter: () -> MutableList<Credentials> = { mutableListOf() },
  stateGetter: () -> ConfigState,
): Crudable {
  return CrudableListsBuilder {
    object : ClassCaseSwitcher<MutableList<*>?>() {
      override fun onConnectionConfig(): MutableList<*> {
        return stateGetter().connections
      }

      override fun onWorkingSetConfig(): MutableList<*> {
        return stateGetter().workingSets
      }

      override fun onCredentials(): MutableList<*>? {
        return withCredentials.runIfTrue {
          credentialsGetter()
        }
      }

      override fun onJobsWorkingSetConfig(): MutableList<*> {
        return stateGetter().jobsWorkingSets
      }

      override fun onElse(): MutableList<*>? {
        return null
      }
    }(it)
  }.withNextUuidProvider {
    UUID.randomUUID().toString()
  }.apply crudable@{
    addFilter = AddFilter { rowClass, row ->
      FilterSwitcher(this, row)(rowClass) == 0L
    }
    updateFilter = UpdateFilter { clazz, currentRow, updatingRow ->
      UpdateFilterSwitcher(this, currentRow, updatingRow)(clazz)
    }
  }.let {
    ConcurrentCrudable(it, SimpleReadWriteAdapter())
  }
}

internal inline fun Crudable.configureCrudable(block: CrudableLists.() -> Unit): Crudable {
  this.castOrNull<ConcurrentCrudable>()?.apply {
    wrappingCrudable.castOrNull<CrudableLists>()?.apply(block)
  }
  return this
}

@Suppress("UNCHECKED_CAST")
internal fun <T> classToList(clazz: Class<out T>, state: ConfigState): MutableList<T>? {
  return object : ClassCaseSwitcher<MutableList<T>?>() {
    override fun onConnectionConfig(): MutableList<T> {
      return state.connections as MutableList<T>
    }

    override fun onWorkingSetConfig(): MutableList<T> {
      return state.workingSets as MutableList<T>
    }

    override fun onJobsWorkingSetConfig(): MutableList<T> {
      return state.jobsWorkingSets as MutableList<T>
    }

    override fun onElse(): MutableList<T>? {
      return null
    }

  }(clazz as Class<*>)
}
