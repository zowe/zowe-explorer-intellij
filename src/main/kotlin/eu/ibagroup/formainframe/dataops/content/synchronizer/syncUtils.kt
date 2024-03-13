/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.dataops.content.synchronizer

import java.nio.charset.Charset

private const val NEW_LINE = "\n"

val DEFAULT_TEXT_CHARSET: Charset = Charset.forName("ISO8859_1")

val DEFAULT_BINARY_CHARSET: Charset = Charset.forName("IBM-1047")

/** Remove string's last blank line */
fun String.removeLastNewLine(): String {
  return if (endsWith(NEW_LINE)) {
    removeSuffix(NEW_LINE)
  } else {
    this
  }
}

/** Remove last blank line in byte array by converting it to string and back again */
fun ByteArray.removeLastNewLine(): ByteArray {
  return toString(DEFAULT_TEXT_CHARSET)
    .removeLastNewLine()
    .toByteArray(DEFAULT_TEXT_CHARSET)
}

/** Add new blank line to the string */
fun ByteArray.addNewLine(): ByteArray {
  return this.plus(NEW_LINE.toByteArray())
}

// TODO: Remove when it becomes possible to mock kotlin inline function.
/** Wrapper for [String.toByteArray] function. It is necessary only for test purposes for now. */
fun String.convertToByteArray(charset: Charset = Charsets.UTF_8): ByteArray {
  return toByteArray(charset)
}
