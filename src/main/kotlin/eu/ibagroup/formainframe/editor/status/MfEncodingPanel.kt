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
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EncodingPanel
import com.intellij.util.ObjectUtils
import eu.ibagroup.formainframe.vfs.MFVirtualFile

/**
 * Encoding panel in status bar with correctly display for MF files.
 */
class MfEncodingPanel(project: Project): EncodingPanel(project) {

  /**
   * Returns the state of the widget for correct display in the status bar.
   * Disabled for MF files.
   * @param file virtual file opened in editor.
   * @return widget state [com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup.WidgetState].
   */
  override fun getWidgetState(file: VirtualFile?): WidgetState {
    if (file is MFVirtualFile) {
      val charset = file.charset
      val charsetName = ObjectUtils.notNull(charset.displayName(), IdeBundle.message("encoding.not.available"))
      val toolTipText = IdeBundle.message(
        "status.bar.text.file.encoding",
        charsetName
      )
      return WidgetState(toolTipText, charsetName, false)
    }
    return super.getWidgetState(file)
  }

  override fun createInstance(project: Project): StatusBarWidget {
    return MfEncodingPanel(project)
  }

  override fun ID(): String {
    return "MF" + StatusBar.StandardWidgets.ENCODING_PANEL
  }
}