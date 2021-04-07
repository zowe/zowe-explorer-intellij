package eu.ibagroup.formainframe.config

import com.intellij.util.messages.Topic

interface SandboxListener {

  companion object {
    @JvmField
    val TOPIC = Topic.create("sandboxListener", SandboxListener::class.java)
  }

  fun <E : Any> update(clazz: Class<out E>)

  fun <E : Any> reload(clazz: Class<out E>)

}