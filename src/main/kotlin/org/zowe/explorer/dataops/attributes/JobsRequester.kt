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

package org.zowe.explorer.dataops.attributes

import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.JobsFilter

/**
 * Information object with jobs filter and connection configuration inside to send request for a list of jobs to zosmf.
 * @param connectionConfig connection configuration to specify the system to work with.
 * @param jobsFilter filter of the jobs (prefix, owner, jobId).
 * @author Valiantsin Krus
 */
class JobsRequester(
  override val connectionConfig: ConnectionConfig,
  val jobsFilter: JobsFilter
) : Requester<ConnectionConfig>
