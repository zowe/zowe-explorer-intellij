package eu.ibagroup.formainframe.explorer.ui

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.util.IconUtil
import eu.ibagroup.formainframe.explorer.ExplorerUnit
import eu.ibagroup.formainframe.explorer.ExplorerViewSettings
import eu.ibagroup.formainframe.vfs.MFVirtualFile

class UssFileNode(
  file: MFVirtualFile,
  project: Project,
  parent: ExplorerTreeNodeBase<*>,
  unit: ExplorerUnit,
  viewSettings: ExplorerViewSettings
) : ExplorerUnitTreeNodeBase<MFVirtualFile, ExplorerUnit>(file, project, parent, unit, viewSettings) {

  override fun update(presentation: PresentationData) {
    presentation.presentableText = value.presentableName
    presentation.setIcon(IconUtil.getIcon(value, 0, project))
  }

  override fun getVirtualFile(): MFVirtualFile {
    return value
  }

  override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
    return mutableListOf()
  }
}