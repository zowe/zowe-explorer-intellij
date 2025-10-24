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

import com.intellij.ide.util.treeView.PresentableNodeDescriptor
import com.intellij.ui.SimpleTextAttributes
import org.zowe.explorer.v3.state.config.ConnectionConfigRelated
import org.zowe.explorer.v3.tree.ExplorerTreeComponentService
import javax.swing.Icon

// TODO: doc
abstract class FetcherNodeDescriptor(
  displayName: String,
  val basePath: List<String>,
  val filterPath: List<String>,
  tooltip: String,
  icon: Icon,
  override var connectionConfigUuid: String
) : ExplorerTreeNodeDescriptor(displayName, tooltip, icon, isLeaf=false, hasExpandChevron=true),
  LazyExpandable, ConnectionConfigRelated, UpdateInfoHolder
{
  abstract val fetchFilter: String
  abstract val invalidationElem: String
  abstract val invalidationPath: List<String>

  override val textToPreserve = listOf(
    PresentableNodeDescriptor.ColoredFragment(
      displayName,
      SimpleTextAttributes.REGULAR_ATTRIBUTES
    )
  )

  override var currentUpdateInfo: PresentableNodeDescriptor.ColoredFragment? = null

  override var wasExpanded = false

  fun setUpdateInfo(updateInfoToSet: PresentableNodeDescriptor.ColoredFragment? = null) {
    this.setUpdateInfo(genuinePresentationData, updateInfoToSet)
  }

  override fun expandNode(node: ExplorerTreeNode) {
    if (!wasExpanded) {
      wasExpanded = true
      ExplorerTreeComponentService.getService()
        .getFilesExplorerComponent(node.project)
        .invalidateNode(node)
    }
  }
}
