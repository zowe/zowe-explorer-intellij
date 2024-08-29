/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.utils

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import org.jetbrains.annotations.Nls
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

val cachesDir by lazy {
  val cachesDirString = System.getProperty("caches_dir")
  return@lazy File(cachesDirString ?: (PathManager.getSystemPath() + "/caches/"))
}

/**
 * Retrieve the topic sync publisher to publish messages on the application bus
 * @param topic the topic to get sync publisher for
 * @param componentManager the component manager to get the topic sync publisher
 */
fun <L : Any> sendTopic(
  topic: Topic<L>,
  componentManager: ComponentManager = ApplicationManager.getApplication()
): L {
  return componentManager.messageBus.syncPublisher(topic)
}

/**
 * Retrieve the topic sync publisher to publish messages on the project bus
 * @param topic the topic to get sync publisher for
 * @param project the project to get the topic sync publisher
 */
fun <L : Any> sendTopic(topic: Topic<L>, project: Project): L {
  return project.messageBus.syncPublisher(topic)
}

/**
 * Subscribe to the specified topic with disposable object to listen messages from the application bus
 * @param topic the topic to subscribe to
 * @param handler the handler to execute on topic message
 * @param disposable target parent disposable to which life cycle newly created connection shall be bound
 * @param componentManager the component manager to retrieve the message bus to subscribe to the topic
 */
fun <L : Any> subscribe(
  topic: Topic<L>,
  handler: L,
  disposable: Disposable,
  componentManager: ComponentManager = ApplicationManager.getApplication()
) = componentManager
  .messageBus
  .connect(disposable)
  .subscribe(topic, handler)

/**
 * Subscribe to the specified topic to listen messages from the application bus
 * @param topic the topic to subscribe to
 * @param handler the handler to execute on topic message
 * @param componentManager the component manager to retrieve the message bus to subscribe to the topic
 */
fun <L : Any> subscribe(
  topic: Topic<L>,
  handler: L,
  componentManager: ComponentManager = ApplicationManager.getApplication()
) = componentManager
  .messageBus
  .connect()
  .subscribe(topic, handler)

/**
 * Subscribe to the specified topic with disposable object to listen messages from the project bus
 * @param topic the topic to subscribe to
 * @param handler the handler to execute on topic message
 * @param disposable target parent disposable to which life cycle newly created connection shall be bound
 * @param project the project to retrieve the message bus to subscribe to the topic
 */
fun <L : Any> subscribe(topic: Topic<L>, handler: L, disposable: Disposable, project: Project) = project
  .messageBus
  .connect(disposable)
  .subscribe(topic, handler)

/**
 * Subscribe to the specified topic to listen messages from the project bus
 * @param topic the topic to subscribe to
 * @param handler the handler to execute on topic message
 * @param project the project to retrieve the message bus to subscribe to the topic
 */
fun <L : Any> subscribe(topic: Topic<L>, handler: L, project: Project) = project
  .messageBus
  .connect()
  .subscribe(topic, handler)

/** Asserts whether write access is allowed */
fun assertWriteAllowed() = ApplicationManager.getApplication().assertWriteAccessAllowed()

inline fun <reified S : Any> ComponentManager.service(): S {
  return getService(S::class.java)
}

inline fun <reified S : Any> ComponentManager.component(): S {
  return getComponent(S::class.java)
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

/**
 * Run the specified task in with progress
 * @param title the title of the task to be shown in the progress
 * @param project the optional project value to make the specific project handling
 * @param cancellable the value to specify if the task is cancellable
 * @param task the task to execute
 */
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

fun runInEdtAndWait(runnable: () -> Unit) {
  ApplicationManager.getApplication().invokeAndWait(runnable)
}

fun runWriteActionInEdt(block: () -> Unit) {
  runInEdt {
    runWriteAction(block)
  }
}

fun runWriteActionInEdtAndWait(block: () -> Unit) {
  runInEdtAndWait {
    runWriteAction(block)
  }
}

/** Return the specified logger instance */
inline fun <reified T : Any> log(): Logger {
  return logger<T>()
}

fun VirtualFile.getParentsChain(): List<VirtualFile> {
  return getParentsChain { parent }
}

fun VirtualFile.getAncestorNodes(): List<VirtualFile> {
  return getAncestorNodes { children?.asIterable() ?: emptyList() }
}

/** Check is the file being edited now */
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

/** Enumeration class with mainframe file types */
enum class MfFileType {
  USS, DATASET, MEMBER
}

/** Class to resolve the mainframe file path */
class MfFilePath(
  private val mfFileType: MfFileType,
  val filePath: String,
  val memberName: String? = null
) {
  override fun toString(): String {
    return when (mfFileType) {
      MfFileType.USS -> filePath
      MfFileType.DATASET -> "//'${filePath}'"
      else -> "//'${filePath}(${memberName})'"
    }
  }
}

/** Get the formed mainframe file path */
fun FileAttributes.formMfPath(): String {
  val fileType = when (this) {
    is RemoteUssAttributes -> MfFileType.USS
    is RemoteDatasetAttributes -> MfFileType.DATASET
    else -> MfFileType.MEMBER
  }
  val filePath = when (this) {
    is RemoteMemberAttributes -> parentFile.name
    is RemoteUssAttributes -> path
    else -> name
  }
  val memberName = if (this is RemoteMemberAttributes) name else null
  return MfFilePath(fileType, filePath, memberName).toString()
}
