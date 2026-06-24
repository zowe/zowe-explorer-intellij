/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.jes.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import org.zowe.explorer.v3.actions.CreateProfileAction
import org.zowe.explorer.v3.impl.jes.tree.JesExplorerComponent

// TODO: doc
class CreateJesProfileAction : CreateProfileAction(
  "JES Profile",
  JesExplorerComponent.JES_EXPLORER_COMPONENT_NAME
) {
  override fun actionPerformed(e: AnActionEvent) {
    TODO("Not yet implemented")
  }
}