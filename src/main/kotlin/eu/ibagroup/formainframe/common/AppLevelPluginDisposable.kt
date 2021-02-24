package eu.ibagroup.formainframe.common

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager

val appLevelPluginDisposable
  get() = AppLevelPluginDisposable.instance

class AppLevelPluginDisposable: Disposable {

  companion object {
    @JvmStatic
    val instance: AppLevelPluginDisposable = ApplicationManager.getApplication().getService(AppLevelPluginDisposable::class.java)
  }

  override fun dispose() {
    println("DISPOOOOOOSEEEEEE")
  }

}