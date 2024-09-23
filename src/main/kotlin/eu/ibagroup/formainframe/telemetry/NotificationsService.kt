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

package eu.ibagroup.formainframe.telemetry

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/** Service for notifications preparation and displaying */
interface NotificationsService {

  companion object {
    @JvmStatic
    fun getService(): NotificationsService = service()
  }

  /**
   * Show an error notification, formed from the provided [Throwable]
   * @param t the throwable from which to get the title and the text for the notification
   * @param project the project to show the notification for (could be null)
   * @param custTitle a custom title to use instead of the [Throwable]'s one
   * @param custDetailsShort a custom short details to use instead of the [Throwable]'s one
   * @param custDetailsLong a custom long details text to use instead of the [Throwable]'s one
   */
  fun notifyError(
    t: Throwable,
    project: Project? = null,
    custTitle: String? = null,
    custDetailsShort: String? = null,
    custDetailsLong: String? = null
  )

}
