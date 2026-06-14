/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.tso.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import org.zowe.explorer.v3.actions.DumbAwareEDTAction
import org.zowe.explorer.v3.impl.tso.tree.TsoSessionsComponent

/**
 * Action to create a new TSO profile.
 */
class CreateTsoProfileAction : DumbAwareEDTAction() {
  override fun actionPerformed(e: AnActionEvent) {
    TODO("Not yet implemented")
  }

  override fun update(e: AnActionEvent) {
    e.presentation.text = "TSO Profile"
    e.presentation.isEnabledAndVisible = e.place.contains(TsoSessionsComponent.TSO_SESSIONS_COMPONENT_NAME)
  }
}