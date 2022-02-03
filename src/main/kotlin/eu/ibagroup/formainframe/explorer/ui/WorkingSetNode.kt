package eu.ibagroup.formainframe.explorer.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.ui.LayeredIcon
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.IconUtil
import eu.ibagroup.formainframe.config.connect.username
import eu.ibagroup.formainframe.explorer.WorkingSet

private val regularIcon = AllIcons.Nodes.Project
private val errorIconElement = AllIcons.Nodes.ErrorMark
private val grayscaleIcon = IconUtil.desaturate(regularIcon)
private val errorIcon = LayeredIcon(grayscaleIcon, errorIconElement)

abstract class WorkingSetNode<MaskType>(
  workingSet: WorkingSet<MaskType>,
  project: Project,
  parent: ExplorerTreeNode<*>,
  treeStructure: ExplorerTreeStructureBase
) : ExplorerUnitTreeNodeBase<WorkingSet<MaskType>, WorkingSet<MaskType>>(
  workingSet, project, parent, workingSet, treeStructure
), MFNode, RefreshableNode {
  protected var cachedChildrenInternal: MutableCollection<out AbstractTreeNode<*>>? = null

  val cachedChildren: MutableCollection<out AbstractTreeNode<*>>
    get() = cachedChildrenInternal ?: mutableListOf()

  override fun isAlwaysExpand(): Boolean {
    return true
  }

  protected fun regular(presentation: PresentationData) {
    presentation.setIcon(regularIcon)
    presentation.tooltip = "Working set"
  }

  protected fun connectionIsNotSet(presentation: PresentationData) {
    presentation.setIcon(errorIcon)
    presentation.addText(" ", SimpleTextAttributes.ERROR_ATTRIBUTES)
    presentation.addText("Error: Check connection", SimpleTextAttributes.ERROR_ATTRIBUTES)
    presentation.tooltip = "Check connection for this working set"
  }

  protected fun destinationsAreEmpty(presentation: PresentationData) {
    presentation.setIcon(grayscaleIcon)
    presentation.tooltip = "Empty working set"
  }

  protected fun addInfo(presentation: PresentationData) {
    val connectionConfig = value.connectionConfig ?: return
    val url = value.connectionConfig?.url ?: return
    val username = username(connectionConfig)
    presentation.addText(" $username on ${connectionConfig.name} [${url}]", SimpleTextAttributes.GRAYED_ATTRIBUTES)
  }
}
