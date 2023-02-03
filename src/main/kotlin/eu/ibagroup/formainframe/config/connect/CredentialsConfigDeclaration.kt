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

import eu.ibagroup.formainframe.config.ConfigDeclaration
import eu.ibagroup.formainframe.config.ConfigDeclarationFactory
import eu.ibagroup.formainframe.utils.crudable.Crudable

class CredentialsConfigDeclarationFactory: ConfigDeclarationFactory {
  override fun buildConfigDeclaration(crudable: Crudable): ConfigDeclaration<*> {
    return CredentialsConfigDeclaration(crudable)
  }
}

class CredentialsConfigDeclaration(crudable: Crudable): ConfigDeclaration<Credentials>(crudable) {

  override val clazz = Credentials::class.java
  override val configPriority = -100.0

  override fun getDecider(): ConfigDecider<Credentials> {
    return object: ConfigDecider<Credentials>() {
      override fun canUpdate(currentRow: Credentials, updatingRow: Credentials) = true
      override fun canAdd(row: Credentials) = true
    }
  }

}
