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
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.CredentialService
import eu.ibagroup.formainframe.config.connect.Credentials
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.JesWorkingSetConfig
import eu.ibagroup.formainframe.utils.clone
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.ReloadableEventHandler
import eu.ibagroup.formainframe.utils.crudable.getAll
import eu.ibagroup.formainframe.utils.isThe
import eu.ibagroup.formainframe.utils.isTheSameAs
import kotlin.streams.toList

/** Stateful class to represent the plugin configs sandbox */
data class SandboxState(
  val configState: ConfigState = ConfigState(),
  val credentials: MutableList<Credentials> = mutableListOf()
)

/**
 * Convert the provided class a the list of config rows
 * @param clazz the class to decide on which list of config rows will be returned
 * @param state the config sandbox state to get the list from
 */
@Suppress("UNCHECKED_CAST")
internal fun <T> classToList(clazz: Class<out T>, state: SandboxState): MutableList<T>? {
  return object : ConfigClassActionDecider<MutableList<T>?>() {

    override fun onConnectionConfig(): MutableList<T> {
      return state.configState.connections as MutableList<T>
    }

    override fun onFilesWorkingSetConfig(): MutableList<T> {
      return state.configState.filesWorkingSets as MutableList<T>
    }

    override fun onJesWorkingSetConfig(): MutableList<T> {
      return state.configState.jesWorkingSets as MutableList<T>
    }

    override fun onCredentials(): MutableList<T> {
      return state.credentials as MutableList<T>
    }

    override fun onElse(): MutableList<T>? {
      return null
    }

  }(clazz as Class<*>)
}

/**
 * Check is the sandbox modified for the specified config class
 * @param clazz the class to check are the configs of this class modified
 */
fun <T> isSandboxModified(clazz: Class<out T>): Boolean {
  return ConfigSandbox.instance.isModified(clazz)
}

/**
 * Check is the sandbox modified for the specified config class
 * @param clazz the class to check are the configs of this class modified
 */
inline fun <reified T> isSandboxModified(): Boolean {
  return isSandboxModified(T::class.java)
}

/**
 * Rollback the config sandbox instance to have the values from the config service
 * @param clazz the class of configs to rollback in the config sandbox
 */
fun <T> rollbackSandbox(clazz: Class<out T>) {
  ConfigSandbox.instance.rollback(clazz)
}

/**
 * Rollback the config sandbox instance to have the values from the config service
 * @param clazz the class of configs to rollback in the config sandbox
 */
inline fun <reified T> rollbackSandbox() {
  rollbackSandbox(T::class.java)
}

/**
 * Config sandbox implementation class.
 * Provides functions to manipulate a config sandbox state
 */
class ConfigSandboxImpl : ConfigSandbox {

  /** Weak state that could be applied to the actual config sandbox state */
  private var state = SandboxState()
    get() = synchronized(stateLock) { field }
    private set(value) = synchronized(stateLock) { field = value }

  /** The actual config sandbox state */
  private var initialState = SandboxState()

  private val stateLock = Any()

  private val eventHandler = object : ReloadableEventHandler {

    /**
     * Trigger "update" event on SandboxListener topic that updates the configs rows of the provided row class
     * @param rowClass the row class to get the config rows to update
     */
    override fun onEvent(rowClass: Class<*>, row: Any) {
      ApplicationManager.getApplication()
        .messageBus
        .syncPublisher(SandboxListener.TOPIC)
        .update(rowClass)
    }

    /**
     * Trigger "reload" event on SandboxListener topic that reloads the config rows of the provided row class
     * @param rowClass the row class to get the config rows to reload
     */
    override fun onReload(rowClass: Class<*>) {
      ApplicationManager.getApplication()
        .messageBus
        .syncPublisher(SandboxListener.TOPIC)
        .reload(rowClass)
    }

  }

  /** Fully initialized config sandbox crudable object */
  override val crudable = makeCrudableWithoutListeners(true, { state.credentials }) { state.configState }
    .configureCrudable {
      eventHandler = this@ConfigSandboxImpl.eventHandler
    }

  /**
   * Check is the weak state modified for the specified config class
   * @param clazz the class to check are the configs of this class modified
   */
  override fun <T> isModified(clazz: Class<out T>): Boolean {
    return synchronized(stateLock) {
      val initial = classToList(clazz, initialState) ?: listOf()
      val current = classToList(clazz, state) ?: listOf()
      val res = !(initial isTheSameAs current)
      res
    }
  }

  /**
   * Apply the weak state to the config if the weak state is modified
   * @param clazz the config class to get the configs to apply the weak state to
   */
  override fun <T : Any> apply(clazz: Class<out T>) {
    synchronized(stateLock) {
      if (isModified(clazz)) {
        classToList(clazz, state)
          ?.let { list ->
            if (clazz.isThe<Credentials>()) {
              with(Crudable.mergeCollections(initialState.credentials, state.credentials)) {
                val credentialService = CredentialService.instance
                listOf(toAdd, toUpdate)
                  .flatten()
                  .forEach { credentialService.setCredentials(it.connectionConfigUuid, it.username, it.password) }
                toDelete.forEach { credentialService.clearCredentials(it.connectionConfigUuid) }
              }
            } else {
              configCrudable.replaceGracefully(clazz, list.stream())
            }
          }
      }
    }
  }

  /** Fetch the config service values to the config sandbox for each config class */
  override fun fetch() {
    synchronized(stateLock) {
      rollbackSandbox<ConnectionConfig>()
      rollbackSandbox<FilesWorkingSetConfig>()
      rollbackSandbox<JesWorkingSetConfig>()
      rollbackSandbox<Credentials>()
    }
  }

  /**
   * Rollback the config sandbox instance to have the values from the config service.
   * Sets the current state along with the initial sandbox state.
   * If the Credentials class is provided to rollback, then all the credentials by connection configs will be fetched to use as the values for the Credentials objects.
   * Triggers the onReload event when the rollback is finished
   * @param clazz the class of configs to rollback in the config sandbox
   */
  @Suppress("UNCHECKED_CAST")
  override fun <T> rollback(clazz: Class<out T>) {
    synchronized(stateLock) {
      val current = if (clazz.isThe<Credentials>()) {
        configCrudable
          .getAll<ConnectionConfig>()
          .map {
            with(CredentialService.instance) {
              Credentials(it.uuid, getUsernameByKey(it.uuid) ?: "", getPasswordByKey(it.uuid) ?: "")
            }
          }
          .toList()
      } else {
        configCrudable.getAll(clazz).toList()
      }
      listOfNotNull(classToList(clazz, state), classToList(clazz, initialState))
        .forEach { list ->
          list.clear()
          list.addAll(
            current
              .mapNotNull { it?.clone(clazz) }
              .toMutableList() as MutableList<T>
          )
        }
      eventHandler.onReload(clazz)
    }
  }

  override fun updateState() {
    initialState = state.clone()
  }

}
