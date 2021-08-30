/*
 * This is property of IBA Group
 */

package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.xmlb.annotations.Attribute
import javax.swing.JComponent

abstract class ExplorerContentProviderFactory<E : Explorer> {
  abstract fun buildComponent(): ExplorerContentProvider<E>

  @Attribute
  open var index: Int = 0
}

interface ExplorerContentProvider<E : Explorer> {

  companion object {
    @JvmField
    val EP =
      ExtensionPointName.create<ExplorerContentProviderFactory<*>>("eu.ibagroup.formainframe.explorerContentProvider")
  }

  val explorer: E

  fun isFileInCutBuffer(virtualFile: VirtualFile): Boolean

  fun buildExplorerContent(parentDisposable: Disposable, project: Project): JComponent

  val displayName: String

  val isLockable: Boolean

}

