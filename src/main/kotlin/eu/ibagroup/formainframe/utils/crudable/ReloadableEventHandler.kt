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

package eu.ibagroup.formainframe.utils.crudable

/**
 * Interface to trigger onEvent handler on each event, providing row class and an element of the event on each of them.
 * Specifies additional onReload event handler to be defined
 */
interface ReloadableEventHandler : EventHandler {

  fun onEvent(rowClass: Class<*>, row: Any)

  fun onReload(rowClass: Class<*>)

  override fun <E : Any> onAdd(rowClass: Class<out E>, added: E) {
    onEvent(rowClass, added)
  }

  override fun <E : Any> onUpdate(rowClass: Class<out E>, oldRow: E, newRow: E) {
    onEvent(rowClass, newRow)
  }

  override fun <E : Any> onDelete(rowClass: Class<out E>, row: E) {
    onEvent(rowClass, row)
  }
}
