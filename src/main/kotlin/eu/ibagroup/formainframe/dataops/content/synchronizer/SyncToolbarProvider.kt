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

package eu.ibagroup.formainframe.dataops.content.synchronizer

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.toolbar.floating.AbstractFloatingToolbarProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity.RequiredForSmartMode

private const val ACTION_GROUP = "eu.ibagroup.formainframe.dataops.content.synchronizer.SyncActionGroup"

/**
 * Class which serves as extension point for editor floating toolbar provider. Defined in plugin.xml
 */
class SyncToolbarProvider : AbstractFloatingToolbarProvider(ACTION_GROUP), RequiredForSmartMode {
  override val autoHideable = true
  override val priority = 1
  override val actionGroup: ActionGroup by lazy {
    resolveActionGroup()
  }

  /**
   * Resolves Sync Action Toolbar during IDE startup activity
   * @return resolved sync action
   */
  private fun resolveActionGroup(): ActionGroup {
    ApplicationManager.getApplication()
    val actionManager = ActionManager.getInstance()
    val action = actionManager.getAction(ACTION_GROUP)
    if (action is ActionGroup) return action
    val defaultActionGroup = DefaultActionGroup()
    actionManager.registerAction(ACTION_GROUP, defaultActionGroup)
    return defaultActionGroup
  }

  /**
   * Runs an activity during IDE startup
   * @param project - current project
   */
  override fun runActivity(project: Project) {
    resolveActionGroup()
  }
}
