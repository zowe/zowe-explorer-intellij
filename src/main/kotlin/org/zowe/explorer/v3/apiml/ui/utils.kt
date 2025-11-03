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

package org.zowe.explorer.v3.apiml.ui

fun isOnlyConnectionNameChanged(
  initialState: ApiMlConnectionDialogState,
  state: ApiMlConnectionDialogState
): Boolean {
  return initialState.name != state.name &&
    initialState.scheme == state.scheme &&
    initialState.host == state.host &&
    initialState.port == state.port &&
    initialState.basePath == state.basePath &&
    initialState.username == state.username &&
    initialState.password.contentEquals(state.password) &&
    initialState.rejectUnauthorized == state.rejectUnauthorized &&
    initialState.gatewayPath == state.gatewayPath
}