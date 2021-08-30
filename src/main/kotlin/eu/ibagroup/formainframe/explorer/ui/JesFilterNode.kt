/*
 * This is property of IBA Group
 */

package eu.ibagroup.formainframe.explorer.ui


import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.util.containers.toMutableSmartList
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.UnitRemoteQueryImpl
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.JesFilterUnit
import eu.ibagroup.formainframe.vfs.MFVirtualFile

class JesFilterNode(value: JesFilterUnit, project: Project, parent: ExplorerTreeNode<*>, explorer: Explorer,
                    treeStructure: ExplorerTreeStructureBase
) : RemoteMFFileFetchNode<JesFilterUnit, JesFilterUnit, JesFilterUnit>(value, project, parent, value, treeStructure )
{

  override val query: RemoteQuery<JesFilterUnit, Unit>?
    get() {
      val connectionConfig = unit.connectionConfig
      return if (connectionConfig != null) {
        UnitRemoteQueryImpl(value, connectionConfig)
      } else null
    }

  override fun Collection<MFVirtualFile>.toChildrenNodes(): MutableList<AbstractTreeNode<*>> {
    return map {
      TODO()
    }.toMutableSmartList()
  }

  override val requestClass = JesFilterUnit::class.java



  override fun update(presentation: PresentationData) {
    presentation.presentableText = value.name
  }


  override fun makeFetchTaskTitle(query: RemoteQuery<JesFilterUnit, Unit>): String {
    return "Fetching jobs for ${query.request.jobsFilter}"
  }


}