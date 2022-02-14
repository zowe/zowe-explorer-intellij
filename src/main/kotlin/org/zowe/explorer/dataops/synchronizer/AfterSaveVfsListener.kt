/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops.synchronizer

import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.messages.Topic
import org.zowe.explorer.utils.sendTopic

fun interface AfterSaveVfsListenerCallback : (List<VFileContentChangeEvent>) -> Unit

@JvmField
val FILES_SAVED = Topic.create("filesSaved", AfterSaveVfsListenerCallback::class.java)

class AfterSaveVfsListener : AsyncFileListener {

  override fun prepareChange(events: MutableList<out VFileEvent>): AsyncFileListener.ChangeApplier {
    return object : AsyncFileListener.ChangeApplier {
      override fun afterVfsChange() {
        events.filterIsInstance<VFileContentChangeEvent>()
          .also {
            if (it.isNotEmpty()) {
              sendTopic(FILES_SAVED)(it)
            }
          }
      }
    }
  }
}
