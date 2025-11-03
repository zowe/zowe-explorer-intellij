/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   Dzianis Lisiankou
 */

package org.zowe.explorer.v3.retrofit

fun extractCookieValue(cookieHeader: String, cookieName: String): String? {
  return cookieHeader
    .split(";")
    .map { it.trim() }
    .find { it.startsWith("$cookieName=") }
    ?.substringAfter("=")
}