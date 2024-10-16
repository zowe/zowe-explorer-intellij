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

package org.zowe.explorer.zowe.service

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import org.zowe.explorer.utils.runIfTrue
import org.zowe.explorer.zowe.ZOWE_CONFIG_NAME
import org.zowe.explorer.zowe.showDialogForDeleteZoweConfigIfNeeded
import org.zowe.explorer.zowe.showNotificationForAddUpdateZoweConfigIfNeeded

/**
 * ZoweFileListener is needed for listening of vfs changes topic and
 * notifying ui if zowe config crudable configs are not synchronized.
 * @author Valiantsin Krus
 * @version 0.5
 * @since 2021-02-12
 */
class ZoweFileListener : BulkFileListener {

  /**
   * Updates zowe config by file events.
   * @param events - events that was triggered.
   * @return Nothing.
   */
  private fun updateZoweConfig(events: MutableList<out VFileEvent>) {
    events.forEach { e ->
      val file = e.file ?: return
      runIfTrue(file.name == ZOWE_CONFIG_NAME) {
        val projectForFile = ProjectLocator.getInstance().guessProjectForFile(file) ?: return
        val type =
          if (file.canonicalPath == ZoweConfigServiceImpl.getZoweConfigLocation(projectForFile, ZoweConfigType.LOCAL))
            ZoweConfigType.LOCAL
          else if (file.canonicalPath == ZoweConfigServiceImpl.getZoweConfigLocation(
              projectForFile,
              ZoweConfigType.GLOBAL
            )
          )
            ZoweConfigType.GLOBAL
          else return
        if (e is VFileDeleteEvent) {
          invokeLater {
            showDialogForDeleteZoweConfigIfNeeded(projectForFile, type = type)
          }
        } else {
          showNotificationForAddUpdateZoweConfigIfNeeded(projectForFile, type = type)
        }
      }
    }
  }

  override fun after(events: MutableList<out VFileEvent>) {
    updateZoweConfig(events)
  }
}
