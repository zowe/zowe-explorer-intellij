/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */
package org.zowe.explorer.utils.crudable

import org.zowe.explorer.utils.crudable.annotations.Column

/** Class that represents the entity with UUID */
open class EntityWithUuid {
  @Column(unique = true)
  var uuid = EMPTY_ID

  constructor()
  constructor(uuid: String) {
    this.uuid = uuid
  }

  override fun hashCode(): Int {
    return uuid.hashCode()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false
    val that = other as EntityWithUuid
    return uuid == that.uuid
  }

  override fun toString(): String {
    return "EntityWithUuid{" +
      "uuid='" + uuid + '\'' +
      '}'
  }

  companion object {
    const val EMPTY_ID = ""
  }
}
