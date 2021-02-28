package eu.ibagroup.formainframe.utils

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.*
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.serviceContainer.AlreadyDisposedException
import com.intellij.util.messages.Topic
import org.jetbrains.concurrency.*
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Dummy private constructor()

fun PluginManager.getPluginDescriptorByClass(clazz: Class<*>): IdeaPluginDescriptor? {
  return getPluginOrPlatformByClassName(clazz.name)?.let {
    findEnabledPlugin(it)
  }
}

val forMainframePluginDescriptor by lazy {
  PluginManager.getInstance().getPluginDescriptorByClass(Dummy::class.java)
    ?: throw IllegalStateException("Dummy class wasn't loaded by For Mainframe plugin's class loader for some reason")
}

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

fun <L : Any> subscribe(topic: Topic<L>, handler: L, disposable: Disposable) = ApplicationManager.getApplication()
  .messageBus
  .connect(disposable)
  .subscribe(topic, handler)

fun assertReadAllowed() = ApplicationManager.getApplication().assertReadAccessAllowed()

fun assertWriteAllowed() = ApplicationManager.getApplication().assertWriteAccessAllowed()

fun <T> submitOnWriteThread(block: () -> T): T {
  @Suppress("UnstableApiUsage")
  return AppUIExecutor.onWriteThread().submit(block).get()
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

inline fun runInEdtAndRead(crossinline block: () -> Unit) {
  return runInEdt { runReadAction { block() } }
}

fun AlreadyDisposedException(clazz: Class<*>) = AlreadyDisposedException("${clazz.name} is already disposed")

inline fun <reified S : Any> service(componentManager: ComponentManager): S {
  return componentManager.getService(S::class.java)
}

inline fun <reified S : Any> appService(): S {
  return service(ApplicationManager.getApplication())
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

fun <T> Result<T>.asDonePromise(): Promise<T> {
  val r = getOrNull()
  return if (r != null) {
    resolvedPromise(r)
  } else {
    rejectedPromise(exceptionOrNull())
  }
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