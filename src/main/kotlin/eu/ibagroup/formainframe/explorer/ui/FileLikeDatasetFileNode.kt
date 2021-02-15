package eu.ibagroup.formainframe.explorer.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.containers.toMutableSmartList
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.dataOpsManager
import eu.ibagroup.formainframe.dataops.getAttributesService
import eu.ibagroup.formainframe.explorer.ExplorerUnit
import eu.ibagroup.formainframe.explorer.ExplorerViewSettings
import eu.ibagroup.formainframe.vfs.MFVirtualFile

class FileLikeDatasetFileNode(
  private val file: MFVirtualFile,
  unit: ExplorerUnit,
  viewSettings: ExplorerViewSettings
) : ExplorerUnitTreeNodeBase<MFVirtualFile, ExplorerUnit>(file, unit, viewSettings) {

  override fun isAlwaysLeaf(): Boolean {
    return !file.isDirectory
  }

  override fun update(presentation: PresentationData) {
    presentation.setIcon(if (file.isDirectory) AllIcons.Nodes.Folder else AllIcons.FileTypes.Text)
    presentation.addText(file.presentableName, SimpleTextAttributes.REGULAR_ATTRIBUTES)
    val volser = dataOpsManager
      .getAttributesService<RemoteDatasetAttributes, MFVirtualFile>()
      .getAttributes(value)?.volser
    volser?.let { presentation.addText(" $it", SimpleTextAttributes.GRAYED_ATTRIBUTES) }
  }

  override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
    return value.cachedChildren.map { FileLikeDatasetFileNode(file, unit, viewSettings) }.toMutableSmartList()
  }

  override fun getVirtualFile(): MFVirtualFile {
    return value
  }
}