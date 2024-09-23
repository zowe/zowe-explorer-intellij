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

package eu.ibagroup.formainframe.editor

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidgetFactory

/**
 * Project post startup activity.
 */
class ProjectStartupActivity: ProjectActivity {

  /**
   * Implementation of [ProjectActivity.execute].
   * Unregisters widget factories that provide status bar widgets that are overridden in the plugin.
   */
  override suspend fun execute(project: Project) {
    val extensionPoint = StatusBarWidgetFactory.EP_NAME.point
    extensionPoint.extensionList.filter {
      it.id == StatusBar.StandardWidgets.ENCODING_PANEL || it.id == StatusBar.StandardWidgets.LINE_SEPARATOR_PANEL
    }.forEach {
      extensionPoint.unregisterExtension(it::class.java)
    }
  }
}
