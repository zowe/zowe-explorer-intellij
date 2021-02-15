package eu.ibagroup.formainframe.explorer.ui

import com.intellij.util.SmartList
import eu.ibagroup.formainframe.explorer.Explorer

private val PROVIDERS = SmartList(FileExplorerTreeStructureProvider())

class FileExplorerTreeStructure(explorer: Explorer) : ExplorerTreeStructureBase(explorer) {

  override fun getRootElement(): Any {
    return FileExplorerTreeNodeRoot(explorer, this)
  }

  override fun commit() {
  }

  override fun hasSomethingToCommit() = false

  override fun getProviders() = PROVIDERS

  override val showVolser = false

  override val showMasksAndPathAsSeparateDirs = true

  override val showWorkingSetInfo = false

}