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

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EncodingPanelWidgetFactory
import eu.ibagroup.formainframe.editor.zoweExplorerInstalled

/**
 * Status bar widget factory for [MfEncodingPanel].
 */
class MfEncodingPanelWidgetFactory: EncodingPanelWidgetFactory() {

  override fun getId(): String {
    return  MF_ENCODING_PANEL_WIDGET
  }

  override fun createWidget(project: Project): StatusBarWidget {
    return MfEncodingPanel(project)
  }

  // This code should not be present in Zowe repo
  /** Always enabled except when the zowe-explorer plugin is installed. */
  override fun canBeEnabledOn(statusBar: StatusBar): Boolean {
    if (zoweExplorerInstalled) {
      return false
    }
    return super.canBeEnabledOn(statusBar)
  }
}