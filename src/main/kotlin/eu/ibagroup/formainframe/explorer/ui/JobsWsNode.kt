package eu.ibagroup.formainframe.explorer.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.ui.LayeredIcon
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.IconUtil
import com.intellij.util.containers.toMutableSmartList
import eu.ibagroup.formainframe.config.connect.username
import eu.ibagroup.formainframe.explorer.GlobalJesWorkingSet

private val regularIcon = AllIcons.Nodes.Project
private val errorIconElement = AllIcons.Nodes.ErrorMark
private val grayscaleIcon = IconUtil.desaturate(regularIcon)
private val errorIcon = LayeredIcon(grayscaleIcon, errorIconElement)

class JobsWsNode(
  workingSet: GlobalJesWorkingSet,
  project: Project,
  parent: ExplorerTreeNode<*>,
  treeStructure: ExplorerTreeStructureBase
) : ExplorerUnitTreeNodeBase<GlobalJesWorkingSet, GlobalJesWorkingSet>(
  workingSet, project, parent, workingSet, treeStructure
), MFNode, RefreshableNode {

  override fun isAlwaysExpand(): Boolean {
    return true
  }

  override fun update(presentation: PresentationData) {
    presentation.addText(value.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
    when {
      value.connectionConfig == null -> connectionIsNotSet(presentation)
      value.masks.isEmpty() -> destinationsAreEmpty(presentation)
      else -> regular(presentation)
    }
    if (treeStructure.showWorkingSetInfo) {
      addInfo(presentation)
    }
  }

  private var cachedChildrenInternal: MutableCollection<out AbstractTreeNode<*>>? = null

  val cachedChildren: MutableCollection<out AbstractTreeNode<*>>
    get() = cachedChildrenInternal ?: mutableListOf()

  override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
    return value.masks.map { JesFilterNode(it, notNullProject, this, value, treeStructure) }
      .toMutableSmartList().also { cachedChildrenInternal = it }
  }

  private fun regular(presentation: PresentationData) {
    presentation.setIcon(regularIcon)
    presentation.tooltip = "Working set"
  }

  private fun connectionIsNotSet(presentation: PresentationData) {
    presentation.setIcon(errorIcon)
    presentation.addText(" ", SimpleTextAttributes.ERROR_ATTRIBUTES)
    presentation.addText("Error: Check connection", SimpleTextAttributes.ERROR_ATTRIBUTES)
    presentation.tooltip = "Check connection for this working set"
  }

  private fun destinationsAreEmpty(presentation: PresentationData) {
    presentation.setIcon(grayscaleIcon)
    presentation.tooltip = "Empty working set"
  }

  private fun addInfo(presentation: PresentationData) {
    val connectionConfig = value.connectionConfig ?: return
    val url = value.connectionConfig?.url ?: return
    val username = username(connectionConfig)
    presentation.addText(" $username on ${connectionConfig.name} [${url}]", SimpleTextAttributes.GRAYED_ATTRIBUTES)
  }
}
