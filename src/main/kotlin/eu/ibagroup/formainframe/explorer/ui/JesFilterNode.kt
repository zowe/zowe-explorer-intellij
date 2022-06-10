/*
 * This is property of IBA Group
 */

package eu.ibagroup.formainframe.explorer.ui


import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.util.containers.toMutableSmartList
import eu.ibagroup.formainframe.config.ws.JobsFilter
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.UnitRemoteQueryImpl
import eu.ibagroup.formainframe.explorer.JesWorkingSet
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import icons.ForMainframeIcons

private val jesFilterIcon = ForMainframeIcons.JclDirectory

class JesFilterNode(
  jobsFilter: JobsFilter,
  project: Project,
  parent: ExplorerTreeNode<*>,
  workingSet: JesWorkingSet,
  treeStructure: ExplorerTreeStructureBase
) : RemoteMFFileFetchNode<JobsFilter, JobsFilter, JesWorkingSet>(
  jobsFilter, project, parent, workingSet, treeStructure
), MFNode, RefreshableNode {

  override val query: RemoteQuery<JobsFilter, Unit>?
    get() {
      val connectionConfig = unit.connectionConfig
      return if (connectionConfig != null) {
        UnitRemoteQueryImpl(value, connectionConfig)
      } else null
    }

  override fun Collection<MFVirtualFile>.toChildrenNodes(): MutableList<AbstractTreeNode<*>> {
    return map {
      JobNode(it, notNullProject, this@JesFilterNode, unit, treeStructure)
    }.toMutableSmartList()
  }

  override val requestClass = JobsFilter::class.java


  override fun update(presentation: PresentationData) {
    presentation.presentableText = value.toString()
    presentation.setIcon(jesFilterIcon)
  }


  override fun makeFetchTaskTitle(query: RemoteQuery<JobsFilter, Unit>): String {
    return "Fetching jobs for ${query.request}"
  }


}
