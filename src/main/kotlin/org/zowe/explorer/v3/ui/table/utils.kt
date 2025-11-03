/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   Dzianis Lisiankou
 */

package org.zowe.explorer.v3.ui.table

import org.zowe.explorer.config.connect.USER_OR_OWNER_SYMBOLS_MAX_SIZE
import org.zowe.explorer.utils.clone

fun <Item: Any> Item?.cloneAndWrap(clazz: Class<out Item>): List<Item> {
  val clonedItem = this?.clone(clazz)
  return if (clonedItem != null) {
    listOf(clonedItem)
  } else listOf()
}

fun <Item: Any> List<Item>.cloneElements(clazz: Class<out Item>): List<Item> {
  return mapNotNull { it.clone(clazz) }
}

/**
 * Returns owner of particular connection config if the owner field is not empty or conforms
 * the [USER_OR_OWNER_SYMBOLS_MAX_SIZE] in length, or the empty string otherwise.
 * If whoAmI function failed for some reason it could contain empty "" or error string inside owner variable.
 * If it is such case, then it returns an empty string.
 * @param possibleOwner
 * @return owner of the connection config if the owner is present, empty string otherwise
 */
fun getOwner(possibleOwner: String): String {
  return if (possibleOwner.isNotEmpty() && possibleOwner.length <= USER_OR_OWNER_SYMBOLS_MAX_SIZE) {
    possibleOwner
  } else {
    ""
  }
}