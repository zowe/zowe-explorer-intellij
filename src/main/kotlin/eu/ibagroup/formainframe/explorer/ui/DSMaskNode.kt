package eu.ibagroup.formainframe.explorer.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.containers.toMutableSmartList
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.UnitRemoteQueryImpl
import eu.ibagroup.formainframe.explorer.WorkingSet
import eu.ibagroup.formainframe.vfs.MFVirtualFile

class DSMaskNode(
  dsMask: DSMask,
  project: Project,
  parent: ExplorerTreeNode<*>,
  workingSet: WorkingSet,
  treeStructure: ExplorerTreeStructureBase
) : RemoteMFFileFetchNode<DSMask, DSMask, WorkingSet>(
  dsMask, project, parent, workingSet, treeStructure
), MFNode, RefreshableNode {

  override fun update(presentation: PresentationData) {
    presentation.addText(value.mask, SimpleTextAttributes.REGULAR_ATTRIBUTES)
    presentation.addText(" ${value.volser}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
    presentation.setIcon(AllIcons.Nodes.Module)
  }

  override val query: RemoteQuery<DSMask, Unit>?
    get() {
      val connectionConfig = unit.connectionConfig
      val urlConnection = unit.urlConnection
      return if (connectionConfig != null && urlConnection != null) {
        UnitRemoteQueryImpl(value, connectionConfig, urlConnection)
      } else null
    }

  override fun Collection<MFVirtualFile>.toChildrenNodes(): MutableList<AbstractTreeNode<*>> {
    return map {
      if (it.isDirectory) {
        LibraryNode(it, notNullProject, this@DSMaskNode, unit, treeStructure)
      } else {
        FileLikeDatasetNode(it, notNullProject, this@DSMaskNode, unit, treeStructure)
      }
    }.toMutableSmartList()
  }

  override val requestClass = DSMask::class.java

  override fun makeFetchTaskTitle(query: RemoteQuery<DSMask, Unit>): String {
    return "Fetching listings for ${query.request.mask}"
  }


}