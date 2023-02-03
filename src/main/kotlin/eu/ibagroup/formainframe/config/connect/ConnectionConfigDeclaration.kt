/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.config.connect

import com.intellij.openapi.options.Configurable
import eu.ibagroup.formainframe.config.ConfigDeclaration
import eu.ibagroup.formainframe.config.ConfigDeclarationFactory
import eu.ibagroup.formainframe.config.connect.ui.ConnectionConfigurable
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.getByColumnLambda

class ConnectionConfigDeclarationFactory : ConfigDeclarationFactory {
  override fun buildConfigDeclaration(crudable: Crudable): ConfigDeclaration<*> {
    return ConnectionConfigDeclaration(crudable)
  }
}

class ConnectionConfigDeclaration(crudable: Crudable) :
  ConfigDeclaration<ConnectionConfig>(crudable) {

  override val clazz = ConnectionConfig::class.java
  override val useCredentials = true
  override val configPriority = 1.0

  override fun getDecider(): ConfigDecider<ConnectionConfig> {
    return object : ConfigDecider<ConnectionConfig>() {
      override fun canAdd(row: ConnectionConfig): Boolean {
        return crudable.getByColumnLambda(row) { it.name }.count() == 0L
      }

      override fun canUpdate(currentRow: ConnectionConfig, updatingRow: ConnectionConfig): Boolean {
        return canAdd(updatingRow)
            || updatingRow.name == currentRow.name
            || updatingRow.zVersion == currentRow.zVersion
            || updatingRow.url == currentRow.url
            || updatingRow.isAllowSelfSigned == currentRow.isAllowSelfSigned
      }

    }
  }

  override fun getConfigurable(): Configurable = ConnectionConfigurable()

}