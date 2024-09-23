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

/**
 * Interface which represents query that should be executed.
 * @param Request data that is necessary to proceed query.
 * @param Result result that should be returned after query execution.
 */
interface Query<Request, Result> : Operation<Result> {

  val request: Request

}
