/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.explorer

import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.config.ws.UssPath

interface WorkingSet : ExplorerUnit {

  val dsMasks: Collection<DSMask>

  fun addMask(dsMask: DSMask)

  fun removeMask(dsMask: DSMask)

  val ussPaths: Collection<UssPath>

  fun addUssPath(ussPath: UssPath)

  fun removeUssPath(ussPath: UssPath)

}