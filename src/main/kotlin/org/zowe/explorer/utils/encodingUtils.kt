/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.utils

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.encoding.EncodingManager
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.content.synchronizer.DocumentedSyncProvider
import org.zowe.explorer.dataops.content.synchronizer.SaveStrategy
import java.nio.charset.Charset

/**
 * Save document file content in new encoding (convert).
 * @param project the project in which the file is open.
 * @param virtualFile file to change encoding.
 * @param charset new encoding.
 */
fun saveIn(project: Project?, virtualFile: VirtualFile, charset: Charset) {
  val syncProvider = DocumentedSyncProvider(virtualFile)
  runWriteActionInEdtAndWait {
    syncProvider.saveDocument()
    EncodingManager.getInstance().setEncoding(virtualFile, charset)
    virtualFile.charset = charset
    val document = syncProvider.getDocument()
    document?.let {
      LoadTextUtil.write(
        project,
        virtualFile,
        virtualFile,
        document.text,
        document.modificationStamp
      )
    }
  }
}

/**
 * Reload document file content in new encoding (reload).
 * @param project the project in which the file is open.
 * @param virtualFile file to change encoding.
 * @param charset new encoding.
 */
fun reloadIn(project: Project?, virtualFile: VirtualFile, charset: Charset) {
  val syncProvider = DocumentedSyncProvider(virtualFile, SaveStrategy.syncOnOpen(project))
  runWriteActionInEdtAndWait {
    syncProvider.saveDocument()
    EncodingManager.getInstance().setEncoding(virtualFile, charset)
    val contentSynchronizer = service<DataOpsManager>().getContentSynchronizer(virtualFile)
    contentSynchronizer?.synchronizeWithRemote(syncProvider)
    LoadTextUtil.clearCharsetAutoDetectionReason(virtualFile)
  }
}

/** Changes the file encoding to the specified one. */
fun changeFileEncodingTo(file: VirtualFile, charset: Charset) {
  runWriteActionInEdtAndWait {
    file.charset = charset
    EncodingManager.getInstance().setEncoding(file, charset)
  }
}

enum class ContentEncodingMode(val value: String) {
  CONVERT("CONVERT"),
  RELOAD("RELOAD")
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
    "${fileName}: Reload or Convert to $encodingName",
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
    "${fileName}: Reload to $encodingName",
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
