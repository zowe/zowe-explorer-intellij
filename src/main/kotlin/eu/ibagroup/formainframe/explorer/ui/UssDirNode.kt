package eu.ibagroup.formainframe.explorer.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.IconUtil
import eu.ibagroup.formainframe.config.ws.UssPath
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributesService
import eu.ibagroup.formainframe.dataops.fetch.RemoteQuery
import eu.ibagroup.formainframe.dataops.fetch.RemoteQueryImpl
import eu.ibagroup.formainframe.dataops.fetch.UssQuery
import eu.ibagroup.formainframe.dataops.getAttributesService
import eu.ibagroup.formainframe.explorer.ExplorerUnit
import eu.ibagroup.formainframe.explorer.ExplorerViewSettings
import eu.ibagroup.formainframe.explorer.WorkingSet
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.formainframe.vfs.MFVirtualFileSystem

private fun withSlashIfNeeded(ussPath: UssPath): String {
  return if (ussPath.path == "/") {
    ussPath.path
  } else {
    ussPath.path + "/"
  }
}

class UssDirNode(
  ussPath: UssPath,
  workingSet: WorkingSet,
  viewSettings: ExplorerViewSettings,
  private var vFile: MFVirtualFile? = null,
  private val isRootNode: Boolean = false
) : RemoteMFFileCacheNode<UssPath, UssQuery, WorkingSet>(ussPath, workingSet, viewSettings) {

  override val query: RemoteQuery<UssQuery>?
    get() {
      val connectionConfig = unit.connectionConfig
      val urlConnection = unit.urlConnection
      return if (connectionConfig != null && urlConnection != null) {
        RemoteQueryImpl(UssQuery(value.path), connectionConfig, urlConnection)
      } else null
    }

  private val attributesService
    get() = unit.explorer.dataOpsManager.getAttributesService<RemoteUssAttributes, MFVirtualFile>()

  override fun Collection<MFVirtualFile>.toChildrenNodes(): List<AbstractTreeNode<*>> {
    return find { attributesService.getAttributes(it)?.path == value.path }
      ?.also { vFile = it }
      ?.children
      ?.map {
        if (it.isDirectory) {
          UssDirNode(UssPath(withSlashIfNeeded(value) + it.name), unit, viewSettings, it)
        } else {
          UssFileNode(it, unit, viewSettings)
        }
      } ?: listOf()
  }

  override val requestClass = UssQuery::class.java

  override fun update(presentation: PresentationData) {
    val icon = when {
      isRootNode -> {
        AllIcons.Nodes.Module
      }
      vFile != null -> {
        IconUtil.getIcon(vFile!!, 0, unit.explorer.project)
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