/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.utils

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.*
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.serviceContainer.AlreadyDisposedException
import com.intellij.util.messages.Topic
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.FileAttributes
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.attributes.RemoteMemberAttributes
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.apache.log4j.Level
import org.jetbrains.annotations.Nls
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.CancellablePromise
import org.jetbrains.concurrency.Promise
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Dummy private constructor()

val cachesDir by lazy {
  val cachesDirString = System.getProperty("caches_dir")
  val cachesDir = File(cachesDirString ?: PathManager.getSystemPath() + "/caches/")
  return@lazy cachesDir
}

fun <L> sendTopic(
  topic: Topic<L>,
  componentManager: ComponentManager = ApplicationManager.getApplication()
): L {
  return componentManager.messageBus.syncPublisher(topic)
}

fun <L : Any> subscribe(
  componentManager: ComponentManager,
  topic: Topic<L>,
  handler: L,
  disposable: Disposable
) = componentManager
  .messageBus
  .connect(disposable)
  .subscribe(topic, handler)

fun <L : Any> subscribe(
  componentManager: ComponentManager,
  topic: Topic<L>,
  handler: L
) = componentManager
  .messageBus
  .connect()
  .subscribe(topic, handler)

fun <L : Any> subscribe(topic: Topic<L>, handler: L) = ApplicationManager.getApplication()
  .messageBus
  .connect()
  .subscribe(topic, handler)

fun <L : Any> subscribe(topic: Topic<L>, disposable: Disposable, handler: L) = ApplicationManager.getApplication()
  .messageBus
  .connect(disposable)
  .subscribe(topic, handler)

fun assertReadAllowed() = ApplicationManager.getApplication().assertReadAccessAllowed()

fun assertWriteAllowed() = ApplicationManager.getApplication().assertWriteAccessAllowed()

fun <T> submitOnWriteThread(block: () -> T): T {
  @Suppress("UnstableApiUsage")
  return AppUIExecutor.onWriteThread(ModalityState.defaultModalityState()).submit(block).get()
}

@Suppress("UnstableApiUsage")
inline fun <T> runWriteActionOnWriteThread(crossinline block: () -> T): T {
  val app = ApplicationManager.getApplication()
  return if (app.isWriteThread) {
    if (app.isWriteAccessAllowed) {
      block()
    } else {
      runWriteAction(block)
    }
  } else
    submitOnWriteThread {
      runWriteAction(block)
    }
}

inline fun <T> runReadActionInEdtAndWait(crossinline block: () -> T): T {
  return invokeAndWaitIfNeeded { runReadAction(block) }
}

fun AlreadyDisposedException(clazz: Class<*>) = AlreadyDisposedException("${clazz.name} is already disposed")

inline fun <reified S : Any> ComponentManager.service(): S {
  return getService(S::class.java)
}

inline fun <reified S : Any> ComponentManager.component(): S {
  return getComponent(S::class.java)
}

inline fun <T> runPromiseAsBackgroundTask(
  title: String,
  project: Project? = null,
  canBeCancelled: Boolean = true,
  needsToCancelPromise: Boolean = false,
  crossinline promiseGetter: (ProgressIndicator) -> Promise<T>
) {
  ProgressManager.getInstance().run(object : Task.Backgroundable(project, title, canBeCancelled) {
    private var promise: Promise<T>? = null
    override fun run(indicator: ProgressIndicator) {
      val lock = ReentrantLock()
      val condition = lock.newCondition()
      promise = promiseGetter(indicator).also {
        it.onProcessed {
          lock.withLock { condition.signalAll() }
        }
      }
      lock.withLock { condition.await() }
    }

    override fun onCancel() {
      if (needsToCancelPromise) {
        promise.castOrNull<CancellablePromise<*>>()?.cancel()
      }
    }
  })
}


fun <T> Promise<T>.get(): T? {
  return if (this is AsyncPromise<T>) {
    get()
  } else {
    val lock = ReentrantLock()
    val condition = lock.newCondition()
    var value: T? = null
    var throwable: Throwable? = null
    var isSuccess = false
    onSuccess {
      isSuccess = true
      value = it
      lock.withLock {
        condition.signalAll()
      }
    }
    onError {
      throwable = it
      lock.withLock {
        condition.signalAll()
      }
    }
    lock.withLock { condition.await() }
    if (isSuccess) {
      value
    } else {
      throw throwable ?: Throwable()
    }
  }
}

inline fun <reified T> runTask(
  @Nls(capitalization = Nls.Capitalization.Sentence) title: String,
  project: Project? = null,
  cancellable: Boolean = true,
  crossinline task: (ProgressIndicator) -> T
): T {
  return ProgressManager.getInstance().run(object : Task.WithResult<T, Exception>(project, title, cancellable) {
    override fun compute(indicator: ProgressIndicator): T {
      return task(indicator)
    }
  })
}

inline fun <reified S : Any> ComponentManager.hasService(): Boolean {
  return picoContainer.getComponentInstance(S::class.java.name) != null
}

inline fun runWriteActionInEdt(crossinline block: () -> Unit) {
  runInEdt {
    runWriteAction(block)
  }
}

inline fun runWriteActionInEdtAndWait(crossinline block: () -> Unit) {
  invokeAndWaitIfNeeded {
    runWriteAction(block)
  }
}

inline fun <reified T : Any> log(): Logger {
  return logger<T>()
}

fun VirtualFile.getParentsChain(): List<VirtualFile> {
  return getParentsChain { parent }
}

fun VirtualFile.getAncestorNodes(): List<VirtualFile> {
  return getAncestorNodes { children?.asIterable() ?: emptyList() }
}

fun VirtualFile.isBeingEditingNow(): Boolean {
  return ProjectManager
    .getInstance()
    .openProjects
    .map { FileEditorManager.getInstance(it).getAllEditors(this).toList() }
    .flatten()
    .isNotEmpty()
}

fun Iterable<VirtualFile>.getMinimalCommonParents(): Collection<VirtualFile> {
  return getMinimalCommonParents { parent }
}

val minimalParentComparator: Comparator<VirtualFile> = Comparator { o1, o2 ->
  val firstParentChain = o1.getParentsChain()
  val secondParentsChain = o2.getParentsChain()
  return@Comparator when {
    firstParentChain.containsAll(secondParentsChain) -> 1
    secondParentsChain.containsAll(firstParentChain) -> -1
    else -> 0
  }
}

enum class MfFileType {
  USS, DATASET, MEMBER
}

class MfFilePath(
  val mfFileType: MfFileType,
  val filePath: String,
  val memberName: String? = null
) {
  override fun toString(): String {
    return when(mfFileType) {
      MfFileType.USS -> filePath
      MfFileType.DATASET -> "//'${filePath}'"
      else -> "//'${filePath}(${memberName})'"
    }
  }
}

fun FileAttributes.formMfPath(): String {
  val fileType = when(this) {
    is RemoteUssAttributes -> MfFileType.USS
    is RemoteDatasetAttributes -> MfFileType.DATASET
    else -> MfFileType.MEMBER
  }
  val filePath = when(this) {
    is RemoteMemberAttributes -> parentFile.name
    is RemoteUssAttributes -> path
    else -> name
  }
  val memberName = if (this is RemoteMemberAttributes) name else null
  return MfFilePath(fileType, filePath, memberName).toString()
}
