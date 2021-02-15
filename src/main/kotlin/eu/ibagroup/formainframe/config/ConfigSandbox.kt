package eu.ibagroup.formainframe.config

import com.intellij.openapi.application.ApplicationManager
import eu.ibagroup.formainframe.utils.crudable.Crudable

interface ConfigSandbox {

  companion object {
    @JvmStatic
    val instance: ConfigSandbox = ApplicationManager.getApplication().getService(ConfigSandbox::class.java)
  }

  fun <T : Any> apply(clazz: Class<out T>)

  fun fetch()

  fun <T> rollback(clazz: Class<out T>)

  fun <T> isModified(clazz: Class<out T>): Boolean

  val crudable: Crudable

}

val sandboxCrudable get() = ConfigSandbox.instance.crudable

inline fun <reified T : Any> applySandbox() {
  ConfigSandbox.instance.apply(T::class.java)
}