/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.v3.tree.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.ui.SimpleTextAttributes
import javax.swing.Icon

// TODO: doc
class ErrorNodeDescriptor(
  errorText: String = "Unknown error",
  errorTooltip: String = "",
  icon: Icon? = null
) : ExplorerTreeNodeDescriptor(errorText, errorTooltip, icon),
  Traversable, Ephemeral
{
  override val path: List<String> = listOf()

  override fun updateNode(presentationData: PresentationData) {
    presentationData.tooltip = tooltip
    presentationData.clearText()
    presentationData.addText(displayName, SimpleTextAttributes.ERROR_ATTRIBUTES)
  }
}
