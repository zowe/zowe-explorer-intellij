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

package eu.ibagroup.formainframe.config.connect

import com.intellij.openapi.options.BoundSearchableConfigurable
import eu.ibagroup.formainframe.config.ConfigDeclaration
import eu.ibagroup.formainframe.config.ConfigDeclarationFactory
import eu.ibagroup.formainframe.config.connect.ui.zosmf.ZOSMFConnectionConfigurable
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.getByColumnLambda

/**
 * Factory to create instance of [ZOSMFConnectionConfigDeclaration].
 * @author Valiantsin Krus
 */
class ConnectionConfigDeclarationFactory : ConfigDeclarationFactory {
  override fun buildConfigDeclaration(crudable: Crudable): ConfigDeclaration<*> {
    return ZOSMFConnectionConfigDeclaration(crudable)
  }
}

/**
 * Declares connection config that will represent connection to zosmf.
 * @param crudable instance of [Crudable] through which to work with config data.
 * @author Valiantsin Krus
 */
class ZOSMFConnectionConfigDeclaration(crudable: Crudable) :
  ConnectionConfigDeclaration<ConnectionConfig>(crudable) {

  override val clazz = ConnectionConfig::class.java
  override val useCredentials = true
  override val configPriority = 1.0

  override fun getDecider(): ConfigDecider<ConnectionConfig> {
    return object : ConfigDecider<ConnectionConfig>() {
      /**
       * Enables to add connection config only if no existing connection with such name found.
       * @param row [ConnectionConfig] instance to add.
       * @return true if no existing connection with such name found and false otherwise.
       */
      override fun canAdd(row: ConnectionConfig): Boolean {
        return crudable.getByColumnLambda(row) { it.name }.count() == 0L
      }

      /**
       * Enables to update connection only if connection with such name exists or if some
       * properties of current connection config and updating connection config are equals.
       * @param currentRow current connection config instance that should be updated.
       * @param updatingRow connection config to replace current connection config.
       * @return true if connection with such name exists or if some properties of
       *         existing and updating connections are equal and false otherwise.
       */
      override fun canUpdate(currentRow: ConnectionConfig, updatingRow: ConnectionConfig): Boolean {
        return canAdd(updatingRow)
            || updatingRow.name == currentRow.name
            || updatingRow.zVersion == currentRow.zVersion
            || updatingRow.url == currentRow.url
            || updatingRow.isAllowSelfSigned == currentRow.isAllowSelfSigned
      }

    }
  }


  override fun getConnectionConfigurable(): BoundSearchableConfigurable {
    return ZOSMFConnectionConfigurable()
  }

}
