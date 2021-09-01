/*
 * This is property of IBA Group
 */

package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager

interface UIComponentManager: Disposable {

  companion object {
    val INSTANCE = ApplicationManager.getApplication().getService(UIComponentManager::class.java)
  }

  fun <E : Explorer<*>> getExplorerContentProviders() : List<ExplorerContentProvider<E>>

  fun <E : Explorer<*>> getExplorerContentProvider(clazz: Class<out E>) : ExplorerContentProvider<E>

  fun <E : Explorer<*>> getExplorer(clazz: Class<out E>) : E

}
