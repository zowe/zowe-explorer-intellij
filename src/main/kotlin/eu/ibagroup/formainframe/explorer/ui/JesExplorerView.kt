/*
 * This is property of IBA Group
 */

package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.config.ws.JobsWorkingSetConfig
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.JesWorkingSet

val JES_EXPLORER_VIEW = DataKey.create<JesExplorerView>("jesExplorerView")

class JesExplorerView(explorer: Explorer<JesWorkingSet>, project: Project, parentDisposable: Disposable, contextMenu: ActionGroup,
                      rootNodeProvider: (Explorer<*>, Project, ExplorerTreeStructureBase) -> ExplorerTreeNode<*>,
                      cutProviderUpdater: (List<VirtualFile>) -> Unit
) : ExplorerTreeView<JesWorkingSet, JobsWorkingSetConfig>(explorer, project,
  parentDisposable,
  contextMenu, rootNodeProvider, cutProviderUpdater
) {


  override fun getData(dataId: String): Any? {
    return when {
      JES_EXPLORER_VIEW.`is`(dataId) -> this
      else -> null
    }
  }

  override fun dispose() {

  }
}
