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

package org.zowe.explorer.config

import com.intellij.openapi.components.service
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.FilesWorkingSetConfig
import org.zowe.explorer.config.ws.JesWorkingSetConfig
import org.zowe.explorer.tso.config.TSOSessionConfig
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.utils.crudable.annotations.Contains

/** Interface to describe the config sandbox and possible ways to work with its state */
interface ConfigSandbox {

  companion object {
    @JvmStatic
    fun getService(): ConfigSandbox = service()
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

  /**
   * Creates collection in config sandbox for specified class.
   * @param clazz config class instance for which to register collection.
   */
  fun <T> registerConfigClass(clazz: Class<out T>)

  fun updateState()

  fun <T : Any> apply(clazz: Class<out T>)

  fun fetch()

  fun <T> rollback(clazz: Class<out T>)

  fun <T> isModified(clazz: Class<out T>): Boolean

}
