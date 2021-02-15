package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class RefreshNode : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
//    runWriteAction {
//      val currentlySelectedFile = e.getData(ConnectorWindowKeys.CURRENTLY_SELECTED_FILE)
//      if (currentlySelectedFile is ZSymlink) {
//        val fetchingDir = currentlySelectedFile.file
//        if (fetchingDir is ZRestFetchingDir<*>) {
//          val treeModel = e.getData(ConnectorWindowKeys.TREE_MODEL)
////          val currentDescriptor = e.getData(ConnectorWindowKeys.CURRENTLY_SELECTED_NODE_DESCRIPTOR)
//          fetchingDir.isNeedsFetching = true
//          fetchingDir.refresh(true, true) {
//            treeModel?.invalidate()
//          }
//        }
//      }
//    }
  }

  override fun update(e: AnActionEvent) {
//    val currentlySelectedFile = e.getData(ConnectorWindowKeys.CURRENTLY_SELECTED_FILE)
//    if (currentlySelectedFile is ZSymlink) {
//      if (currentlySelectedFile.file is ZRestFetchingDir<*>) {
//        e.presentation.isVisible = true
//        return
//      }
//    }
//    e.presentation.isVisible = false
  }
}