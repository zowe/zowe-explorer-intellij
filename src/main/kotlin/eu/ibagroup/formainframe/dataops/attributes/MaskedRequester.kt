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

import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.utils.nullIfBlank

/**
 * Information object with query mask and connection configuration inside
 * to send request for a list of datasets to zosmf.
 * @param connectionConfig connection configuration to specify the system to work with.
 * @param queryMask datasets mask.
 */
data class MaskedRequester(
  override val connectionConfig: ConnectionConfig,
  val queryMask: DSMask,
) : Requester<ConnectionConfig> {
  val queryVolser: String
    get() = queryMask.volser
}
