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
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.JobsWorkingSetConfig
import org.zowe.explorer.config.ws.FilesWorkingSetConfig
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.utils.crudable.annotations.Contains

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
      JobsWorkingSetConfig::class
    ]
  )
  val crudable: Crudable

}

val sandboxCrudable get() = ConfigSandbox.instance.crudable

fun <T: Any> applySandbox(clazz: Class<out T>) {
  ConfigSandbox.instance.apply(clazz)
}

inline fun <reified T : Any> applySandbox() {
  applySandbox(T::class.java)
}
