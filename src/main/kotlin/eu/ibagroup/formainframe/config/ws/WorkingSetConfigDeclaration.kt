/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.config.ws

import eu.ibagroup.formainframe.config.ConfigDeclaration
import eu.ibagroup.formainframe.config.ConfigDeclarationFactory
import eu.ibagroup.formainframe.config.ws.ui.files.FilesWSConfigurable
import eu.ibagroup.formainframe.config.ws.ui.jes.JesWsConfigurable
import eu.ibagroup.formainframe.utils.crudable.Crudable

/**
 * Instance to build implementation of [WorkingSetConfigDeclaration] for [FilesWorkingSetConfig].
 * @author Valiantsin Krus
 */
class FilesWorkingSetConfigDeclarationFactory : ConfigDeclarationFactory {

  override fun buildConfigDeclaration(): ConfigDeclaration<*> {
    return object : WorkingSetConfigDeclaration<FilesWorkingSetConfig>(FilesWorkingSetConfig::class.java) {
      override val configPriority = 3.0
      override fun getConfigurable() = FilesWSConfigurable()
    }
  }

}

/**
 * Instance to build implementation of [WorkingSetConfigDeclaration] for [JesWorkingSetConfig].
 * @author Valiantsin Krus
 */
class JesWorkingSetConfigDeclarationFactory : ConfigDeclarationFactory {
  override fun buildConfigDeclaration(): ConfigDeclaration<*> {
    return object : WorkingSetConfigDeclaration<JesWorkingSetConfig>(JesWorkingSetConfig::class.java) {
      override val configPriority = 2.0
      override fun getConfigurable() = JesWsConfigurable()
    }
  }
}

/**
 * Abstract class with wrapped logic of working with working sets configs.
 * @param clazz instance of class that implements [WorkingSetConfig].
 * @author Valiantsin Krus.
 */
abstract class WorkingSetConfigDeclaration<WS : WorkingSetConfig>(
  override val clazz: Class<out WS>
) : ConfigDeclaration<WS>() {

  override fun getDecider(crudable: Crudable): ConfigDecider<WS> {
    return object : ConfigDecider<WS>() {

      /**
       * Enables to add working set config only if no existing working set with such name found.
       * @param row [WorkingSetConfig] instance to add.
       * @return true if no existing working set with such name found and false otherwise.
       */
      override fun canAdd(row: WS): Boolean {
        return crudable.find(clazz) { it.name == row.name }.count() == 0L
      }

      /**
       * Enables to update working set only if connection with such name
       * exists or if names of current and updating working sets are equal.
       * @param currentRow working set config instance that should be updated.
       * @param updatingRow working set config to replace current working set config.
       * @return true if working set with such name exists or if names of
       *         existing and updating working sets are equal and false otherwise.
       */
      override fun canUpdate(currentRow: WS, updatingRow: WS): Boolean {
        return canAdd(updatingRow) || updatingRow.name == currentRow.name
      }
    }
  }

}