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

package eu.ibagroup.formainframe.dataops

import eu.ibagroup.formainframe.config.connect.ConnectionConfigBase

/**
 * Interface which represents query that should be executed on remote system,
 * consists from query and connection config to system (such as zosmf, cics, etc.).
 * @param Connection The system (such as zosmf, cics etc.) connection class to work with (see [ConnectionConfigBase]).
 * @param Request data that is necessary to proceed query.
 * @param Result result that should be returned after query execution.
 */
interface RemoteQuery<Connection: ConnectionConfigBase, Request, Result>
  : Query<Request, Result>, RemoteInfo<Connection>
