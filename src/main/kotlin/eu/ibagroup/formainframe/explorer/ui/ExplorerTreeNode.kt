package eu.ibagroup.formainframe.explorer.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.SettingsProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.showYesNoDialog
import com.intellij.ui.SimpleTextAttributes
import eu.ibagroup.formainframe.analytics.AnalyticsService
import eu.ibagroup.formainframe.analytics.events.FileAction
import eu.ibagroup.formainframe.analytics.events.FileEvent
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.content.synchronizer.SaveStrategy
import eu.ibagroup.formainframe.dataops.content.synchronizer.DocumentedSyncProvider
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.UIComponentManager
import eu.ibagroup.formainframe.utils.isBeingEditingNow
import eu.ibagroup.formainframe.utils.service
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import javax.swing.tree.TreePath

abstract class ExplorerTreeNode<Value : Any>(
  value: Value,
  project: Project,
  val parent: ExplorerTreeNode<*>?,
  val explorer: Explorer<*>,
  protected val treeStructure: ExplorerTreeStructureBase
) : AbstractTreeNode<Value>(project, value), SettingsProvider {

  open fun init() {
    @Suppress("LeakingThis")
    treeStructure.registerNode(this)
  }
  init {
    @Suppress("LeakingThis")
    init()
  }

  private val contentProvider = UIComponentManager.INSTANCE.getExplorerContentProvider(explorer::class.java)

  private val descriptor: OpenFileDescriptor?
    get() {
      return OpenFileDescriptor(notNullProject, virtualFile ?: return null)
    }

  public override fun getVirtualFile(): MFVirtualFile? {
    return null
  }

  val notNullProject = project

  override fun getSettings(): ViewSettings {
    return treeStructure
  }

  protected fun updateMainTitleUsingCutBuffer(text: String, presentationData: PresentationData) {
    val file = virtualFile ?: return
    val textAttributes = if (contentProvider.isFileInCutBuffer(file)) {
      SimpleTextAttributes.GRAYED_ATTRIBUTES
    } else {
      SimpleTextAttributes.REGULAR_ATTRIBUTES
    }
    presentationData.addText(text, textAttributes)
  }

  override fun navigate(requestFocus: Boolean) {
    val file = virtualFile ?: return
    descriptor?.let {
      if (!file.isDirectory) {
        val dataOpsManager = explorer.componentManager.service<DataOpsManager>()
        val contentSynchronizer = dataOpsManager.getContentSynchronizer(file) ?: return
        val doSync = file.isReadable || showYesNoDialog(
          title = "File ${file.name} is not readable",
          message = "Do you want to try open it anyway?",
          project = project,
          icon = AllIcons.General.WarningDialog
        )
        if (doSync) {
          val syncProvider = DocumentedSyncProvider(file = file, saveStrategy = SaveStrategy.default(project))
          if (!file.isBeingEditingNow()) {
            contentSynchronizer.synchronizeWithRemote(syncProvider)
          }
        }
        dataOpsManager.tryToGetAttributes(file)?.let { attributes ->
          service<AnalyticsService>().trackAnalyticsEvent(FileEvent(attributes, FileAction.OPEN))
        }
        it.navigate(requestFocus)
      }
    }
  }

  override fun canNavigate(): Boolean {
    return descriptor?.canNavigate() ?: super.canNavigate()
  }

  override fun canNavigateToSource(): Boolean {
    return descriptor?.canNavigateToSource() ?: super.canNavigateToSource()
  }

  private val pathList: List<ExplorerTreeNode<*>>
    get() = if (parent != null) {
      parent.pathList + this
    } else {
      listOf(this)
    }

  val path: TreePath
    get() = TreePath(pathList.toTypedArray())

}
