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

import eu.ibagroup.formainframe.utils.crudable.EntityWithUuid
import eu.ibagroup.formainframe.utils.crudable.annotations.Column
import eu.ibagroup.r2z.annotations.ZVersion

/**
 * Class which represents connection config.
 * Instances of this class are saved and can be reloaded after Intellij closed.
 */
class ConnectionConfig : EntityWithUuid {

  @Column
  var name = ""

  @Column
  var url = ""

  @Column
  var isAllowSelfSigned = true

  @Column
  var zVersion = ZVersion.ZOS_2_1

  constructor() {}

  constructor(
    uuid: String,
    name: String,
    url: String,
    isAllowSelfSigned: Boolean,
    zVersion: ZVersion
  ) : super(uuid) {
    this.name = name
    this.url = url
    this.isAllowSelfSigned = isAllowSelfSigned
    this.zVersion = zVersion
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false

    other as ConnectionConfig

    if (name != other.name) return false
    if (url != other.url) return false
    if (isAllowSelfSigned != other.isAllowSelfSigned) return false
    if (zVersion != other.zVersion) return false

    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + name.hashCode()
    result = 31 * result + url.hashCode()
    result = 31 * result + isAllowSelfSigned.hashCode()
    result = 31 * result + zVersion.hashCode()
    return result
  }

  override fun toString(): String {
    return "ConnectionConfig(name='$name', url='$url', isAllowSelfSigned=$isAllowSelfSigned, zVersion=$zVersion)"
  }

}
