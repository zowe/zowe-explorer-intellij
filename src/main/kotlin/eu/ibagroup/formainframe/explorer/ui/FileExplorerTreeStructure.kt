package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.project.Project
import com.intellij.util.SmartList
import eu.ibagroup.formainframe.explorer.Explorer

private val PROVIDERS = SmartList(FileExplorerTreeStructureProvider())

class FileExplorerTreeStructure(explorer: Explorer, project: Project) : ExplorerTreeStructureBase(explorer, project) {

  override fun getRootElement(): Any {
    return FileExplorerTreeNodeRoot(explorer, project, this)
  }

  override fun commit() {
  }

  override fun hasSomethingToCommit() = false

  override fun getProviders() = PROVIDERS

  override val showVolser = false

  override val showMasksAndPathAsSeparateDirs = true

  override val showWorkingSetInfo = false

}