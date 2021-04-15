package eu.ibagroup.formainframe.dataops.synchronizer

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.impl.TrailingSpacesStripper
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.PsiDocumentListener
import com.intellij.util.concurrency.AppExecutorUtil
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.AttributesService
import eu.ibagroup.formainframe.dataops.attributes.VFileInfoAttributes
import eu.ibagroup.formainframe.dataops.attributes.attributesListener
import eu.ibagroup.formainframe.utils.QueueExecutor
import eu.ibagroup.formainframe.utils.runReadActionInEdtAndWait
import eu.ibagroup.formainframe.utils.subscribe
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

abstract class AbstractQueuedContentSynchronizer(
  protected val dataOpsManager: DataOpsManager
) : ContentSynchronizer {

  private val fileToExecutorMap = ConcurrentHashMap<VirtualFile, QueueExecutor<Unit>>()
  private val fileToDocumentListenerMap = ConcurrentHashMap<VirtualFile, DocumentListener>()

  protected abstract fun buildExecutorForFile(
    providerFactory: (QueueExecutor<Unit>) -> SyncProvider
  ): Pair<QueueExecutor<Unit>, SyncProvider>

  protected val disposableQueues = Disposable {
    fileToExecutorMap.values.parallelStream().forEach { it.shutdown() }
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
            .forEach {
              it.accept(Unit)
            }
        }
      },
      disposable = disposableQueues
    )
    subscribe(
      componentManager = dataOpsManager.componentManager,
      topic = AttributesService.ATTRIBUTES_CHANGED,
      handler = attributesListener<VFileInfoAttributes, VirtualFile> {
        onUpdate { _, _, file ->
          if (isAlreadySynced(file)) {
            triggerSync(file)
          }
        }
        onDelete { _, file ->
          removeSync(file)
        }
      }
    )
  }

  override fun startSync(
    file: VirtualFile,
    project: Project,
    acceptancePolicy: AcceptancePolicy,
    saveStrategy: SaveStrategy,
    removeSyncOnThrowable: (file: VirtualFile, t: Throwable) -> Boolean,
    progressIndicator: ProgressIndicator
  ) {
    if (file.isDirectory) {
      throw IllegalArgumentException("Directories cannot be synced")
    }
    if (!file.isValid || !file.exists()) {
      throw IllegalArgumentException("Non-valid or non-existing files cannot be synced")
    }
    if (!accepts(file)) {
      throw IllegalArgumentException("${this::class.java} cannot accept file ${file.path} for syncing")
    }
    val existingExecutor = fileToExecutorMap[file]
    if (existingExecutor != null) {
      throw IllegalArgumentException("File ${file.path} is already synced")
    }
    val length = file.length
    if (length != 0L && acceptancePolicy == AcceptancePolicy.IF_EMPTY_ONLY) {
      throw IllegalArgumentException("Cannot sync non-empty file due provided acceptance policy $acceptancePolicy")
    }
    TrailingSpacesStripper.setEnabled(file, false)
    val configFactory = { executor: QueueExecutor<Unit> ->
      QueueSyncProvider(
        file = file,
        progressIndicator = progressIndicator,
        saveStrategy = saveStrategy,
        queueExecutor = executor,
        synchronizer = this,
        removeSyncOnThrowable = { f, t ->
          if (f.isValid) {
            removeSyncOnThrowable(f, t)
          } else {
            true
          }
        }
      )
    }
    val (executor, syncConfig) = buildExecutorForFile(configFactory)
    val documentListener = object : DocumentListener {
      override fun documentChanged(event: DocumentEvent) {
        executor.accept(Unit)
      }
    }
    fileToExecutorMap[file] = executor
    executor.accept(Unit)
    syncConfig.waitForSyncStarted()
    AppExecutorUtil.getAppExecutorService().submit {
      val lock = ReentrantLock()
      val condition = lock.newCondition()
      var document = runReadActionInEdtAndWait { FileDocumentManager.getInstance().getDocument(file) }
      if (document == null) {
        lock.withLock {
          document = runReadActionInEdtAndWait { FileDocumentManager.getInstance().getDocument(file) }
          if (document != null) {
            condition.signalAll()
          }
        }
      }
      subscribe(
        componentManager = project,
        topic = PsiDocumentListener.TOPIC,
        handler = PsiDocumentListener { doc, _, _ ->
          val docFile = FileDocumentManager.getInstance().getFile(doc)
          if (docFile == file) {
            lock.withLock {
              document = doc
              condition.signalAll()
            }
          }
        }
      )
      while (document == null) {
        lock.withLock {
          condition.await()
        }
      }
      document?.addDocumentListener(documentListener)
      fileToDocumentListenerMap[file] = documentListener
    }
  }

  override fun isAlreadySynced(file: VirtualFile): Boolean {
    return fileToExecutorMap[file] != null
  }

  override fun triggerSync(file: VirtualFile) {
    val executor = fileToExecutorMap[file] ?: throw IllegalArgumentException("File ${file.path} is not synced")
    executor.accept(Unit)
  }

  override fun removeSync(file: VirtualFile) {
    val executor = fileToExecutorMap[file] ?: return
    executor.shutdown()
    fileToExecutorMap.remove(file)
    fileToDocumentListenerMap.remove(file)
  }

}