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

import com.intellij.util.xmlb.annotations.Attribute
import java.util.*

/**
 * Class that represents an entity with UUID
 * @property uuid the UUID of the entity
 */
abstract class EntityWithUuid(
  @get:Attribute
  var uuid: String = EMPTY_ID
) {

  init {
    if (uuid == EMPTY_ID) {
      uuid = UUID.randomUUID().toString()
    }
  }

  companion object {
    const val EMPTY_ID = ""
  }

}
