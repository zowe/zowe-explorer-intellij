/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.config.connect

import org.zowe.explorer.utils.crudable.EntityWithUuid
import org.zowe.explorer.utils.crudable.annotations.Column
import eu.ibagroup.r2z.CodePage
import eu.ibagroup.r2z.annotations.ZVersion

class ConnectionConfig : EntityWithUuid {
  @Column
  var name = ""

  @Column
  var url = ""

  @Column
  var isAllowSelfSigned = true

  @Column
  var codePage = CodePage.IBM_1047

  @Column
  var zVersion = ZVersion.ZOS_2_1

  @Column
  var zoweConfigPath: String? = null

  constructor() {}

  constructor(
    uuid: String,
    name: String,
    url: String,
    isAllowSelfSigned: Boolean,
    codePage: CodePage,
    zVersion: ZVersion,
    zoweConfigPath: String? = null
  ) : super(uuid) {
    this.name = name
    this.url = url
    this.isAllowSelfSigned = isAllowSelfSigned
    this.codePage = codePage
    this.zVersion = zVersion
    this.zoweConfigPath = zoweConfigPath
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false

    other as ConnectionConfig

    if (name != other.name) return false
    if (url != other.url) return false
    if (isAllowSelfSigned != other.isAllowSelfSigned) return false
    if (codePage != other.codePage) return false
    if (zVersion != other.zVersion) return false
    if (zoweConfigPath != other.zoweConfigPath) return false

    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + name.hashCode()
    result = 31 * result + url.hashCode()
    result = 31 * result + isAllowSelfSigned.hashCode()
    result = 31 * result + codePage.hashCode()
    result = 31 * result + zVersion.hashCode()
    result = 31 * result + zoweConfigPath.hashCode()
    return result
  }

  override fun toString(): String {
    return "ConnectionConfig(name='$name', url='$url', isAllowSelfSigned=$isAllowSelfSigned, codePage=$codePage, zVersion=$zVersion, zoweConfigPath=$zoweConfigPath)"
  }


}
