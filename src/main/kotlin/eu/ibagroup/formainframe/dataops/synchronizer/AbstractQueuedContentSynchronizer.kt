package eu.ibagroup.formainframe.dataops.synchronizer

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.FetchCallback
import eu.ibagroup.formainframe.utils.QueueExecutor
import eu.ibagroup.formainframe.utils.subscribe
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

abstract class AbstractQueuedContentSynchronizer(
  protected val dataOpsManager: DataOpsManager
) : ContentSynchronizer {

  private val fileToExecutorMap = ConcurrentHashMap<VirtualFile, Pair<QueueExecutor<FetchCallback<Unit>, Unit>, FetchCallback<Unit>>>()

  protected abstract fun buildExecutorForFile(
    file: VirtualFile,
    saveStrategy: SaveStrategy
  ): QueueExecutor<FetchCallback<Unit>, Unit>

  protected val disposableQueues = Disposable {
    fileToExecutorMap.values.parallelStream().forEach { it.first.shutdown() }
    fileToExecutorMap.clear()
  }

  init {
    Disposer.register(dataOpsManager, disposableQueues)
    subscribe(
      componentManager = dataOpsManager.componentManager,
      topic = VirtualFileManager.VFS_CHANGES,
      handler = object : BulkFileListener {
        override fun after(events: MutableList<out VFileEvent>) {
          events
            .filterIsInstance<VFileContentChangeEvent>()
            .mapNotNull {
              val executor = fileToExecutorMap[it.file]
              if (executor != null && it.requestor !is ContentSynchronizer) {
                executor
              } else null
            }
            .forEach { it.first.accept(it.second) }
        }
      },
      disposable = disposableQueues
    )
  }

  override fun enforceSync(
    file: VirtualFile,
    acceptancePolicy: AcceptancePolicy,
    saveStrategy: SaveStrategy,
    onSyncEstablished: FetchCallback<Unit>
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
    fileToExecutorMap[file] = Pair(executor, onSyncEstablished)
    executor.accept(onSyncEstablished)
  }

  override fun isAlreadySynced(file: VirtualFile): Boolean {
    return fileToExecutorMap[file] != null
  }

  override fun removeSync(file: VirtualFile) {
    val pair = fileToExecutorMap[file]
      ?: throw IOException("Cannot remove sync for ${file.path} since it's not synchronized already")
    pair.first.shutdown()
    fileToExecutorMap.remove(file)
    pair.second.onFinish()
  }

}