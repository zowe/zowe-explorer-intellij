package eu.ibagroup.formainframe.explorer.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.fetch.LibraryQuery
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.RemoteQueryImpl
import eu.ibagroup.formainframe.dataops.getAttributesService
import eu.ibagroup.formainframe.explorer.ExplorerViewSettings
import eu.ibagroup.formainframe.explorer.WorkingSet
import eu.ibagroup.formainframe.utils.service
import eu.ibagroup.formainframe.vfs.MFVirtualFile

class LibraryNode(
  library: MFVirtualFile,
  project: Project,
  workingSet: WorkingSet,
  explorerViewSettings: ExplorerViewSettings
) : RemoteMFFileCacheNode<MFVirtualFile, LibraryQuery, WorkingSet>(library, project, workingSet, explorerViewSettings) {

  override val query: RemoteQuery<LibraryQuery>?
    get() {
      val connectionConfig = unit.connectionConfig
      val urlConnection = unit.urlConnection
      return if (connectionConfig != null && urlConnection != null) {
        RemoteQueryImpl(LibraryQuery(value), connectionConfig, urlConnection)
      } else null
    }

  override fun Collection<MFVirtualFile>.toChildrenNodes(): List<AbstractTreeNode<*>> {
    return map { FileLikeDatasetNode(it, notNullProject, unit, viewSettings) }
  }

  override val requestClass = LibraryQuery::class.java

  override fun update(presentation: PresentationData) {
    presentation.setIcon(if (value.isDirectory) AllIcons.Nodes.Folder else AllIcons.FileTypes.Any_type)
    presentation.addText(value.presentableName, SimpleTextAttributes.REGULAR_ATTRIBUTES)
    val volser = service<DataOpsManager>(explorer.componentManager)
      .getAttributesService<RemoteDatasetAttributes, MFVirtualFile>()
      .getAttributes(value)?.volser
    volser?.let { presentation.addText(" $it", SimpleTextAttributes.GRAYED_ATTRIBUTES) }
  }

  override fun getVirtualFile(): MFVirtualFile {
    return value
  }
}