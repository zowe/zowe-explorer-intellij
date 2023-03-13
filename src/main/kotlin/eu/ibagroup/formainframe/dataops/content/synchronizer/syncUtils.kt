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

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.encoding.EncodingManager
import eu.ibagroup.formainframe.utils.runWriteActionInEdtAndWait
import java.nio.charset.Charset

private const val NEW_LINE = "\n"

val DEFAULT_TEXT_CHARSET: Charset = Charset.forName("ISO8859_1")

val DEFAULT_BINARY_CHARSET: Charset = Charset.forName("IBM-1047")

const val LF_LINE_SEPARATOR: String = "\n"

const val CR_LINE_SEPARATOR: String = "\r"

enum class ContentEncodingMode(val value: String) {
  CONVERT("CONVERT"),
  RELOAD("RELOAD")
}

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

/**
 * Dialog for selecting content encoding mode (reload, convert or cancel).
 * @param fileName name of the file.
 * @param encodingName name of the file encoding.
 * @param project the project to show dialog.
 * @return content encoding mode [ContentEncodingMode] or null.
 */
fun showReloadConvertCancelDialog(fileName: String, encodingName: String, project: Project?): ContentEncodingMode? {
  val result = Messages.showDialog(
    project,
    "The encoding you've chosen ('${encodingName}') may change the contents of '${fileName}'.<br>"
        + "Do you want to<br>"
        + "1. <b>Reload</b> the file from remote in the new encoding '${encodingName}' and overwrite contents " +
        "(may not display correctly) or<br>"
        + "2. <b>Convert</b> the text and overwrite file in the new encoding?<br>",
    "${fileName}: Reload or Convert to ${encodingName}",
    arrayOf("Reload", "Convert", "Cancel"),
    2,
    AllIcons.General.QuestionDialog,
    null
  )
  return when (result) {
    0 -> ContentEncodingMode.RELOAD
    1 -> ContentEncodingMode.CONVERT
    else -> null
  }
}

/**
 * Dialog for selecting content encoding mode (reload or cancel).
 * @param fileName name of the file.
 * @param encodingName name of the file encoding.
 * @param project the project to show dialog.
 * @return content encoding mode [ContentEncodingMode] or null.
 */
fun showReloadCancelDialog(fileName: String, encodingName: String, project: Project?): ContentEncodingMode? {
  val result = Messages.showDialog(
    project,
    "The encoding you've chosen ('${encodingName}') may change the contents of '${fileName}'.<br>"
        + "Do you want to <b>Reload</b> the file from remote in the new encoding '${encodingName}' and overwrite contents " +
        "(may not display correctly).<br>",
    "${fileName}: Reload to ${encodingName}",
    arrayOf("Reload", "Cancel"),
    1,
    AllIcons.General.QuestionDialog,
    null
  )
  return when (result) {
    0 -> ContentEncodingMode.RELOAD
    else -> null
  }
}

/** Initializes the line separator to the contents of the file (by default "\n"). */
fun initLineSeparator(file: VirtualFile) {
  if (file.contentsToByteArray().isEmpty()) {
    file.detectedLineSeparator = LF_LINE_SEPARATOR
  }
  file.detectedLineSeparator = LoadTextUtil.detectLineSeparator(file, true)
}

/** Changes the file encoding to the specified one. */
fun changeFileEncodingTo(file: VirtualFile, charset: Charset) {
  runWriteActionInEdtAndWait {
    EncodingManager.getInstance().setEncoding(file, charset)
  }
}