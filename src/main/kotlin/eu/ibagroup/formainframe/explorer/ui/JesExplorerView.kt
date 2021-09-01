/*
 * This is property of IBA Group
 */

package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.config.jobs.JobsWorkingSetConfig
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.JesExplorer
import eu.ibagroup.formainframe.explorer.JesWorkingSet

class JesExplorerView(explorer: Explorer<JesWorkingSet>, project: Project, parentDisposable: Disposable, contextMenu: ActionGroup,
                      rootNodeProvider: (Explorer<*>, Project, ExplorerTreeStructureBase) -> ExplorerTreeNode<*>,
                      cutProviderUpdater: (List<VirtualFile>) -> Unit
) : ExplorerTreeView<JesWorkingSet, JobsWorkingSetConfig>(explorer, project,
  parentDisposable,
  contextMenu, rootNodeProvider, cutProviderUpdater
) {


  override fun getData(dataId: String): Any? {
    return null
  }

  override fun dispose() {

  }
}
