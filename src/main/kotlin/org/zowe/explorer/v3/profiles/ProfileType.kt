/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.profiles

// TODO: doc
enum class ProfileType(val typeAsString: String) {
  EXPLORER_IJ("explorer_ij"),
  FILES_IJ("files_ij"),
  JES_IJ("jes_ij"),
  ZOSMF("zosmf")
}
