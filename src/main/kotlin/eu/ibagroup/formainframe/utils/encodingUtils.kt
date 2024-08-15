/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.utils

import com.intellij.CommonBundle
import com.intellij.codeInspection.InspectionEngine
import com.intellij.codeInspection.InspectionManager
import com.intellij.icons.AllIcons
import com.intellij.ide.IdeBundle
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.encoding.EncodingManager
import com.intellij.openapi.vfs.encoding.EncodingUtil.Magic8
import com.intellij.profile.codeInspection.InspectionProjectProfileManager
import com.intellij.psi.PsiManager
import com.intellij.xml.util.XmlStringUtil
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.content.synchronizer.DocumentedSyncProvider
import eu.ibagroup.formainframe.dataops.content.synchronizer.SaveStrategy
import eu.ibagroup.formainframe.explorer.ui.ChangeEncodingDialog
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import javax.swing.Icon

/**
 * Save document file content in new encoding (convert).
 * @param project the project in which the file is open.
 * @param virtualFile file to change encoding.
 * @param charset new encoding.
 */
fun saveIn(project: Project?, virtualFile: VirtualFile, charset: Charset) {
  val syncProvider = DocumentedSyncProvider(virtualFile, SaveStrategy.default(project))
  runWriteActionInEdtAndWait {
    syncProvider.saveDocument()
    val bytes = syncProvider.retrieveCurrentContent()
    changeEncodingTo(virtualFile, charset)
    virtualFile.getOutputStream(null).use {
      it.write(bytes)
    }
  }
}

/**
 * Reload document file content in new encoding (reload).
 * @param project the project in which the file is open.
 * @param virtualFile file to change encoding.
 * @param charset new encoding.
 * @param indicator progress indicator to reflect reloading process.
 */
fun reloadIn(project: Project?, virtualFile: VirtualFile, charset: Charset, indicator: ProgressIndicator?) {
  val syncProvider = DocumentedSyncProvider(virtualFile, SaveStrategy.syncOnOpen(project))
  val contentSynchronizer = DataOpsManager.instance.getContentSynchronizer(virtualFile)
  runWriteActionInEdtAndWait { changeEncodingTo(virtualFile, charset) }
  contentSynchronizer?.synchronizeWithRemote(syncProvider, indicator)
}

/** Changes the file encoding to the specified one. */
fun changeEncodingTo(file: VirtualFile, charset: Charset) {
  EncodingManager.getInstance().setEncoding(file, charset)
  file.charset = charset
}

/** Checks if it is safe to reload file in the specified encoding. */
fun isSafeToReloadIn(virtualFile: VirtualFile, text: String, bytes: ByteArray, charset: Charset): Magic8 {
  return try {
    val decoder = charset.newDecoder()
    val result = decoder.decode(ByteBuffer.wrap(bytes)).toString()
    val lineSeparator = FileDocumentManager.getInstance().getLineSeparator(virtualFile, null)
    val textToSave = StringUtil.convertLineSeparators(result, lineSeparator)
    if (StringUtil.equals(textToSave, text)) {
      Magic8.ABSOLUTELY
    } else {
      Magic8.WELL_IF_YOU_INSIST
    }
  } catch (e: Exception) {
    Magic8.NO_WAY
  }
}

/** Checks if it is safe to convert file to the specified encoding. */
fun isSafeToConvertTo(virtualFile: VirtualFile, text: String, charset: Charset): Magic8 {
  return try {
    val encoder = charset.newEncoder()
    val lineSeparator = FileDocumentManager.getInstance().getLineSeparator(virtualFile, null)
    val textToSave = StringUtilRt.convertLineSeparators(text, lineSeparator)
    encoder.encode(CharBuffer.wrap(textToSave))
    Magic8.ABSOLUTELY
  } catch (e: Exception) {
    Magic8.NO_WAY
  }
}

/** Data class that represents info about safe encoding change. */
data class EncodingInspection(
  val safeToReload: Magic8,
  val safeToConvert: Magic8
)

/**
 * Checks if it is safe to change file encoding to the specified one.
 * @see isSafeToReloadIn
 * @see isSafeToConvertTo
 */
fun inspectSafeEncodingChange(virtualFile: VirtualFile, charset: Charset): EncodingInspection {
  val contentSynchronizer = DataOpsManager.instance.getContentSynchronizer(virtualFile)
  val syncProvider = DocumentedSyncProvider(virtualFile)
  val fileNotSynced = contentSynchronizer?.isFileUploadNeeded(syncProvider) == true
  val text = syncProvider.getDocument()?.text
    ?: throw IllegalArgumentException("Cannot get document text")
  val bytes = if (fileNotSynced) syncProvider.retrieveCurrentContent()
  else contentSynchronizer?.successfulContentStorage(syncProvider)
    ?: throw IllegalArgumentException("Cannot get content bytes")
  val safeToReload = isSafeToReloadIn(virtualFile, text, bytes, charset)
  val safeToConvert = isSafeToConvertTo(virtualFile, text, charset)
  return EncodingInspection(safeToReload, safeToConvert)
}

/**
 * Change file encoding action that invokes the change encoding dialog.
 * @return true if changed or false otherwise.
 */
fun changeFileEncodingAction(
  project: Project?,
  virtualFile: VirtualFile,
  attributes: RemoteUssAttributes,
  charset: Charset
): Boolean {
  val encodingInspection = inspectSafeEncodingChange(virtualFile, charset)
  val safeToReload = encodingInspection.safeToReload
  val safeToConvert = encodingInspection.safeToConvert
  val dialog = ChangeEncodingDialog(project, virtualFile, attributes, charset, safeToReload, safeToConvert)
  dialog.show()
  return dialog.exitCode == ChangeEncodingDialog.RELOAD_EXIT_CODE || dialog.exitCode == ChangeEncodingDialog.CONVERT_EXIT_CODE
}

/**
 * Creates a group of actions to change the file encoding.
 * Checks if it is safe to change the encoding and sorts the actions by the possibility of change.
 */
fun createCharsetsActionGroup(virtualFile: VirtualFile, attributes: RemoteUssAttributes): DefaultActionGroup {
  val group = DefaultActionGroup()

  val action: (charset: Charset, icon: Icon?) -> DumbAwareAction = { charset, icon ->
    object : DumbAwareAction(charset.name(), null, icon) {
      override fun actionPerformed(e: AnActionEvent) {
        changeFileEncodingAction(e.project, virtualFile, attributes, charset)
      }

      override fun update(e: AnActionEvent) {
        e.presentation.icon = icon
        super.update(e)
      }

      override fun getActionUpdateThread() = ActionUpdateThread.BGT
    }
  }

  fun getActionPriority(action: DumbAwareAction): Int {
    return when (action.templatePresentation.icon) {
      AllIcons.General.Error -> 2
      AllIcons.General.Warning -> 1
      else -> 0
    }
  }

  val comparator = Comparator { action1: DumbAwareAction, action2: DumbAwareAction ->
    getActionPriority(action1) - getActionPriority(action2)
  }

  val sortedCharsetActions = getSupportedEncodings().filter { it != virtualFile.charset }
    .map {
      val encodingInspection = inspectSafeEncodingChange(virtualFile, it)
      val safeToReload = encodingInspection.safeToReload
      val safeToConvert = encodingInspection.safeToConvert
      val icon = if (safeToConvert == Magic8.NO_WAY && safeToReload == Magic8.NO_WAY) AllIcons.General.Error
      else if (safeToConvert == Magic8.NO_WAY) AllIcons.General.Warning
      else null
      action(it, icon)
    }
    .sortedWith(comparator)

  val favoriteCharsetsSize = 5
  val favoriteCharsets = sortedCharsetActions.subList(0, favoriteCharsetsSize)
  favoriteCharsets.forEach { group.add(it) }
  val moreCharsets = sortedCharsetActions.subList(favoriteCharsetsSize, sortedCharsetActions.size)
  val more = DefaultActionGroup.createPopupGroup { IdeBundle.message("action.text.more") }
  moreCharsets.forEach { more.add(it) }
  group.add(more)

  return group
}

/**
 * Checks the compatibility of the content and the current encoding before saving the file.
 * @return true if compatible or false otherwise.
 */
fun checkEncodingCompatibility(file: VirtualFile, project: Project): Boolean {
  var compatible = true
  val psiFile = runReadAction { PsiManager.getInstance(project).findFile(file) }
  psiFile?.let {
    val inspectionProfile = InspectionProjectProfileManager.getInstance(project).currentProfile
    val inspectionTool = inspectionProfile.getInspectionTool("MFLossyEncoding", project)
    inspectionTool?.let { tool ->
      val inspectionManager = InspectionManager.getInstance(project)
      val descriptors =
        InspectionEngine.runInspectionOnFile(it, tool, inspectionManager.createNewGlobalContext())
      if (descriptors.isNotEmpty()) {
        compatible = false
      }
    }
  }
  return compatible
}

/**
 * Show dialog on save that warns the user if the content and the selected encoding are incompatible.
 * @return true if saves anyway or false otherwise.
 */
fun showSaveAnywayDialog(charset: Charset): Boolean {
  val result = Messages.showDialog(
    XmlStringUtil.wrapInHtml(
      IdeBundle.message("encoding.unsupported.characters.message", charset.displayName()) +
        "<br><br>Content may change after saving."
    ),
    IdeBundle.message("incompatible.encoding.dialog.title", charset.displayName()),
    arrayOf("Save Anyway", CommonBundle.getCancelButtonText()),
    1,
    AllIcons.General.WarningDialog
  )
  return result == 0
}
