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

package org.zowe.explorer.explorer.actions

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.runModalTask
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.FileAttributes
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.attributes.RemoteMemberAttributes
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.explorer.ui.FileExplorerView
import org.zowe.explorer.explorer.ui.getExplorerView
import org.zowe.explorer.vfs.MFVirtualFile
import com.intellij.ide.util.treeView.AbstractTreeNode
import java.util.LinkedHashMap

/**
 * Action that implements the "Compare with..." functionality
 * Allows comparing two mainframe files
 */
class CompareWithAction : AnAction() {

  companion object {
    private const val RECENT_COMPARISONS_KEY = "zowe.explorer.recent.comparisons"
    private const val MAX_RECENT_FILES = 10
    
    // In-memory cache to store recent file pairs used for comparison
    private val recentComparisonCache = LinkedHashMap<String, String>(MAX_RECENT_FILES, 0.75f, true)
  }

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }

  /**
   * Handler for the action when selected from the context menu
   */
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getExplorerView<FileExplorerView>() ?: return
    val selectedNodesData = view.mySelectedNodesData

    if (selectedNodesData.size != 1) {
      Messages.showErrorDialog(
        "Please select exactly one file to compare",
        "Compare Files Error"
      )
      return
    }

    val sourceFile = selectedNodesData[0].file as? MFVirtualFile ?: return
    val sourceAttributes = selectedNodesData[0].attributes ?: return

    // Show file chooser popup with available files for comparison
    val allFiles = collectAvailableFilesForComparison(view, sourceFile, sourceAttributes, e)
    if (allFiles.isEmpty()) {
      Messages.showMessageDialog(
        e.project,
        "No compatible files found for comparison",
        "Compare Files",
        Messages.getInformationIcon()
      )
      return
    }
    
    // Sort files to prioritize recently compared files
    val sourceFilePath = getFilePath(sourceFile)
    val availableFiles = sortFilesByRecentComparisons(allFiles, sourceFilePath)

    val popup = JBPopupFactory.getInstance().createListPopup(
      object : BaseListPopupStep<Pair<VirtualFile, FileAttributes>>(
        "Select File to Compare With",
        availableFiles
      ) {
        override fun getTextFor(value: Pair<VirtualFile, FileAttributes>): String {
          val localDataOpsManager = service<DataOpsManager>()
          val path = when (val attr = value.second) {
            is RemoteDatasetAttributes -> attr.name
            is RemoteMemberAttributes -> {
              val member = attr.name
              // Get the parent dataset name from the parentFile attribute
              val parentAttr = localDataOpsManager.tryToGetAttributes(attr.parentFile) as? RemoteDatasetAttributes
              val dataset = parentAttr?.name ?: ""
              "$dataset($member)"
            }
            is RemoteUssAttributes -> attr.path
            else -> value.first.path
          }
          return path
        }

        override fun onChosen(selectedValue: Pair<VirtualFile, FileAttributes>, finalChoice: Boolean): PopupStep<*>? {
          // Store this comparison in the recent cache
          storeRecentComparison(sourceFile, selectedValue.first)
          
          // Run the comparison
          compareFiles(e, sourceFile, selectedValue.first)
          return null
        }
      }
    )

    popup.showInBestPositionFor(e.dataContext)
  }
  
  /**
   * Get a unique path/identifier for a file
   */
  private fun getFilePath(file: VirtualFile): String {
    val dataOpsManager = service<DataOpsManager>()
    val attr = dataOpsManager.tryToGetAttributes(file)
    
    return when (attr) {
      is RemoteDatasetAttributes -> attr.name
      is RemoteMemberAttributes -> {
        val member = attr.name
        // Get the parent dataset name from the parentFile attribute
        val parentAttr = dataOpsManager.tryToGetAttributes(attr.parentFile) as? RemoteDatasetAttributes
        val dataset = parentAttr?.name ?: ""
        "$dataset($member)"
      }
      is RemoteUssAttributes -> attr.path
      else -> file.path
    }
  }
  
  /**
   * Store a recent comparison in both memory and persistent cache
   */
  private fun storeRecentComparison(file1: VirtualFile, file2: VirtualFile) {
    val path1 = getFilePath(file1)
    val path2 = getFilePath(file2)
    
    // Update in-memory cache
    if (recentComparisonCache.size >= MAX_RECENT_FILES) {
      // Remove oldest entry if at capacity
      recentComparisonCache.entries.firstOrNull()?.let {
        recentComparisonCache.remove(it.key)
      }
    }
    recentComparisonCache[path1] = path2
    
    // Also update persistent storage for the next session
    val propertiesComponent = PropertiesComponent.getInstance()
    val existingValue = propertiesComponent.getValue(RECENT_COMPARISONS_KEY, "")
    
    val comparisons = if (existingValue.isNotEmpty()) {
      existingValue.split(";").toMutableList()
    } else {
      mutableListOf()
    }
    
    // Add current comparison
    val comparisonStr = "$path1:$path2"
    comparisons.remove(comparisonStr) // Remove if exists to avoid duplicates
    comparisons.add(0, comparisonStr) // Add to the beginning
    
    // Trim the list if needed
    while (comparisons.size > MAX_RECENT_FILES) {
      comparisons.removeAt(comparisons.size - 1)
    }
    
    // Save back to properties
    propertiesComponent.setValue(RECENT_COMPARISONS_KEY, comparisons.joinToString(";"))
  }
  
  /**
   * Sort files based on recent comparison history
   */
  private fun sortFilesByRecentComparisons(
    files: List<Pair<VirtualFile, FileAttributes>>, 
    sourcePath: String
  ): List<Pair<VirtualFile, FileAttributes>> {
    // Load cache if empty
    if (recentComparisonCache.isEmpty()) {
      loadRecentComparisonsCache()
    }
    
    return files.sortedWith(Comparator { a, b ->
      val pathA = getFilePath(a.first)
      val pathB = getFilePath(b.first)
      
      // Check if either file was recently compared with the source
      val isARecent = recentComparisonCache[sourcePath] == pathA
      val isBRecent = recentComparisonCache[sourcePath] == pathB
      
      when {
        isARecent && !isBRecent -> -1
        !isARecent && isBRecent -> 1
        else -> 0 // If both or neither are recent, don't change order
      }
    })
  }
  
  /**
   * Load recent comparisons from persistent storage
   */
  private fun loadRecentComparisonsCache() {
    val propertiesComponent = PropertiesComponent.getInstance()
    val storedComparisons = propertiesComponent.getValue(RECENT_COMPARISONS_KEY, "")
    
    if (storedComparisons.isNotEmpty()) {
      storedComparisons.split(";").forEach { comparison ->
        val parts = comparison.split(":")
        if (parts.size == 2) {
          recentComparisonCache[parts[0]] = parts[1]
        }
      }
    }
  }

  /**
   * Collect all files from the explorer that are eligible for comparison
   */
  private fun collectAvailableFilesForComparison(
    view: FileExplorerView,
    sourceFile: VirtualFile,
    sourceAttributes: FileAttributes,
    e: AnActionEvent
  ): List<Pair<VirtualFile, FileAttributes>> {
    val dataOpsManager = service<DataOpsManager>()
    val result = mutableListOf<Pair<VirtualFile, FileAttributes>>()
    val allFiles = mutableListOf<VirtualFile>()
    
    // Simplified approach - collect files from all selected nodes that have files
    view.mySelectedNodesData.forEach { nodeData ->
      nodeData.file?.let { file ->
        // Add this file to our list if it's not the source file
        if (file != sourceFile && !file.isDirectory && file is MFVirtualFile) {
          allFiles.add(file)
        }
      }
    }
    
    // Also try to get currently open files
    try {
      val fileEditorManager = FileEditorManager.getInstance(e.project ?: return emptyList())
      val openFiles = fileEditorManager.openFiles
      allFiles.addAll(openFiles.filterIsInstance<MFVirtualFile>().filter { it != sourceFile && !it.isDirectory })
    } catch (ex: Exception) {
      // Ignore errors trying to get open files
    }
    
    // Add files from explorer units that have been accessed
    try {
      view.explorer.units.forEach { unit ->
        // We can't directly access files here, so look for matches in what we've found
        // This is here for future expansion
      }
    } catch (ex: Exception) {
      // Ignore errors trying to traverse units
    }
    
    // Process all found files
    allFiles.forEach { file ->
      if (file != sourceFile && !file.isDirectory) {
        val attributes = dataOpsManager.tryToGetAttributes(file)
        if (attributes != null && areFilesCompatibleForComparison(sourceAttributes, attributes)) {
          result.add(Pair(file, attributes))
        }
      }
    }
    
    return result
  }

  /**
   * Check if two files can be compared
   */
  private fun areFilesCompatibleForComparison(attr1: FileAttributes, attr2: FileAttributes): Boolean {
    return when {
      attr1 is RemoteDatasetAttributes && attr2 is RemoteDatasetAttributes -> true
      attr1 is RemoteMemberAttributes && attr2 is RemoteMemberAttributes -> true
      attr1 is RemoteUssAttributes && attr2 is RemoteUssAttributes -> true
      else -> false
    }
  }

  /**
   * Compare two files using IntelliJ's diff tool
   */
  private fun compareFiles(e: AnActionEvent, file1: VirtualFile, file2: VirtualFile) {
    runModalTask(
      title = "Comparing Files",
      project = e.project,
      cancellable = true
    ) { progressIndicator ->
      // Get file names for display
      val fileName1 = getDisplayName(file1)
      val fileName2 = getDisplayName(file2)
      
      val contentFactory = DiffContentFactory.getInstance()
      val content1 = contentFactory.create(e.project, file1)
      val content2 = contentFactory.create(e.project, file2)

      val diffRequest = SimpleDiffRequest(
        "Comparing ${fileName1} with ${fileName2}",
        content1,
        content2,
        fileName1,
        fileName2
      )

      DiffManager.getInstance().showDiff(e.project, diffRequest)
    }
  }
  
  /**
   * Get a user-friendly display name for a file
   */
  private fun getDisplayName(file: VirtualFile): String {
    val dataOpsManager = service<DataOpsManager>()
    val attr = dataOpsManager.tryToGetAttributes(file)
    
    return when (attr) {
      is RemoteDatasetAttributes -> attr.name
      is RemoteMemberAttributes -> {
        val member = attr.name
        // Get the parent dataset name from the parentFile attribute
        val parentAttr = dataOpsManager.tryToGetAttributes(attr.parentFile) as? RemoteDatasetAttributes
        val dataset = parentAttr?.name ?: ""
        "$dataset($member)"
      }
      is RemoteUssAttributes -> attr.path
      else -> file.name
    }
  }

  /**
   * Control when the action is enabled in the UI
   */
  override fun update(e: AnActionEvent) {
    val view = e.getExplorerView<FileExplorerView>()
    val selectedNodesData = view?.mySelectedNodesData

    e.presentation.isEnabledAndVisible = selectedNodesData?.size == 1 &&
        selectedNodesData[0].file?.isDirectory == false &&
        (selectedNodesData[0].attributes is RemoteDatasetAttributes ||
            selectedNodesData[0].attributes is RemoteMemberAttributes ||
            selectedNodesData[0].attributes is RemoteUssAttributes)
  }
} 