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
import com.intellij.ui.SimpleTextAttributes
import javax.swing.Icon

// TODO: doc
class NoItemsFoundNodeDescriptor(
  displayName: String = "No items found",
  tooltip: String = "No children items to display",
  icon: Icon? = null
) : ExplorerTreeNodeDescriptor(displayName, tooltip, icon),
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
