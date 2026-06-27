/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.files.dialogs

data class DsMaskEntry(val mask: String)

data class UssPathEntry(val path: String)

class CreateFilesProfileDialogState {
  var profileName: String = ""
  var connectionProfile: String = ""
  var dsMasks: MutableList<DsMaskEntry> = mutableListOf()
  var ussPaths: MutableList<UssPathEntry> = mutableListOf()
}
