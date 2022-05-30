/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops.log

import org.zowe.explorer.config.connect.ConnectionConfig

/**
 * Base interface for implementing mainframe process unique information like id or name.
 * @author Valentine Krus
 */
interface MFProcessInfo {
  val connectionConfig: ConnectionConfig
}
