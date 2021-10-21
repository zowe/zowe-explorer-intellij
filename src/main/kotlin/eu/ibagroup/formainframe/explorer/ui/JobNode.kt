/*
 * This is property of IBA Group
 */

package eu.ibagroup.formainframe.explorer.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.UnitRemoteQueryImpl
import eu.ibagroup.formainframe.dataops.attributes.RemoteJobAttributes
import eu.ibagroup.formainframe.dataops.fetch.JobQuery
import eu.ibagroup.formainframe.explorer.GlobalJesWorkingSet
import eu.ibagroup.formainframe.vfs.MFVirtualFile

private val jobIcon = AllIcons.Nodes.Folder

class JobNode(
  library: MFVirtualFile,
  project: Project,
  parent: ExplorerTreeNode<*>,
  workingSet: GlobalJesWorkingSet,
  treeStructure: ExplorerTreeStructureBase
) : RemoteMFFileFetchNode<MFVirtualFile, JobQuery, GlobalJesWorkingSet>(
  library, project, parent, workingSet, treeStructure
), MFNode, RefreshableNode {
  override fun makeFetchTaskTitle(query: RemoteQuery<JobQuery, Unit>): String {
    return "Fetching members for ${query.request.library.name}"
  }

  override val query: RemoteQuery<JobQuery, Unit>?
    get() {
      val connectionConfig = unit.connectionConfig


      return if (connectionConfig != null) {
        UnitRemoteQueryImpl(JobQuery(value), connectionConfig)
      } else null
    }

  override fun Collection<MFVirtualFile>.toChildrenNodes(): List<AbstractTreeNode<*>> {
    return map { SpoolFileNode(it, notNullProject, this@JobNode, unit, treeStructure) }
  }

  override val requestClass = JobQuery::class.java

  override fun update(presentation: PresentationData) {
    val attributes = service<DataOpsManager>().tryToGetAttributes(value) as? RemoteJobAttributes
    val job = attributes?.jobInfo
    val jobIdText = if (job == null) "" else "(${job.jobId})"
    presentation.addText("${job?.jobName ?: ""} ${jobIdText} ", SimpleTextAttributes.REGULAR_ATTRIBUTES)
    presentation.addText(job?.status?.value ?: "", SimpleTextAttributes.GRAYED_ATTRIBUTES)
    presentation.setIcon(jobIcon)
  }
}
