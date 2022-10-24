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
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.JesWorkingSetConfig
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.annotations.Contains

/**
 * Apply the sandbox state to the config if it is modified
 * @param clazz the config class to get the configs to apply the state to
 */
fun <T : Any> applySandbox(clazz: Class<out T>) {
  ConfigSandbox.instance.apply(clazz)
}

/**
 * Apply the sandbox state to the config if it is modified
 * @param clazz the config class to get the configs to apply the state to
 */
inline fun <reified T : Any> applySandbox() {
  applySandbox(T::class.java)
}

/** Interface to describe the config sandbox and possible ways to work with its state */
interface ConfigSandbox {

  companion object {
    @JvmStatic
    val instance: ConfigSandbox
      get() = ApplicationManager.getApplication().getService(ConfigSandbox::class.java)
  }

  fun updateState()

  fun <T : Any> apply(clazz: Class<out T>)

  fun fetch()

  fun <T> rollback(clazz: Class<out T>)

  fun <T> isModified(clazz: Class<out T>): Boolean

  @get:Contains(
    entities = [
      FilesWorkingSetConfig::class,
      ConnectionConfig::class,
      JesWorkingSetConfig::class
    ]
  )
  val crudable: Crudable

}

val sandboxCrudable get() = ConfigSandbox.instance.crudable
