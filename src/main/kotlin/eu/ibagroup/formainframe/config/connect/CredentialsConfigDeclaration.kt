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

import eu.ibagroup.formainframe.config.ConfigDeclaration
import eu.ibagroup.formainframe.config.ConfigDeclarationFactory
import eu.ibagroup.formainframe.utils.crudable.Crudable

/**
 * Factory to create instance of [CredentialsConfigDeclaration].
 * @author Valiantsin Krus
 */
class CredentialsConfigDeclarationFactory: ConfigDeclarationFactory {
  override fun buildConfigDeclaration(crudable: Crudable): ConfigDeclaration<*> {
    return CredentialsConfigDeclaration(crudable)
  }
}

/**
 * Declares config to work with credentials. It is the only class that is necessary to declare without
 * any logical load. All the logic of storing credentials securely is described in [CredentialService].
 * @param crudable instance of [Crudable] (not used in this class).
 * @author Valiantsin Krus
 */
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
