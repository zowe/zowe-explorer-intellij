/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.explorer.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import org.zowe.explorer.common.ui.cleanInvalidateOnExpand
import org.zowe.explorer.config.connect.ConnectionConfigBase
import org.zowe.explorer.explorer.Explorer
import org.zowe.explorer.explorer.FileExplorerContentProvider
import org.zowe.explorer.utils.castOrNull

/**
 * Node for fetching next batch of files.
 * @param filesLeft - count of files that was not fetched yet.
 * @author Valiantsin Krus
 */
class LoadMoreNode<Connection: ConnectionConfigBase>(
  project: Project,
  parent: ExplorerTreeNode<Connection, *>,
  explorer: Explorer<Connection, *>,
  treeStructure: ExplorerTreeStructureBase,
  filesLeft: Int? = null
) : InfoNodeBase<Connection>(project, parent, explorer, treeStructure) {
  override val text = if (filesLeft == null) "load more" else "load more - $filesLeft items left"
  override val textAttributes: SimpleTextAttributes = SimpleTextAttributes.GRAYED_ATTRIBUTES

  /** Loads next batch of files. */
  override fun navigate(requestFocus: Boolean) {
    val view = FileExplorerContentProvider.getInstance().getExplorerView(project) ?: return
    val parentNode = parent?.castOrNull<FileFetchNode<*, *, *, *, *, *>>() ?: return
    val query = parentNode.query ?: return
    parentNode.needToLoadMore = true

    parentNode.cleanCache(recursively = false, cleanFetchProviderCache = false)
    cleanInvalidateOnExpand(parentNode, view)
    view.myFsTreeStructure
      .findByPredicate {
        if (it is FetchNode) {
          it.query == query
        } else false
      }
      .onEach { foundNode ->
        synchronized(view.myStructure) {
          view.myStructure.invalidate(foundNode, true)
        }
      }
  }

  override fun canNavigate(): Boolean {
    return FileExplorerContentProvider.getInstance().getExplorerView(project) != null
  }
}
