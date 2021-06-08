package eu.ibagroup.formainframe.explorer.ui

import com.intellij.icons.AllIcons
import com.intellij.icons.AllIcons.Actions.Pause
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.components.service
import com.intellij.openapi.fileTypes.impl.FileTypeRenderer
import com.intellij.openapi.project.Project
import com.intellij.ui.LayeredIcon
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.IconUtil
import com.intellij.util.containers.toMutableSmartList
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.MFRemoteFileAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.getAttributesService
import eu.ibagroup.formainframe.explorer.ExplorerUnit
import eu.ibagroup.formainframe.utils.service
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import javax.swing.SwingConstants

private val migratedIcon = AllIcons.FileTypes.Any_type


class FileLikeDatasetNode(
  file: MFVirtualFile,
  project: Project,
  parent: ExplorerTreeNode<*>,
  unit: ExplorerUnit,
  treeStructure: ExplorerTreeStructureBase
) : ExplorerUnitTreeNodeBase<MFVirtualFile, ExplorerUnit>(
  file, project, parent, unit, treeStructure
), MFNode {

  override fun isAlwaysLeaf(): Boolean {
    return !value.isDirectory
  }

  override fun update(presentation: PresentationData) {
    val attributes = service<DataOpsManager>().tryToGetAttributes(value) as? RemoteDatasetAttributes
    val isMigrated = attributes?.isMigrated ?: false
    presentation.setIcon(if (value.isDirectory) AllIcons.Nodes.Folder else if (isMigrated) migratedIcon else AllIcons.FileTypes.Text)
    updateMainTitleUsingCutBuffer(value.presentableName, presentation)
    val volser = explorer.componentManager.service<DataOpsManager>()
      .getAttributesService<RemoteDatasetAttributes, MFVirtualFile>()
      .getAttributes(value)?.volser
    volser?.let { presentation.addText(" $it", SimpleTextAttributes.GRAYED_ATTRIBUTES) }
  }

  override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
    return value.cachedChildren
      .map { FileLikeDatasetNode(value, notNullProject, this, unit, treeStructure) }.toMutableSmartList()
  }

  override fun getVirtualFile(): MFVirtualFile {
    return value
  }
}