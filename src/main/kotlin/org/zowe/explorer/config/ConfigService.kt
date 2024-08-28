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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.util.messages.Topic
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.tso.config.TSOSessionConfig
import org.zowe.explorer.config.ws.FilesWorkingSetConfig
import org.zowe.explorer.config.ws.JesWorkingSetConfig
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.utils.crudable.EventHandler
import org.zowe.explorer.utils.crudable.annotations.Contains
import org.zowe.explorer.utils.sendTopic
import java.time.Duration

fun sendConfigServiceTopic(): EventHandler = sendTopic(CONFIGS_CHANGED)

@JvmField
val CONFIGS_CHANGED = Topic.create("configsChanged", EventHandler::class.java)

/** Interface to represent the config service */
interface ConfigService : PersistentStateComponent<ConfigStateV2> {

  companion object {
    @JvmStatic
    val instance: ConfigService
      get() = ApplicationManager.getApplication().getService(ConfigService::class.java)
  }

  @get:Contains(
    entities = [
      FilesWorkingSetConfig::class,
      ConnectionConfig::class,
      JesWorkingSetConfig::class,
      TSOSessionConfig::class
    ]
  )
  val crudable: Crudable

  val eventHandler: EventHandler

  val autoSaveDelay: Duration

  /** Identifies if the auto sync is used to save file content. */
  var isAutoSyncEnabled: Boolean

  /** Identifies size of the files batch to fetch in a single request. */
  var batchSize: Int

  /**
   * Finds [ConfigDeclaration] for specified class through registered extension points.
   * @param rowClass config class instance for which to find [ConfigDeclaration].
   * @return config declaration instance.
   */
  fun <T : Any> getConfigDeclaration(rowClass: Class<out T>): ConfigDeclaration<T>

  /**
   * Creates collection in config state for specified class.
   * @param clazz config class instance for which to register collection.
   */
  fun <T> registerConfigClass(clazz: Class<out T>)

  /**
   * Walks through registered config declarations (see [ConfigDeclaration])
   * and creates collection in config state for each of their config classes.
   */
  fun registerAllConfigClasses()

  /**
   * Walks through registered collections and gets corresponding config classes for them.
   * @return list of registered in state config classes (see [ConfigDeclaration])
   */
  fun getRegisteredConfigClasses(): List<Class<*>>

  /**
   * Gets all registered config declarations (see [ConfigDeclaration]).
   * @return list of all registered config declarations.
   */
  fun getRegisteredConfigDeclarations(): List<ConfigDeclaration<*>>

}

val configCrudable: Crudable
  get() = ConfigService.instance.crudable
