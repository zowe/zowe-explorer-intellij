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

package eu.ibagroup.formainframe.dataops.attributes

import eu.ibagroup.formainframe.utils.Copyable
import eu.ibagroup.formainframe.utils.PasteAcceptor
import org.zowe.kotlinsdk.XIBMDataType

/** Interface to describe possible file attributes and interactions with them */
interface FileAttributes : Cloneable, Copyable, PasteAcceptor {

  val name: String

  val length: Long

  var contentMode: XIBMDataType

  public override fun clone(): FileAttributes

  override val isCopyPossible: Boolean
    get() = true

  override val isPastePossible: Boolean
    get() = false

}
