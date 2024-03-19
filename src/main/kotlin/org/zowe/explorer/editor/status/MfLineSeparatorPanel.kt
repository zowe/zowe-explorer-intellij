/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.editor.status

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.LineSeparatorPanel
import org.zowe.explorer.editor.isMfVirtualFile
import org.zowe.explorer.editor.isUssVirtualFile
import kotlinx.coroutines.CoroutineScope

const val MF_LINE_SEPARATOR_PANEL_WIDGET = "ZoweMF" + StatusBar.StandardWidgets.LINE_SEPARATOR_PANEL

/**
 * Line separator panel in status bar with correctly display for MF files.
 */
class MfLineSeparatorPanel(project: Project, scope: CoroutineScope): LineSeparatorPanel(project, scope) {

  override fun createInstance(project: Project): StatusBarWidget {
    return MfLineSeparatorPanel(project, scope)
  }

  /** Widget is not enabled for all MF files except USS files. */
  override fun isEnabledForFile(file: VirtualFile?): Boolean {
    if (file != null && file.isMfVirtualFile() && !file.isUssVirtualFile()) {
      return false
    }
    // need to disable changing line separator when more than one project is open
    // see https://youtrack.jetbrains.com/issue/IDEA-346634/
    if (ProjectManager.getInstance().openProjects.size > 1) {
      return false
    }
    return super.isEnabledForFile(file)
  }

  override fun ID(): String {
    return  MF_LINE_SEPARATOR_PANEL_WIDGET
  }
}
