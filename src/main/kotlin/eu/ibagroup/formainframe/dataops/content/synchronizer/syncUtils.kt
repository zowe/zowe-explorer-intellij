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

import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.utils.runWriteActionInEdtAndWait
import java.nio.charset.Charset

private const val NEW_LINE = "\n"

val DEFAULT_TEXT_CHARSET: Charset = Charset.forName("ISO8859_1")

val DEFAULT_BINARY_CHARSET: Charset = Charset.forName("IBM-1047")

const val LF_LINE_SEPARATOR: String = "\n"

const val CR_LINE_SEPARATOR: String = "\r"

/** Remove string's last blank line */
fun String.removeLastNewLine(): String {
  return if (endsWith(NEW_LINE)) {
    removeSuffix(NEW_LINE)
  } else {
    this
  }
}

fun ByteArray.removeLastNewLine(): ByteArray {
  return if (last() == NEW_LINE.toByte()) {
    dropLast(1).toByteArray()
  } else {
    this
  }
}

/** Add new blank line to the string */
fun ByteArray.addNewLine(): ByteArray {
  return this.plus(NEW_LINE.toByteArray())
}

/** Initializes the line separator to the contents of the file (by default "\n"). */
fun initLineSeparator(file: VirtualFile, project: Project? = null) {
  runWriteActionInEdtAndWait {
    LoadTextUtil.changeLineSeparators(
      project,
      file,
      LoadTextUtil.detectLineSeparator(file, true) ?: LF_LINE_SEPARATOR,
      file
    )
  }
}