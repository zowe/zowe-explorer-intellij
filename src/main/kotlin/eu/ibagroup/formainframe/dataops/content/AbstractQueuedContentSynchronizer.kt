package eu.ibagroup.formainframe.dataops.content

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.messages.MessageBus
import eu.ibagroup.formainframe.utils.QueueExecutor
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

abstract class AbstractQueuedContentSynchronizer(
  messageBus: MessageBus,
  parentDisposable: Disposable
) : ContentSynchronizer {

  private val fileToExecutorMap = ConcurrentHashMap<VirtualFile, QueueExecutor<Unit, Unit>>()

  protected abstract fun buildExecutorForFile(
    file: VirtualFile,
    saveStrategy: SaveStrategy
  ): QueueExecutor<Unit, Unit>

  protected val disposableQueues = Disposable {
    fileToExecutorMap.forEach { (_, u) -> u.shutdown() }
    fileToExecutorMap.clear()
  }

  init {
    Disposer.register(parentDisposable, disposableQueues)
    messageBus.connect(parentDisposable)
      .subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
        override fun after(events: MutableList<out VFileEvent>) {
          events
            .filterIsInstance<VFileContentChangeEvent>()
            .mapNotNull {
              val executor = fileToExecutorMap[it.file]
              if (executor != null && it.requestor !is ContentSynchronizer) {
                executor
              } else null
            }
            .forEach { it.accept(Unit) }
        }
      })
  }

  override fun enforceSync(
    file: VirtualFile,
    acceptancePolicy: AcceptancePolicy,
    saveStrategy: SaveStrategy
  ) {
    if (file.isDirectory) {
      throw IOException("Directories cannot be synced")
    }
    if (!file.isValid || !file.exists()) {
      throw IOException("Non-valid or non-existing files cannot be synced")
    }
    if (!accepts(file)) {
      throw IllegalArgumentException("${this::class.java} cannot accept file ${file.path} for syncing")
    }
    val existingExecutor = fileToExecutorMap[file]
    if (existingExecutor != null) {
      throw IOException("File ${file.path} is already synced")
    }
    val length = file.length
    if (length != 0L && acceptancePolicy == AcceptancePolicy.IF_EMPTY_ONLY) {
      throw IOException("Cannot sync non-empty file due provided acceptance policy $acceptancePolicy")
    }
    val executor = buildExecutorForFile(file, saveStrategy)
    fileToExecutorMap[file] = executor
    executor.accept(Unit)
  }

  override fun isAlreadySynced(file: VirtualFile): Boolean {
    return fileToExecutorMap[file] != null
  }

  override fun removeSync(file: VirtualFile) {
    val executor = fileToExecutorMap[file]
      ?: throw IOException("Cannot remove sync for ${file.path} since it's not synchronized already")
    executor.shutdown()
  }

}