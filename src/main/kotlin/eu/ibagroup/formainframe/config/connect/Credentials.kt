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

import eu.ibagroup.formainframe.utils.crudable.annotations.Column

/**
 * Class which represents credentials.
 * Instances of this class are saved and can be reloaded after Intellij closed
 */
class Credentials {
  @Column(unique = true)
  var configUuid = ""

  @Column
  var username = ""

  @Column
  var password: CharArray = charArrayOf()

  constructor()
  constructor(connectionConfigUuid: String, username: String, password: CharArray) {
    this.configUuid = connectionConfigUuid
    this.username = username
    this.password = password
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false
    val that = other as Credentials
    if (configUuid != that.configUuid) return false
    return username == that.username && password.contentEquals(that.password)
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + configUuid.hashCode()
    result = 31 * result + username.hashCode()
    result = 31 * result + password.contentHashCode()
    return result
  }

  override fun toString(): String {
    return "Credentials{" +
        "connectionConfigUuid='" + configUuid + '\'' +
        ", username='" + username + '\'' +
        ", password='" + password + '\'' +
        '}'
  }
}
