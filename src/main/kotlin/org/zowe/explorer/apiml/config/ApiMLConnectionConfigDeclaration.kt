/*
 * Copyright (c) 2024 IBA Group.
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

package org.zowe.explorer.apiml.config

import com.intellij.openapi.options.BoundSearchableConfigurable
import org.zowe.explorer.apiml.config.ui.ApiMLConnectionConfigurable
import org.zowe.explorer.config.ConfigDeclaration
import org.zowe.explorer.config.ConfigDeclarationFactory
import org.zowe.explorer.utils.crudable.Crudable

class ApiMLConnectionInfoConfigDeclarationFactory: ConfigDeclarationFactory {
  override fun buildConfigDeclaration(crudable: Crudable): ConfigDeclaration<*> {
    return ApiMLConnectionInfoConfigDeclaration(crudable)
  }
}

class ApiMLConnectionInfoConfigDeclaration(
  crudable: Crudable
) : ConfigDeclaration<ApiMLConnectionConfig>(crudable) {

  override val clazz = ApiMLConnectionConfig::class.java

  override val useCredentials = true

  override val configPriority = 5.0

  override fun getDecider(): ConfigDecider<ApiMLConnectionConfig> {
    return object: ConfigDecider<ApiMLConnectionConfig>() {
      override fun canAdd(row: ApiMLConnectionConfig): Boolean {
        return crudable.find(clazz) { it.name == row.name }.count() == 0L
      }

      override fun canUpdate(currentRow: ApiMLConnectionConfig, updatingRow: ApiMLConnectionConfig): Boolean {
        return canAdd(updatingRow) || currentRow.name == updatingRow.name
      }
    }
  }

  override fun getConfigurable(): BoundSearchableConfigurable {
    return ApiMLConnectionConfigurable()
  }

}