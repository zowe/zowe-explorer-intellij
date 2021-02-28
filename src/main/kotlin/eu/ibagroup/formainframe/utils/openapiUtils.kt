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
import java.io.File

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

inline fun <T> runWriteActionOnWriteThread(crossinline block: () -> T): T {
  @Suppress("UnstableApiUsage")
  return if (ApplicationManager.getApplication().isWriteThread) {
    runWriteAction(block)
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
