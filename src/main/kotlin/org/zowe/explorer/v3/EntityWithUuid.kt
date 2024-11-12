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

/**
 * Class that represents an entity with UUID
 * @param uuid the UUID of the entity
 */
abstract class EntityWithUuid(@Column(unique = true) var uuid: String = EMPTY_ID) {

  companion object {
    const val EMPTY_ID = ""
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
    return "EntityWithUuid{uuid='$uuid'}"
  }

}
