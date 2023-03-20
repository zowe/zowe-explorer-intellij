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

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.LineSeparatorPanel
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import kotlinx.coroutines.CoroutineScope

const val MF_LINE_SEPARATOR_PANEL_WIDGET = "MF" + StatusBar.StandardWidgets.LINE_SEPARATOR_PANEL

/**
 * Line separator panel in status bar with correctly display for MF files.
 */
class MfLineSeparatorPanel(project: Project, scope: CoroutineScope): LineSeparatorPanel(project, scope) {

  /**
   * Returns the state of the widget for correct display in the status bar.
   * Displayed only for MF files.
   * @param file virtual file opened in editor.
   * @return widget state [com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup.WidgetState].
   */
  override fun getWidgetState(file: VirtualFile?): WidgetState {
    if (file !is MFVirtualFile) {
      return WidgetState.HIDDEN
    }
    return super.getWidgetState(file)
  }

  override fun createInstance(project: Project): StatusBarWidget {
    return MfLineSeparatorPanel(project, scope)
  }

  /** Widget is not enabled for all MF files except USS files. */
  override fun isEnabledForFile(file: VirtualFile?): Boolean {
    val attributes = file?.let { service<DataOpsManager>().tryToGetAttributes(it) }
    if (attributes !is RemoteUssAttributes) {
      return false
    }
    return super.isEnabledForFile(file)
  }

  override fun ID(): String {
    return  MF_LINE_SEPARATOR_PANEL_WIDGET
  }
}