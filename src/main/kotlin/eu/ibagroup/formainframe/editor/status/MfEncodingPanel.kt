/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */

package eu.ibagroup.formainframe.editor.status

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EncodingPanel
import kotlinx.coroutines.CoroutineScope
import eu.ibagroup.formainframe.editor.isMfVirtualFile
import eu.ibagroup.formainframe.editor.zoweExplorerInstalled

// This code should not be as described below in Zowe repo
// Change MF to ZoweMF
const val MF_ENCODING_PANEL_WIDGET = "MF" + StatusBar.StandardWidgets.ENCODING_PANEL

/**
 * Encoding panel in status bar with correctly display for MF files.
 */
class MfEncodingPanel(project: Project, scope: CoroutineScope): EncodingPanel(project, scope) {

  // This code should not be present in Zowe repo
  /**
   * Returns the state of the widget for correct display in the status bar.
   * Always displayed except when the zowe-explorer plugin is installed.
   * @param file virtual file opened in editor.
   * @return widget state [com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup.WidgetState].
   */
  override fun getWidgetState(file: VirtualFile?): WidgetState {
    if (zoweExplorerInstalled) {
      return WidgetState.HIDDEN
    }
    return super.getWidgetState(file)
  }

  override fun createInstance(project: Project): StatusBarWidget {
    return MfEncodingPanel(project, scope)
  }

  /** Widget is not enabled for MF files. */
  override fun isEnabledForFile(file: VirtualFile?): Boolean {
    if (file != null && file.isMfVirtualFile()) {
      return false
    }
    return super.isEnabledForFile(file)
  }

  override fun ID(): String {
    return MF_ENCODING_PANEL_WIDGET
  }
}
