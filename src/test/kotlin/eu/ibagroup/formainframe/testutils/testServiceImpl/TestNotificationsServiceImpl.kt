/*
 * Copyright (c) 2024 IBA Group.
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

package eu.ibagroup.formainframe.testutils.testServiceImpl

import com.intellij.openapi.project.Project
import eu.ibagroup.formainframe.telemetry.NotificationsService

open class TestNotificationsServiceImpl : NotificationsService {

  var testInstance = object : NotificationsService {

    override fun notifyError(
      t: Throwable,
      project: Project?,
      custTitle: String?,
      custDetailsShort: String?,
      custDetailsLong: String?
    ) {
      TODO("Not yet implemented")
    }

  }

  override fun notifyError(
    t: Throwable,
    project: Project?,
    custTitle: String?,
    custDetailsShort: String?,
    custDetailsLong: String?
  ) {
    return this.testInstance.notifyError(t, project, custTitle, custDetailsShort, custDetailsLong)
  }

}