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

package org.zowe.explorer.config

import org.zowe.explorer.utils.crudable.EventHandler

/**
 * Class that implements possible event handlers
 */
class ConfigEventHandler : EventHandler {

  override fun <E : Any> onAdd(rowClass: Class<out E>, added: E) {
    sendConfigServiceTopic().onAdd(rowClass, added)
  }

  override fun <E : Any> onUpdate(rowClass: Class<out E>, oldRow: E, newRow: E) {
    sendConfigServiceTopic().onUpdate(rowClass, oldRow, newRow)
  }

  override fun <E : Any> onDelete(rowClass: Class<out E>, row: E) {
    sendConfigServiceTopic().onDelete(rowClass, row)
  }

}
