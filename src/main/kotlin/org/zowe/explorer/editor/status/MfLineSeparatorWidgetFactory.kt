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
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.widget.StatusBarEditorBasedWidgetFactory
import com.intellij.ui.UIBundle
import kotlinx.coroutines.CoroutineScope

/**
 * Status bar widget factory for [MfLineSeparatorPanel].
 */
class MfLineSeparatorWidgetFactory: StatusBarEditorBasedWidgetFactory() {

  override fun getId(): String {
    return "MF" + StatusBar.StandardWidgets.LINE_SEPARATOR_PANEL
  }

  override fun getDisplayName(): String {
    return UIBundle.message("status.bar.line.separator.widget.name")
  }

  override fun createWidget(project: Project, scope: CoroutineScope): StatusBarWidget {
    return MfLineSeparatorPanel(project, scope)
  }
}
