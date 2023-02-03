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

class FilesWorkingSetConfigDeclarationFactory : ConfigDeclarationFactory {

  override fun buildConfigDeclaration(crudable: Crudable): ConfigDeclaration<*> {
    return object : WorkingSetConfigDeclaration<FilesWorkingSetConfig>(crudable, FilesWorkingSetConfig::class.java) {
      override val configPriority = 3.0
      override fun getConfigurable() = FilesWSConfigurable()
    }
  }

}

class JesWorkingSetConfigDeclarationFactory : ConfigDeclarationFactory {
  override fun buildConfigDeclaration(crudable: Crudable): ConfigDeclaration<*> {
    return object : WorkingSetConfigDeclaration<JesWorkingSetConfig>(crudable, JesWorkingSetConfig::class.java) {
      override val configPriority = 2.0
      override fun getConfigurable() = JesWsConfigurable()
    }
  }
}

abstract class WorkingSetConfigDeclaration<WS : WorkingSetConfig>(
  crudable: Crudable,
  override val clazz: Class<out WS>
) : ConfigDeclaration<WS>(crudable) {

  override fun getDecider(): ConfigDecider<WS> {
    return object : ConfigDecider<WS>() {
      override fun canAdd(row: WS): Boolean {
        return crudable.find(clazz) { it.name == row.name }.count() == 0L
      }

      override fun canUpdate(currentRow: WS, updatingRow: WS): Boolean {
        return canAdd(updatingRow) || updatingRow.name == currentRow.name
      }
    }
  }

}