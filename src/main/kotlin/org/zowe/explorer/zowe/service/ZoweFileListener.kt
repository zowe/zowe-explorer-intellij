/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.zowe.service

import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import org.zowe.explorer.utils.runIfTrue
import org.zowe.explorer.utils.service
import org.zowe.explorer.zowe.showNotificationForAddUpdateZoweConfigIfNeeded

/**
 * ZoweFileListener is needed for listening of vfs changes topic and
 * notifying ui if zowe config crudable configs are not synchronized.
 * @author Valiantsin Krus
 * @version 0.5
 * @since 2021-02-12
 */
class ZoweFileListener: BulkFileListener {

  /**
   * Updates zowe config by file events.
   * @param events - events that was triggered.
   * @param isBefore - true if event triggered before changes action and false otherwise.
   * @return Nothing.
   */
  private fun updateZoweConfig(events: MutableList<out VFileEvent>, isBefore: Boolean) {
    events.forEach { e ->
      val file = e.file ?: return
      runIfTrue(file.name == "zowe.config.json") {
        val projectForFile = ProjectLocator.getInstance().guessProjectForFile(file) ?: return
        if (e is VFileDeleteEvent) {
          projectForFile.service<ZoweConfigService>().zoweConfig = null
        } else if(!isBefore) {
          showNotificationForAddUpdateZoweConfigIfNeeded(projectForFile)
        }
      }
    }
  }

  override fun before(events: MutableList<out VFileEvent>) {
    updateZoweConfig(events, true)
  }
  override fun after(events: MutableList<out VFileEvent>) {
    updateZoweConfig(events, false)
  }
}
