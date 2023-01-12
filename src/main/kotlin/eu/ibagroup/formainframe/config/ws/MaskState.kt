/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.config.ws

import eu.ibagroup.formainframe.explorer.FilesWorkingSet
import eu.ibagroup.formainframe.utils.MaskType

/**
 * Class to represent a mask state
 * @param mask the mask name
 * @see MaskType
 * @param type the mask type (z/OS or USS)
 * @param isTypeSelectedAutomatically parameter to represent whether the type selected basing on the mask name input
 * @param isTypeSelectedManually parameter to represent whether the type selected by the user through the combo box
 */
open class MaskState(
  var mask: String = "",
  var type: MaskType = MaskType.ZOS,
  var isTypeSelectedAutomatically: Boolean = false,
  var isTypeSelectedManually: Boolean = false,
) : TableRow

/**
 * Mask state extension that stores working set reference to check the mask uniqueness basing on the working set existing masks
 * @param ws the working set to check the mask uniqueness
 */
class MaskStateWithWS(var ws: FilesWorkingSet) : MaskState()
