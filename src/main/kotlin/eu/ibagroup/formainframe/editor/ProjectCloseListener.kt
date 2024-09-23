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

package eu.ibagroup.formainframe.editor

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.project.VetoableProjectManagerListener
import com.intellij.openapi.vfs.encoding.EncodingProjectManager
import com.intellij.openapi.vfs.encoding.EncodingProjectManagerImpl
import eu.ibagroup.formainframe.vfs.MFVirtualFile

//private val log = log<FileEditorEventsListener>()

// TODO: implement as soon as the syncronizer will be rewritten
/**
 * Project close event listener.
 * Handle files which are not synchronized before the close
 */
class ProjectCloseListener : ProjectManagerListener {
  init {
    val projListener = object : VetoableProjectManagerListener {
      /**
       * Check whether all the files of the project are synchronized
       * @param project the project to check the files
       */
      override fun canClose(project: Project): Boolean {
//        val configService = ConfigService.getService()
//        if (!configService.isAutoSyncEnabled.get() && ApplicationManager.getApplication().isActive) {
//          val openFiles = project.component<FileEditorManager>().openFiles
//          if (openFiles.isNotEmpty()) {
//            openFiles.forEach { file ->
//              val document = FileDocumentManager.getInstance().getDocument(file) ?: let {
//                log.info("Document cannot be used here")
//                return@forEach
//              }
//              if (showSyncOnCloseDialog(file.name, project)) {
//                runModalTask(
//                  title = "Syncing ${file.name}",
//                  project = project,
//                  cancellable = true
//                ) {
//                  runInEdt {
//                    FileDocumentManager.getInstance().saveDocument(document)
//                    DataOpsManager.getService().getContentSynchronizer(file)?.userSync(file)
//                  }
//                }
//              }
//            }
//          }
//        }
        return true
      }
    }
    ProjectManager.getInstance().addProjectManagerListener(projListener)
  }

  /**
   * Filters encoding mappings that need to be written to the config (encodings.xml).
   * MFVirtualFile encodings are filtered out.
   * @param project the project to filter encoding mappings.
   */
  override fun projectClosingBeforeSave(project: Project) {
    runWriteAction {
      val encodingManager = EncodingProjectManager.getInstance(project) as EncodingProjectManagerImpl
      val filteredMappings = encodingManager.allMappings.toMutableMap().filter { it.key !is MFVirtualFile }
      encodingManager.setMapping(filteredMappings)
    }
    super.projectClosingBeforeSave(project)
  }
}
