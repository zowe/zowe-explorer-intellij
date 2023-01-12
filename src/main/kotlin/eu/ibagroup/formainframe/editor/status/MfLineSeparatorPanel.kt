/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.editor.status

import com.intellij.ide.IdeBundle
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.LineSeparatorPanel
import com.intellij.util.LineSeparator
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.vfs.MFVirtualFile

/**
 * Line separator panel in status bar with correctly display for MF files.
 */
class MfLineSeparatorPanel(project: Project): LineSeparatorPanel(project) {

  /**
   * Returns the state of the widget for correct display in the status bar.
   * Disabled for all MF files except uss files.
   * @param file virtual file opened in editor.
   * @return widget state [com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup.WidgetState].
   */
  override fun getWidgetState(file: VirtualFile?): WidgetState {
    val attributes = file?.let { service<DataOpsManager>().tryToGetAttributes(it) }
    if (file is MFVirtualFile && attributes !is RemoteUssAttributes) {
      val lineSeparator = FileDocumentManager.getInstance().getLineSeparator(file, project)
      val toolTipText = IdeBundle.message("tooltip.line.separator", StringUtil.escapeLineBreak(lineSeparator))
      val panelText = LineSeparator.fromString(lineSeparator).toString()
      return WidgetState(toolTipText, panelText, false)
    }
    return super.getWidgetState(file)
  }

  override fun createInstance(project: Project): StatusBarWidget {
    return MfLineSeparatorPanel(project)
  }

  override fun ID(): String {
    return  "MF" + StatusBar.StandardWidgets.LINE_SEPARATOR_PANEL
  }
}