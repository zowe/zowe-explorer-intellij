/*
 * Copyright (c) 2020 IBA Group.
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

package org.zowe.explorer.dataops.attributes;

import org.zowe.explorer.config.connect.ConnectionConfigBase

/** Interface that is necessary to implement requests to z/OSMF for specific entity (USS files, datasets, jobs etc.) */
interface Requester<Connection : ConnectionConfigBase> {
  val connectionConfig: Connection
}
