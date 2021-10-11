/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.vfs

private val encodeSplitRegex = Regex(
  "((?<=\\[)|(?=\\[)|(?<=])|(?=])|(?<=\\|)|(?=\\|))"
)

private val decodeSplitRegex = Regex(
  "((?<=\\[5B])|(?=\\[5B])|(?<=(\\[7C]))|(?=(\\[7C]))|(?<=(\\[5D]))|(?=(\\[5D])))"
)

fun encodeFilename(toEncode: String): String {
  return toEncode.split(encodeSplitRegex).joinToString(separator = "") {
    when (it) {
      "[" -> "[5B]"
      "]" -> "[5D]"
      "|" -> "[7C]"
      else -> it
    }
  }
}

fun decodeFilename(toDecode: String): String {
  return toDecode.split(decodeSplitRegex).joinToString(separator = "") {
    when (it) {
      "[5B]" -> "["
      "[5D]" -> "]"
      "[7C]" -> "|"
      else -> it
    }
  }
}

