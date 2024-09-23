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

package eu.ibagroup.formainframe.testutils.testServiceImpl

import eu.ibagroup.formainframe.config.connect.CredentialService

open class TestCredentialsServiceImpl : CredentialService {
  var testInstance = object : CredentialService {
    override fun getUsernameByKey(connectionConfigUuid: String): String {
      return "testUser"
    }

    override fun getPasswordByKey(connectionConfigUuid: String): String {
      return "testPassword"
    }

    override fun setCredentials(connectionConfigUuid: String, username: String, password: String) {
    }

    override fun clearCredentials(connectionConfigUuid: String) {
    }

  }

  override fun getUsernameByKey(connectionConfigUuid: String): String? {
    return this.testInstance.getUsernameByKey(connectionConfigUuid)
  }

  override fun getPasswordByKey(connectionConfigUuid: String): String? {
    return this.testInstance.getPasswordByKey(connectionConfigUuid)
  }

  override fun setCredentials(connectionConfigUuid: String, username: String, password: String) {
    this.testInstance.setCredentials(connectionConfigUuid, username, password)
  }

  override fun clearCredentials(connectionConfigUuid: String) {
    this.testInstance.clearCredentials(connectionConfigUuid)
  }
}
