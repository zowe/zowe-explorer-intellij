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
 * Mask state extension that stores working set reference to check the mask uniqueness basing
 * on the working set existing masks.
 * An instance of this class could also be called with the [MaskState] instance provided,
 * that will create a new instance of the mask state with working set
 * @param mask the mask name
 * @param type the mask type (z/OS or USS)
 * @param isTypeSelectedAutomatically parameter to represent whether the type selected basing on the mask name input
 * @param isTypeSelectedManually parameter to represent whether the type selected by the user through the combo box
 * @param ws the working set to check the mask uniqueness
 */
class MaskStateWithWS(
  mask: String = "",
  type: MaskType = MaskType.ZOS,
  isTypeSelectedAutomatically: Boolean = false,
  isTypeSelectedManually: Boolean = false,
  var ws: FilesWorkingSet
) : MaskState(mask, type, isTypeSelectedAutomatically, isTypeSelectedManually) {
  constructor(maskState: MaskState, ws: FilesWorkingSet) :
      this(
        mask = maskState.mask,
        type = maskState.type,
        isTypeSelectedAutomatically = maskState.isTypeSelectedAutomatically,
        isTypeSelectedManually = maskState.isTypeSelectedManually,
        ws
      )
}
