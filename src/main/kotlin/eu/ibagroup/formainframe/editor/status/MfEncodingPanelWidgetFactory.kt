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

/**
 * Status bar widget factory for [MfEncodingPanel].
 */
class MfEncodingPanelWidgetFactory: EncodingPanelWidgetFactory() {

  override fun getId(): String {
    return  "MF" + StatusBar.StandardWidgets.ENCODING_PANEL
  }

  override fun createWidget(project: Project): StatusBarWidget {
    return MfEncodingPanel(project)
  }
}