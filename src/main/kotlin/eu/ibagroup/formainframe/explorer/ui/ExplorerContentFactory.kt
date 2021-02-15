package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.Disposable
import javax.swing.JComponent

interface ExplorerContentFactory {

  fun buildComponent(parentDisposable: Disposable): JComponent

  val displayName: String

  val isLockable: Boolean

}