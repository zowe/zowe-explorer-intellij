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

package eu.ibagroup.formainframe.explorer.ui

import com.intellij.ide.dnd.*
import com.intellij.ide.projectView.impl.ProjectViewImpl
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.awt.RelativeRectangle
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ArrayUtilRt
import eu.ibagroup.formainframe.common.ui.getVirtualFile
import eu.ibagroup.formainframe.common.ui.makeNodeDataFromTreePath
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.attributes.*
import eu.ibagroup.formainframe.explorer.Explorer
import groovy.lang.Tuple4
import java.awt.Rectangle
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

val IS_DRAG_AND_DROP_KEY = DataKey.create<Boolean>("IsDropKey")
val DRAGGED_FROM_PROJECT_FILES_ARRAY = DataKey.create<List<VirtualFile>>("DraggedFilesFromProject")

/**
 * Performs copying files with drag&drop operation.
 * @param myTree file explorer tree.
 * @param explorer file explorer instance (logical representation of explorer view data).
 * @param copyPasteSupport explorer copy/paste support.
 * @author Valiantsin Krus
 */
class FileExplorerViewDropTarget(
  private val myTree: Tree,
  val explorer: Explorer<ConnectionConfig, *>,
  private val copyPasteSupport: FileExplorerView.ExplorerCopyPasteSupport
) : DnDNativeTarget {

  /**
   * Checks if files are copying between different systems (but not between remote and local).
   * @param sources tree paths corresponding for files to copy.
   * @param target tree path corresponding for file to copy to.
   * @return true if it is crossystem copy or false otherwise.
   */
  @Suppress("UNCHECKED_CAST")
  private fun isCrossSystemCopy(sources: Collection<TreePath?>, target: TreePath): Boolean {

    val sourceFilesAttributes = sources
      .mapNotNull { runCatching { makeNodeDataFromTreePath(explorer, it) }.getOrNull()?.attributes }
      .mapNotNull { if (it is RemoteMemberAttributes) it.getLibraryAttributes(service()) else it }
      .mapNotNull { runCatching { it as MFRemoteFileAttributes<ConnectionConfig, Requester<ConnectionConfig>> }.getOrNull() }

    val targetAttributes = runCatching {
      makeNodeDataFromTreePath(explorer, target)?.attributes as MFRemoteFileAttributes<ConnectionConfig, Requester<ConnectionConfig>>?
    }.getOrNull() ?: return false

    return sourceFilesAttributes.any {
      it.findCommonUrlConnections(targetAttributes).isEmpty()
    }
  }

  /**
   * Extracts tree paths corresponding for files to copy from transferable wrapper.
   * @param transferData can be any object, but actually function works only with TransferableWrapper.
   * @see TransferableWrapper
   * @return extracted tree paths array.
   */
  private fun getSourcePaths(transferData: Any): Array<TreePath?>? {
    val wrapper = if (transferData is TransferableWrapper) transferData else null
    return wrapper?.treePaths
  }

  /**
   * Extracts source tree paths, target path, bounds of target node
   * and tree containing target node from drag&drop event.
   * @param event drage&drop event from which to extract necessary data.
   * @return above information in the specified order inside Tuple4.
   */
  private fun getSourcesTargetAndBounds(event: DnDEvent): Tuple4<Array<TreePath?>?, TreePath, Rectangle, JTree>? {
    event.setDropPossible(false, "")

    val point = event.point ?: return null

    val treeToUpdate: JTree

    val projectTree = getProjectTree()
    treeToUpdate = if (event.currentOverComponent == myTree) {
      myTree
    } else if (event.currentOverComponent == projectTree && projectTree != null) {
      projectTree
    } else {
      return null
    }
    val target: TreePath = treeToUpdate.getClosestPathForLocation(point.x, point.y) ?: return null

    val bounds = treeToUpdate.getPathBounds(target)
    if (bounds == null || bounds.y > point.y || point.y >= bounds.y + bounds.height) return null

    val sources = getSourcePaths(event.attachedObject)
    return Tuple4(sources, target, bounds, treeToUpdate)
  }

  /**
   * Proceed copying of dragged files.
   * @param event arisen drag&drop event.
   */
  override fun drop(event: DnDEvent) {
    val sourcesTargetBounds = getSourcesTargetAndBounds(event) ?: return

    val pasteProvider = copyPasteSupport.pasteProvider
    val cutProvider = copyPasteSupport.cutProvider
    val copyProvider = copyPasteSupport.copyProvider
    val sourceTreePaths = sourcesTargetBounds.v1?.toList() ?: listOf()

    val isCopiedFromRemote = event.attachedObject is FileExplorerViewDragSource.ExplorerTransferableWrapper
    val isCopiedToRemote = sourcesTargetBounds.v4 == myTree

    val copyCutContext = DataContext {
      when (it) {
        CommonDataKeys.PROJECT.name -> copyPasteSupport.project
        ExplorerDataKeys.NODE_DATA_ARRAY.name -> sourceTreePaths
          .mapNotNull { treePath ->
            if ((treePath?.lastPathComponent as DefaultMutableTreeNode).userObject is ExplorerTreeNode<*, *>)
              makeNodeDataFromTreePath(explorer, treePath)
            else null
          }.toTypedArray()

        CommonDataKeys.VIRTUAL_FILE_ARRAY.name -> {
          if (sourcesTargetBounds.v4 == myTree) {
            arrayOf(makeNodeDataFromTreePath(explorer, sourcesTargetBounds.v2)?.file).filterNotNull().toTypedArray()
          } else {
            arrayOf(sourcesTargetBounds.v2.getVirtualFile()).filterNotNull().toTypedArray()
          }
        }

        IS_DRAG_AND_DROP_KEY.name -> true
        DRAGGED_FROM_PROJECT_FILES_ARRAY.name -> {
          if (isCopiedFromRemote) {
            emptyList()
          } else {
            sourcesTargetBounds.v1?.mapNotNull { treePath -> treePath?.getVirtualFile() }
          }
        }

        else -> null
      }
    }
    if (isCopiedFromRemote && isCopiedToRemote && !isCrossSystemCopy(sourceTreePaths, sourcesTargetBounds.v2)) {
      if (cutProvider.isCutEnabled(copyCutContext)) {
        cutProvider.performCut(copyCutContext)
      }
    } else {
      if (copyProvider.isCopyEnabled(copyCutContext)) {
        copyProvider.performCopy(copyCutContext)
      }
    }
    pasteProvider.performPaste(copyCutContext)
  }

  /**
   * Extracts the project tree.
   * @return project tree if it was initialized or null otherwise.
   */
  private fun getProjectTree(): JTree? {
    return runCatching {
      copyPasteSupport.project?.let { ProjectViewImpl.getInstance(it).currentProjectViewPane.tree }
    }.getOrNull()
  }

  /**
   * Updates the state of mouse and dragged files in UI. For example,
   * the mouse sign will be forbidding if it is not possible to copy
   * dragged files on the covered by mouse tree node.
   * @see DnDTargetChecker.update
   * @param event arisen drag&drop event.
   * @return true if this target is unable to handle the event and parent component should be asked to process it.
   *         false if this target is able to handle the event and parent component should NOT be asked to process it.
   */
  override fun update(event: DnDEvent): Boolean {
    val sourcesTargetBounds = getSourcesTargetAndBounds(event) ?: return false
    val sources = sourcesTargetBounds.v1 ?: return false
    if (
      ArrayUtilRt.find(sources, sourcesTargetBounds.v2) != -1
      || !FileCopyPasteUtil.isFileListFlavorAvailable(event)
    ) {
      return false
    }

    val isCopiedFromRemote = event.attachedObject is FileExplorerViewDragSource.ExplorerTransferableWrapper
    val pasteEnabled = if (isCopiedFromRemote && sourcesTargetBounds.v4 === getProjectTree()) {
      val vFile = sourcesTargetBounds.v2.getVirtualFile()
      if (vFile == null) {
        false
      } else {
        copyPasteSupport.isPastePossible(listOf(vFile), sources.mapNotNull { makeNodeDataFromTreePath(explorer, it) })
      }
    } else if (!isCopiedFromRemote && sourcesTargetBounds.v4 == myTree) {
      val sourceFiles = sources.mapNotNull { it?.getVirtualFile() }
      val target =
        makeNodeDataFromTreePath(explorer, sourcesTargetBounds.v2)?.file?.let { listOf(it) } ?: emptyList()
      copyPasteSupport.isPastePossibleForFiles(target, sourceFiles)
    } else if (sourcesTargetBounds.v4 == myTree) {
      copyPasteSupport.isPastePossibleFromPath(listOf(sourcesTargetBounds.v2), sources.toList())
    } else false

    event.isDropPossible = pasteEnabled
    if (pasteEnabled) {
      event.setHighlighting(
        RelativeRectangle(sourcesTargetBounds.v4, sourcesTargetBounds.v3),
        DnDEvent.DropTargetHighlightingType.RECTANGLE
      )
    }
    return false
  }

}
