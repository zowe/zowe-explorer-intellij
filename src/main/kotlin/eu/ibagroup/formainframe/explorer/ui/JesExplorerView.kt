/*
 * This is property of IBA Group
 */

package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.explorer.Explorer

class JesExplorerView(explorer: Explorer, project: Project, parentDisposable: Disposable, contextMenu: ActionGroup,
                      rootNodeProvider: (Explorer, Project, ExplorerTreeStructureBase) -> ExplorerTreeNode<*>,
                      cutProviderUpdater: (List<VirtualFile>) -> Unit
) : ExplorerTreeView(explorer, project,
  parentDisposable,
  contextMenu, rootNodeProvider, cutProviderUpdater
) {


  override fun getData(dataId: String): Any? {
    return null
  }

  override fun dispose() {

  }
}