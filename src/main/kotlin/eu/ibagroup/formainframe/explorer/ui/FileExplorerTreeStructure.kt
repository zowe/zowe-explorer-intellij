package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.SmartList
import eu.ibagroup.formainframe.explorer.Explorer
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

private val PROVIDERS = SmartList(FileExplorerTreeStructureProvider())

class FileExplorerTreeStructure(explorer: Explorer, project: Project) : ExplorerTreeStructureBase(explorer, project) {

  private val valueToNodeMap = Collections.synchronizedMap(
    WeakHashMap<Any, ConcurrentLinkedQueue<ExplorerTreeNode<*>>>()
  )
  private val fileToNodeMap = Collections.synchronizedMap(
    WeakHashMap<VirtualFile, ConcurrentLinkedQueue<ExplorerTreeNode<*>>>()
  )

  override fun registerNode(node: ExplorerTreeNode<*>) {
    valueToNodeMap.getOrPut(node.value) { ConcurrentLinkedQueue() }.add(node)
    val file = node.virtualFile ?: return
    fileToNodeMap.getOrPut(file) { ConcurrentLinkedQueue() }.add(node)
  }

  @Suppress("UNCHECKED_CAST")
  override fun <V : Any> findByValue(value: V): Collection<ExplorerTreeNode<V>> {
    return valueToNodeMap[value] as Collection<ExplorerTreeNode<V>>? ?: emptySet()
  }

  override fun findByPredicate(predicate: (ExplorerTreeNode<*>) -> Boolean): Collection<ExplorerTreeNode<*>> {
    return valueToNodeMap.values.flatten().filter(predicate)
  }

  override fun findByVirtualFile(file: VirtualFile): Collection<ExplorerTreeNode<*>> {
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

  override val showVolser = true

  override val showMasksAndPathAsSeparateDirs = true

  override var showWorkingSetInfo = true

}