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

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.options.Configurable
import eu.ibagroup.formainframe.utils.crudable.Crudable

interface ConfigDeclarationFactory {
  fun buildConfigDeclaration(crudable: Crudable): ConfigDeclaration<*>
}

abstract class ConfigDeclaration<T: Any>(val crudable: Crudable) {

  abstract val clazz: Class<out T>
  open val useCredentials: Boolean = false
  abstract val configPriority: Double

  companion object {
    val EP = ExtensionPointName.create<ConfigDeclarationFactory>("eu.ibagroup.formainframe.configDeclaration")
  }

  abstract class ConfigDecider<Config> {
    abstract fun canAdd(row: Config): Boolean
    abstract fun canUpdate(currentRow: Config, updatingRow: Config): Boolean
  }

  abstract fun getDecider(): ConfigDecider<T>

  open fun getConfigurable(): Configurable? = null
}