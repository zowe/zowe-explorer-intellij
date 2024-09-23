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

import java.util.concurrent.locks.Lock

/** Interface that describes the management of locks */
interface LocksManager {

  fun <E : Any> getLockForAdding(rowClass: Class<out E>): Lock?

  fun <E : Any> getLockForGettingAll(rowClass: Class<out E>): Lock?

  fun <E : Any> getLockForUpdating(rowClass: Class<out E>): Lock?

  fun <E : Any> getLockForDeleting(rowClass: Class<out E>): Lock?

  fun <E : Any> getLockForNextUniqueValue(rowClass: Class<out E>): Lock?

}
