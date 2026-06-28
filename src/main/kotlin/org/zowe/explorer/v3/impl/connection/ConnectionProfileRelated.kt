/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.connection

/**
 * Interface for classes related to a connection profile from the Zowe Team Config.
 * [connectionProfile] is the profile path (e.g. `lpar1.zosmf`) used to resolve
 * connection properties and credentials via the profile hierarchy
 */
interface ConnectionProfileRelated {
  val connectionProfile: String
}
