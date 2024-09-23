/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */

package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.xmlb.annotations.Attribute
import eu.ibagroup.formainframe.config.connect.ConnectionConfigBase
import javax.swing.JComponent

abstract class ExplorerContentProviderFactory<Connection: ConnectionConfigBase, E : Explorer<Connection, *>> {
  abstract fun buildComponent(): ExplorerContentProvider<Connection, E>

  @Attribute
  open var index: Int = 0
}

/** Explorer content provider interface to represent the basic fields needed to be initialized */
interface ExplorerContentProvider<Connection: ConnectionConfigBase, E : Explorer<Connection, *>> {

  companion object {
    @JvmField
    val EP =
      ExtensionPointName.create<ExplorerContentProviderFactory<*, *>>("eu.ibagroup.formainframe.explorerContentProvider")
  }

  val explorer: E

  // TODO: move to FileExplorerContentProvider?
  fun isFileInCutBuffer(virtualFile: VirtualFile): Boolean

  fun buildExplorerContent(parentDisposable: Disposable, project: Project): JComponent

  val displayName: String

  val isLockable: Boolean

}
