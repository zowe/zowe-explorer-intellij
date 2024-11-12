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

package org.zowe.explorer.v3

import org.zowe.explorer.utils.crudable.annotations.Column
import org.zowe.kotlinsdk.annotations.ZVersion

/**
 * Connection config based class. Could be used as a basic connection config representation or as a base for other
 * connection config type
 * @param uuid the unique UUID of the entity
 * @param name the name of the connection config
 * @param url the URL for the connection config
 * @param isAllowSelfSigned to indicate whether it is allowed to use self-signed certificates during a connection
 * @param zVersion the version of the z/OS being used for the connection
 * @param owner the actual USS user related to the USS user provided in the connection to work with in the USS part
 */
open class ConnectionConfig(
  uuid: String = EMPTY_ID,
  @Column var name: String = "",
  @Column var url: String = "",
  @Column var isAllowSelfSigned: Boolean = true,
  @Column var zVersion: ZVersion = ZVersion.ZOS_2_3,
  @Column var owner: String = ""
) : EntityWithUuid(uuid) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false

    other as ConnectionConfig

    if (name != other.name) return false
    if (url != other.url) return false
    if (isAllowSelfSigned != other.isAllowSelfSigned) return false
    if (zVersion != other.zVersion) return false
    if (owner != other.owner) return false

    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + name.hashCode()
    result = 31 * result + url.hashCode()
    result = 31 * result + isAllowSelfSigned.hashCode()
    result = 31 * result + zVersion.hashCode()
    result = 31 * result + owner.hashCode()
    return result
  }

  override fun toString(): String {
    return "ConnectionConfig(name='$name', url='$url', isAllowSelfSigned=$isAllowSelfSigned, zVersion=$zVersion, owner=$owner)"
  }

}
