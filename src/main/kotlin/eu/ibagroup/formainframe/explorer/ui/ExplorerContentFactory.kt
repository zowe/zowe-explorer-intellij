package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

interface ExplorerContentFactory {

  fun buildComponent(parentDisposable: Disposable, project: Project): JComponent

  val displayName: String

  val isLockable: Boolean

}