/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.explorer.ui

import com.intellij.CommonBundle
import com.intellij.icons.AllIcons
import com.intellij.ide.IdeBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.encoding.EncodingUtil.Magic8
import com.intellij.xml.util.XmlStringUtil
import eu.ibagroup.formainframe.common.message
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.content.synchronizer.DocumentedSyncProvider
import eu.ibagroup.formainframe.utils.reloadIn
import eu.ibagroup.formainframe.utils.runWriteActionInEdtAndWait
import eu.ibagroup.formainframe.utils.saveIn
import eu.ibagroup.formainframe.utils.updateFileTag
import java.awt.event.ActionEvent
import java.nio.charset.Charset
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JLabel

/** Class that represent dialog for changing file encoding. */
class ChangeEncodingDialog(
  private val project: Project?,
  private val virtualFile: VirtualFile,
  private val attributes: RemoteUssAttributes,
  private val charset: Charset,
  private val safeToReload: Magic8,
  private val safeToConvert: Magic8
) : DialogWrapper(false) {

  companion object {
    const val RELOAD_EXIT_CODE = 10
    const val CONVERT_EXIT_CODE = 20

    // TODO: Remove when it becomes possible to mock class constructor with init section.
    /** Wrapper for init() method. It is necessary only for test purposes for now. */
    private fun initialize(init: () -> Unit) {
      init()
    }
  }

  private val message: String

  private val possibleToConvert = attributes.isWritable

  init {
    if (possibleToConvert) {
      title = message("encoding.reload.or.convert.dialog.title", virtualFile.name, charset.name())
      message = message("encoding.reload.or.convert.dialog.message", virtualFile.name, charset.name())
    } else {
      title = message("encoding.reload.dialog.title", virtualFile.name, charset.name())
      message = message("encoding.reload.dialog.message", virtualFile.name, charset.name())
    }
    initialize { init() }
  }

  override fun createCenterPanel(): JComponent {
    val label = JLabel(XmlStringUtil.wrapInHtml(message))
    label.icon = Messages.getQuestionIcon()
    label.iconTextGap = 10
    return label
  }

  /**
   * Creates actions for dialog.
   * Reload and cancel actions by default, and convert action if possible.
   */
  override fun createActions(): Array<Action> {
    val actions = mutableListOf<Action>()
    val reloadAction = object : DialogWrapperAction(IdeBundle.message("button.reload")) {
      override fun doAction(e: ActionEvent?) {
        if (safeToReload == Magic8.NO_WAY && !showReloadAnywayDialog()) {
          doCancelAction()
          return
        }
        val contentSynchronizer = DataOpsManager.instance.getContentSynchronizer(virtualFile)
        val syncProvider = DocumentedSyncProvider(virtualFile)
        if (contentSynchronizer?.isFileUploadNeeded(syncProvider) == true) {
          runWriteActionInEdtAndWait { syncProvider.saveDocument() }
          contentSynchronizer.synchronizeWithRemote(syncProvider)
        }
        attributes.charset = charset
        updateFileTag(attributes)
        reloadIn(project, virtualFile, charset)
        close(RELOAD_EXIT_CODE)
      }
    }
    if (!SystemInfo.isMac && safeToReload == Magic8.NO_WAY) {
      reloadAction.putValue(Action.SMALL_ICON, AllIcons.General.Warning)
    }
    actions.add(reloadAction)
    reloadAction.putValue(Action.MNEMONIC_KEY, 'R'.code)
    val severalProjectsOpen = ProjectManager.getInstance().openProjects.size > 1
    val convertAction = object : DialogWrapperAction(IdeBundle.message("button.convert")) {
      override fun doAction(e: ActionEvent?) {
        if (safeToConvert == Magic8.NO_WAY && !showConvertAnywayDialog()) {
          doCancelAction()
          return
        }
        attributes.charset = charset
        updateFileTag(attributes)
        saveIn(project, virtualFile, charset)
        close(CONVERT_EXIT_CODE)
      }

      override fun isEnabled(): Boolean {
        // need to disable encoding conversion when more than one project is open
        // see https://youtrack.jetbrains.com/issue/IDEA-346634/
        return !severalProjectsOpen
      }
    }
    if (!SystemInfo.isMac && convertAction.isEnabled && safeToConvert == Magic8.NO_WAY) {
      convertAction.putValue(Action.SMALL_ICON, AllIcons.General.Warning)
    }
    if (severalProjectsOpen) {
      val tooltipText = message("encoding.convert.button.error.tooltip")
      convertAction.putValue(Action.SHORT_DESCRIPTION, tooltipText)
    }
    if (possibleToConvert) {
      actions.add(convertAction)
    }
    convertAction.putValue(Action.MNEMONIC_KEY, 'C'.code)
    val cancelAction = myCancelAction
    actions.add(cancelAction)
    cancelAction.putValue(DEFAULT_ACTION, true)
    return actions.toTypedArray()
  }

  /**
   * Show dialog on reload that warns the user if the content and the selected encoding are incompatible.
   * @return true if reload anyway or false otherwise.
   */
  private fun showReloadAnywayDialog(): Boolean {
    val result = Messages.showDialog(
      XmlStringUtil.wrapInHtml(
        IdeBundle.message(
          "dialog.title.file.0.can.t.be.reloaded",
          virtualFile.name,
          charset.displayName(),
          "<br><br>Current encoding: '" + virtualFile.charset.displayName() + "'."
        )
      ),
      IdeBundle.message("incompatible.encoding.dialog.title", charset.displayName()),
      arrayOf(IdeBundle.message("button.reload.anyway"), CommonBundle.getCancelButtonText()),
      1,
      AllIcons.General.WarningDialog
    )
    return result == 0
  }

  /**
   * Show dialog on convert that warns the user if the content and the selected encoding are incompatible.
   * @return true if convert anyway or false otherwise.
   */
  private fun showConvertAnywayDialog(): Boolean {
    val result = Messages.showDialog(
      XmlStringUtil.wrapInHtml(
        IdeBundle.message("encoding.do.not.convert.message", charset.displayName()) + "<br><br>" +
        IdeBundle.message("encoding.unsupported.characters.message", charset.displayName())
      ),
      IdeBundle.message("incompatible.encoding.dialog.title", charset.displayName()),
      arrayOf(IdeBundle.message("button.convert.anyway"), CommonBundle.getCancelButtonText()),
      1,
      AllIcons.General.WarningDialog
    )
    return result == 0
  }
}
