/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.tree.nodes

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// TODO: doc
fun getCurrentRefreshDateTime(): String {
  return DateTimeFormatter
    .ofPattern("dd MMM YYYY HH:mm:ss", Locale.ENGLISH)
    .withZone(ZoneId.systemDefault())
    .format(LocalDateTime.now())
    .uppercase(Locale.getDefault())
}
