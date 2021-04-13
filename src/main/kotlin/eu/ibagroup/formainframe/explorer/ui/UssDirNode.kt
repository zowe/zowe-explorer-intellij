package eu.ibagroup.formainframe.explorer.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.util.IconUtil
import eu.ibagroup.formainframe.config.ws.UssPath
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.UnitRemoteQueryImpl
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.fetch.UssQuery
import eu.ibagroup.formainframe.dataops.getAttributesService
import eu.ibagroup.formainframe.explorer.WorkingSet
import eu.ibagroup.formainframe.utils.service
import eu.ibagroup.formainframe.vfs.MFVirtualFile

private fun withSlashIfNeeded(ussPath: UssPath): String {
  return if (ussPath.path == "/") {
    ussPath.path
  } else {
    ussPath.path + "/"
  }
}

class UssDirNode(
  ussPath: UssPath,
  project: Project,
  parent: ExplorerTreeNode<*>,
  workingSet: WorkingSet,
  treeStructure: ExplorerTreeStructureBase,
  private var vFile: MFVirtualFile? = null,
  private val isRootNode: Boolean = false
) : RemoteMFFileFetchNode<UssPath, UssQuery, WorkingSet>(
  ussPath, project, parent, workingSet, treeStructure
), UssNode, RefreshableNode {

  val isConfigUssPath = vFile == null

  override val query: RemoteQuery<UssQuery, Unit>?
    get() {
      val connectionConfig = unit.connectionConfig
      val urlConnection = unit.urlConnection
      return if (connectionConfig != null && urlConnection != null) {
        UnitRemoteQueryImpl(UssQuery(value.path), connectionConfig, urlConnection)
      } else null
    }

  private val attributesService
    get() = explorer.componentManager.service<DataOpsManager>()
      .getAttributesService<RemoteUssAttributes, MFVirtualFile>()

  override fun Collection<MFVirtualFile>.toChildrenNodes(): List<AbstractTreeNode<*>> {
    return find { attributesService.getAttributes(it)?.path == value.path }
      ?.also {
        vFile = it
        treeStructure.registerNode(this@UssDirNode)
      }
      ?.children
      ?.map {
        if (it.isDirectory) {
          UssDirNode(
            UssPath(withSlashIfNeeded(value) + it.name),
            notNullProject,
            this@UssDirNode,
            unit,
            treeStructure,
            it
          )
        } else {
          UssFileNode(it, notNullProject, this@UssDirNode, unit, treeStructure)
        }
      } ?: listOf()
  }

  override val requestClass = UssQuery::class.java

  override fun makeFetchTaskTitle(query: RemoteQuery<UssQuery, Unit>): String {
    return "Fetching USS listings for ${query.request.path}"
  }

  override fun update(presentation: PresentationData) {
    val icon = when {
      isRootNode -> {
        AllIcons.Nodes.Module
      }
      vFile != null -> {
        IconUtil.getIcon(vFile!!, 0, project)
      }
      else -> {
        AllIcons.Nodes.Folder
      }
    }
    val text = when {
      isRootNode -> {
        value.path
      }
      vFile != null -> {
        vFile!!.presentableName
      }
      else -> {
        value.path.split("/").last()
      }
    }
    presentation.setIcon(icon)
    presentation.presentableText = text
  }

  override fun getVirtualFile(): MFVirtualFile? {
    return vFile
  }

}