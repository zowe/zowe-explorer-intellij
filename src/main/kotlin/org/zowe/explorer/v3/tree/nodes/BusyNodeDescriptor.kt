/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.tree.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.SimpleTextAttributes

// TODO: doc
class BusyNodeDescriptor(
  displayName: String,
  tooltip: String = "Node is busy with some action..."
) : ExplorerTreeNodeDescriptor(displayName, tooltip, AnimatedIcon.Default()),
  Traversable, Ephemeral
{
  override val path: List<String> = listOf()

  override val genuinePresentationData: PresentationData
    get() {
      return super.genuinePresentationData
        .also {
          it.clearText()
          it.addText(displayName, SimpleTextAttributes.GRAYED_ATTRIBUTES)
        }
    }
}
