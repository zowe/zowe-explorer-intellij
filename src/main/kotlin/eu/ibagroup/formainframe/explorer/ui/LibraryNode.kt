package eu.ibagroup.formainframe.explorer.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.UnitRemoteQueryImpl
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.fetch.LibraryQuery
import eu.ibagroup.formainframe.dataops.getAttributesService
import eu.ibagroup.formainframe.explorer.WorkingSet
import eu.ibagroup.formainframe.utils.service
import eu.ibagroup.formainframe.vfs.MFVirtualFile

class LibraryNode(
  library: MFVirtualFile,
  project: Project,
  parent: ExplorerTreeNode<*>,
  workingSet: WorkingSet,
  treeStructure: ExplorerTreeStructureBase
) : RemoteMFFileFetchNode<MFVirtualFile, LibraryQuery, WorkingSet>(
  library, project, parent, workingSet, treeStructure
), MFNode, RefreshableNode {

  override val query: RemoteQuery<LibraryQuery, Unit>?
    get() {
      val connectionConfig = unit.connectionConfig
      val urlConnection = unit.urlConnection
      return if (connectionConfig != null && urlConnection != null) {
        UnitRemoteQueryImpl(LibraryQuery(value), connectionConfig, urlConnection)
      } else null
    }

  override fun Collection<MFVirtualFile>.toChildrenNodes(): List<AbstractTreeNode<*>> {
    return map { FileLikeDatasetNode(it, notNullProject, this@LibraryNode, unit, treeStructure) }
  }

  override val requestClass = LibraryQuery::class.java

  override fun update(presentation: PresentationData) {
    presentation.setIcon(if (value.isDirectory) AllIcons.Nodes.Folder else AllIcons.FileTypes.Any_type)
    presentation.addText(value.presentableName, SimpleTextAttributes.REGULAR_ATTRIBUTES)
    val volser = explorer.componentManager.service<DataOpsManager>()
      .getAttributesService<RemoteDatasetAttributes, MFVirtualFile>()
      .getAttributes(value)?.volser
    volser?.let { presentation.addText(" $it", SimpleTextAttributes.GRAYED_ATTRIBUTES) }
  }

  override fun getVirtualFile(): MFVirtualFile {
    return value
  }

  override fun makeFetchTaskTitle(query: RemoteQuery<LibraryQuery, Unit>): String {
    return "Fetching members for ${query.request.library.name}"
  }
}