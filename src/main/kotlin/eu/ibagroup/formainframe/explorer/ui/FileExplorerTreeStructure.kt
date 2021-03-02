package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.SmartList
import com.jetbrains.rd.util.ConcurrentHashMap
import eu.ibagroup.formainframe.explorer.Explorer
import java.util.concurrent.ConcurrentLinkedQueue

private val PROVIDERS = SmartList(FileExplorerTreeStructureProvider())

class FileExplorerTreeStructure(explorer: Explorer, project: Project) : ExplorerTreeStructureBase(explorer, project) {

  private val valueToNodeMap = ConcurrentHashMap<Any, ConcurrentLinkedQueue<ExplorerTreeNodeBase<*>>>()
  private val fileToNodeMap = ConcurrentHashMap<VirtualFile, ConcurrentLinkedQueue<ExplorerTreeNodeBase<*>>>()

  override fun registerNode(node: ExplorerTreeNodeBase<*>) {
    valueToNodeMap.getOrPut(node.value) { ConcurrentLinkedQueue() }.add(node)
    val file = node.virtualFile ?: return
    fileToNodeMap.getOrPut(file) { ConcurrentLinkedQueue() }.add(node)
  }

  @Suppress("UNCHECKED_CAST")
  override fun <V : Any> findByValue(value: V): Collection<ExplorerTreeNodeBase<V>> {
    return valueToNodeMap[value] as Collection<ExplorerTreeNodeBase<V>>? ?: emptySet()
  }

  override fun findByPredicate(predicate: (ExplorerTreeNodeBase<*>) -> Boolean): Collection<ExplorerTreeNodeBase<*>> {
    return valueToNodeMap.values.flatten().filter(predicate)
  }

  override fun findByVirtualFile(file: VirtualFile): Collection<ExplorerTreeNodeBase<*>> {
    return fileToNodeMap[file] ?: emptySet()
  }

  private val root by lazy { FileExplorerTreeNodeRoot(explorer, project, this) }

  override fun getRootElement(): Any {
    return root
  }

  override fun commit() {
  }

  override fun hasSomethingToCommit() = false

  override fun getProviders() = PROVIDERS

  override val showVolser = false

  override val showMasksAndPathAsSeparateDirs = true

  override val showWorkingSetInfo = false

}