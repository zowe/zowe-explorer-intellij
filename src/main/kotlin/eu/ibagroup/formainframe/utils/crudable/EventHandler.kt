/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.utils.crudable

interface EventHandler {
  fun <E : Any> onAdd(rowClass: Class<out E>, added: E)
  fun <E : Any> onUpdate(rowClass: Class<out E>, oldRow: E, newRow: E)
  fun <E : Any> onDelete(rowClass: Class<out E>, row: E)
}