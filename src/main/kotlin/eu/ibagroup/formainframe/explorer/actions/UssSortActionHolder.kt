/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

// TODO: too much boilerplate
package eu.ibagroup.formainframe.explorer.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.components.service
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.SortQueryKeys
import eu.ibagroup.formainframe.dataops.UnitRemoteQueryImpl
import eu.ibagroup.formainframe.dataops.fetch.UssQuery
import eu.ibagroup.formainframe.explorer.ui.FileExplorerView
import eu.ibagroup.formainframe.explorer.ui.UssDirNode
import eu.ibagroup.formainframe.explorer.ui.getExplorerView
import eu.ibagroup.formainframe.vfs.MFVirtualFile

/**
 * Represents internal USS fetch provider to be able to update the query for each USS dir node whose sorting is enabled
 */
internal val fetchProvider = service<DataOpsManager>()
  .getFileFetchProvider(
    UssQuery::class.java,
    RemoteQuery::class.java,
    MFVirtualFile::class.java
  )

/**
 * Represents the custom sort action group in the FileExplorerView context menu
 */
class SortActionGroup : DefaultActionGroup() {

  /**
   * Update method to determine if sorting is possible for particular item in the tree
   */
  override fun update(e: AnActionEvent) {
    val view = e.getExplorerView<FileExplorerView>()
    view ?: let {
      e.presentation.isEnabledAndVisible = false
      return@let
    }
    val selectedNode = view?.mySelectedNodesData ?: return
    val treePathFromModel = view.myTree.selectionPath
    e.presentation.isEnabledAndVisible = selectedNode.any {
      it.node is UssDirNode && view.myTree.isExpanded(treePathFromModel)
    }
  }
}

/**
 * Custom action for sorting by item Name (Dir or File name in case of USS)
 */
class SortByNameAction : ToggleAction() {

  /**
   * Update method to determine if sorting by Name is possible for particular item in the tree
   */
  override fun update(e: AnActionEvent) {
    super.update(e)
    e.presentation.isEnabledAndVisible = true
  }

  /**
   * Action performed method to register the custom behavior when By Name sorting is clicked
   */
  override fun setSelected(e: AnActionEvent, state: Boolean) {
    val view = e.getExplorerView<FileExplorerView>() ?: return
    val selectedNode = view.mySelectedNodesData[0].node
    if (selectedNode is UssDirNode) {
      val queryToUpdate = (selectedNode.query as UnitRemoteQueryImpl)
      if (selectedNode.currentSortQueryKeysList.isEmpty()) {
        selectedNode.currentSortQueryKeysList.add(SortQueryKeys.DATE)
        selectedNode.currentSortQueryKeysList.add(SortQueryKeys.ASCENDING)
        queryToUpdate.sortKeys.addAll(selectedNode.currentSortQueryKeysList)
      } else {
        selectedNode.currentSortQueryKeysList.remove(SortQueryKeys.TYPE)
        selectedNode.currentSortQueryKeysList.remove(SortQueryKeys.DATE)
        if (!selectedNode.currentSortQueryKeysList.contains(SortQueryKeys.NAME))
          selectedNode.currentSortQueryKeysList.add(SortQueryKeys.NAME)
        queryToUpdate.sortKeys.addAll(selectedNode.currentSortQueryKeysList)
      }
      queryToUpdate.requester = selectedNode
      selectedNode.cleanCache(false)
      fetchProvider.reload(queryToUpdate)
    }
  }

  /**
   * Custom isSelected method determines if the sorting By Name is currently enabled or not. Updates UI by 'tick' mark
   */
  override fun isSelected(e: AnActionEvent): Boolean {
    val view = e.getExplorerView<FileExplorerView>() ?: return false
    val selectedNode = view.mySelectedNodesData[0].node
    if (selectedNode is UssDirNode) {
      return selectedNode.currentSortQueryKeysList.contains(SortQueryKeys.NAME)
    }
    return false
  }

  /**
   * If action is dumb aware or not
   */
  override fun isDumbAware(): Boolean {
    return true
  }
}

/**
 * Custom action for sorting by item Type (Dir or File type+name in case of USS)
 */
class SortByTypeAction : ToggleAction() {

  override fun update(e: AnActionEvent) {
    super.update(e)
    e.presentation.isEnabledAndVisible = true
  }

  /**
   * Action performed method to register the custom behavior when By Type sorting is clicked
   */
  override fun setSelected(e: AnActionEvent, state: Boolean) {
    val view = e.getExplorerView<FileExplorerView>() ?: return
    val selectedNode = view.mySelectedNodesData[0].node
    if (selectedNode is UssDirNode) {
      val queryToUpdate = (selectedNode.query as UnitRemoteQueryImpl)
      if (selectedNode.currentSortQueryKeysList.isEmpty()) {
        selectedNode.currentSortQueryKeysList.add(SortQueryKeys.DATE)
        selectedNode.currentSortQueryKeysList.add(SortQueryKeys.ASCENDING)
        queryToUpdate.sortKeys.addAll(selectedNode.currentSortQueryKeysList)
      } else {
        selectedNode.currentSortQueryKeysList.remove(SortQueryKeys.NAME)
        selectedNode.currentSortQueryKeysList.remove(SortQueryKeys.DATE)
        if (!selectedNode.currentSortQueryKeysList.contains(SortQueryKeys.TYPE))
          selectedNode.currentSortQueryKeysList.add(SortQueryKeys.TYPE)
        queryToUpdate.sortKeys.addAll(selectedNode.currentSortQueryKeysList)
      }
      queryToUpdate.requester = selectedNode
      selectedNode.cleanCache(false)
      fetchProvider.reload(queryToUpdate)
    }
  }

  /**
   * Custom isSelected method determines if the sorting By Type is currently enabled or not. Updates UI by 'tick' mark
   */
  override fun isSelected(e: AnActionEvent): Boolean {
    val view = e.getExplorerView<FileExplorerView>() ?: return false
    val selectedNode = view.mySelectedNodesData[0].node
    if (selectedNode is UssDirNode) {
      return selectedNode.currentSortQueryKeysList.contains(SortQueryKeys.TYPE)
    }
    return false
  }

  /**
   * If action is dumb aware or not
   */
  override fun isDumbAware(): Boolean {
    return true
  }
}

/**
 * Custom action for sorting by item Modification Date (Dir or File modify date in case of USS)
 */
class SortByModificationDateAction : ToggleAction() {

  override fun update(e: AnActionEvent) {
    super.update(e)
    e.presentation.isEnabledAndVisible = true
  }

  /**
   * Action performed method to register the custom behavior when By Modification Date sorting is clicked
   */
  override fun setSelected(e: AnActionEvent, state: Boolean) {
    val view = e.getExplorerView<FileExplorerView>() ?: return
    val selectedNode = view.mySelectedNodesData[0].node
    if (selectedNode is UssDirNode) {
      val queryToUpdate = (selectedNode.query as UnitRemoteQueryImpl)
      if (selectedNode.currentSortQueryKeysList.isEmpty()) {
        selectedNode.currentSortQueryKeysList.add(SortQueryKeys.DATE)
        selectedNode.currentSortQueryKeysList.add(SortQueryKeys.ASCENDING)
        queryToUpdate.sortKeys.addAll(selectedNode.currentSortQueryKeysList)
      } else {
        selectedNode.currentSortQueryKeysList.remove(SortQueryKeys.NAME)
        selectedNode.currentSortQueryKeysList.remove(SortQueryKeys.TYPE)
        if (!selectedNode.currentSortQueryKeysList.contains(SortQueryKeys.DATE))
          selectedNode.currentSortQueryKeysList.add(SortQueryKeys.DATE)
        queryToUpdate.sortKeys.addAll(selectedNode.currentSortQueryKeysList)
      }
      queryToUpdate.requester = selectedNode
      selectedNode.cleanCache(false)
      fetchProvider.reload(queryToUpdate)
    }
  }

  /**
   * Custom isSelected method determines if the sorting By Modification Date is currently enabled or not. Updates UI by 'tick' mark
   */
  override fun isSelected(e: AnActionEvent): Boolean {
    val view = e.getExplorerView<FileExplorerView>() ?: return false
    val selectedNode = view.mySelectedNodesData[0].node
    if (selectedNode is UssDirNode) {
      return selectedNode.currentSortQueryKeysList.contains(SortQueryKeys.DATE)
    }
    return false
  }

  /**
   * If action is dumb aware or not
   */
  override fun isDumbAware(): Boolean {
    return true
  }
}

/**
 * Custom action for sorting by item Name/Type/Modify Date in ascending order (Dir or File name in case of USS)
 */
class SortByAscendingOrderAction : ToggleAction() {

  override fun update(e: AnActionEvent) {
    super.update(e)
    e.presentation.isEnabledAndVisible = true
  }

  /**
   * Action performed method to register the custom behavior when Ascending sorting is clicked
   */
  override fun setSelected(e: AnActionEvent, state: Boolean) {
    val view = e.getExplorerView<FileExplorerView>() ?: return
    val selectedNode = view.mySelectedNodesData[0].node
    if (selectedNode is UssDirNode) {
      val queryToUpdate = (selectedNode.query as UnitRemoteQueryImpl)
      if (selectedNode.currentSortQueryKeysList.isEmpty()) {
        selectedNode.currentSortQueryKeysList.add(SortQueryKeys.DATE)
        selectedNode.currentSortQueryKeysList.add(SortQueryKeys.ASCENDING)
        queryToUpdate.sortKeys.addAll(selectedNode.currentSortQueryKeysList)
      } else {
        selectedNode.currentSortQueryKeysList.remove(SortQueryKeys.DESCENDING)
        if (!selectedNode.currentSortQueryKeysList.contains(SortQueryKeys.ASCENDING))
          selectedNode.currentSortQueryKeysList.add(SortQueryKeys.ASCENDING)
        queryToUpdate.sortKeys.addAll(selectedNode.currentSortQueryKeysList)
      }
      queryToUpdate.requester = selectedNode
      selectedNode.cleanCache(false)
      fetchProvider.reload(queryToUpdate)
    }
  }

  /**
   * Custom isSelected method determines if the ascending sorting is currently enabled or not. Updates UI by 'tick' mark
   */
  override fun isSelected(e: AnActionEvent): Boolean {
    val view = e.getExplorerView<FileExplorerView>() ?: return false
    val selectedNode = view.mySelectedNodesData[0].node
    if (selectedNode is UssDirNode) {
      return selectedNode.currentSortQueryKeysList.contains(SortQueryKeys.ASCENDING)
    }
    return false
  }

  /**
   * If action is dumb aware or not
   */
  override fun isDumbAware(): Boolean {
    return true
  }
}

/**
 * Custom action for sorting by item Name/Type/Modify Date in descending order (Dir or File name in case of USS)
 */
class SortByDescendingOrderAction : ToggleAction() {

  override fun update(e: AnActionEvent) {
    super.update(e)
    e.presentation.isEnabledAndVisible = true
  }

  /**
   * Action performed method to register the custom behavior when Descending sorting is clicked
   */
  override fun setSelected(e: AnActionEvent, state: Boolean) {
    val view = e.getExplorerView<FileExplorerView>() ?: return
    val selectedNode = view.mySelectedNodesData[0].node
    if (selectedNode is UssDirNode) {
      val queryToUpdate = (selectedNode.query as UnitRemoteQueryImpl)
      if (selectedNode.currentSortQueryKeysList.isEmpty()) {
        selectedNode.currentSortQueryKeysList.add(SortQueryKeys.DATE)
        selectedNode.currentSortQueryKeysList.add(SortQueryKeys.DESCENDING)
        queryToUpdate.sortKeys.addAll(selectedNode.currentSortQueryKeysList)
      } else {
        selectedNode.currentSortQueryKeysList.remove(SortQueryKeys.ASCENDING)
        if (!selectedNode.currentSortQueryKeysList.contains(SortQueryKeys.DESCENDING))
          selectedNode.currentSortQueryKeysList.add(SortQueryKeys.DESCENDING)
        queryToUpdate.sortKeys.addAll(selectedNode.currentSortQueryKeysList)
      }
      queryToUpdate.requester = selectedNode
      selectedNode.cleanCache(false)
      fetchProvider.reload(queryToUpdate)
    }
  }

  /**
   * Custom isSelected method determines if the descending sorting is currently enabled or not. Updates UI by 'tick' mark
   */
  override fun isSelected(e: AnActionEvent): Boolean {
    val view = e.getExplorerView<FileExplorerView>() ?: return false
    val selectedNode = view.mySelectedNodesData[0].node
    if (selectedNode is UssDirNode) {
      return selectedNode.currentSortQueryKeysList.contains(SortQueryKeys.DESCENDING)
    }
    return false
  }

  /**
   * If action is dumb aware or not
   */
  override fun isDumbAware(): Boolean {
    return true
  }
}
