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

package eu.ibagroup.formainframe.tso.config

import com.intellij.openapi.options.BoundSearchableConfigurable
import eu.ibagroup.formainframe.config.ConfigDeclaration
import eu.ibagroup.formainframe.config.ConfigDeclarationFactory
import eu.ibagroup.formainframe.tso.config.ui.TSOSessionConfigurable
import eu.ibagroup.formainframe.utils.crudable.Crudable

/**
 * Factory to create instance of [TSOSessionConfigDeclaration]
 */
class TSOSessionConfigDeclarationFactory: ConfigDeclarationFactory {
  override fun buildConfigDeclaration(): ConfigDeclaration<*> {
    return TSOSessionConfigDeclaration()
  }
}

/**
 * Declaration for TSO session config which describes the logic for working with the config
 */
class TSOSessionConfigDeclaration() : ConfigDeclaration<TSOSessionConfig>() {

  override val clazz = TSOSessionConfig::class.java
  override val configPriority = 4.0

  override fun getDecider(crudable: Crudable): ConfigDecider<TSOSessionConfig> {
    return object: ConfigDecider<TSOSessionConfig>() {

      /**
       * Enables to add TSO session config only if no existing session with such name found
       */
      override fun canAdd(row: TSOSessionConfig): Boolean {
        return crudable.find(clazz) { it.name == row.name }.count() == 0L
      }

      /**
       * Enables to update TSO session only if session with such name
       * exists or if names of current and updating sessions are equal.
       */
      override fun canUpdate(currentRow: TSOSessionConfig, updatingRow: TSOSessionConfig): Boolean {
        return canAdd(updatingRow) || updatingRow.name == currentRow.name
      }

    }
  }

  override fun getConfigurable(): BoundSearchableConfigurable {
    return TSOSessionConfigurable()
  }

}
