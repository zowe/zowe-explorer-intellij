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

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.content.service.areDependentFilesSyncingNow
import eu.ibagroup.formainframe.dataops.content.service.isFileSyncingNow
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

/**
 * Check if the file or dependent files are syncing now and show the warning dialog if they are.
 * @param project project to show dialog.
 * @param file virtual file to check.
 * @param checkDependentFiles flag to enable the check for dependent files, by default it is false.
 * @return true if the file or dependent files are syncing now and false otherwise.
 */
fun checkFileForSync(
  project: Project?, file: VirtualFile, checkDependentFiles: Boolean = false
): Boolean {
  val message: String
  val commonMessage = "You can't perform this action on the file <b>${file.name}</b>"
  val title = "Synchronization Is In Progress"
  val result = if (checkDependentFiles) {
    message = "$commonMessage because this file or dependent files are currently being synchronized"
    isFileSyncingNow(file) || areDependentFilesSyncingNow(file)
  } else {
    message = "$commonMessage because this file is currently being synchronized"
    isFileSyncingNow(file)
  }
  return if (result) {
    Messages.showWarningDialog(project, message, title)
    true
  } else false
}
