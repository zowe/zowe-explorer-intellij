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

package eu.ibagroup.formainframe.rateus

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import eu.ibagroup.formainframe.config.ConfigService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** "Rate us" notification displaying handler */
class RateUsStartupActivity : ProjectActivity {

  /** Show Rate Us notification if user did not rate yet / did not disable the notification yet */
  override suspend fun execute(project: Project) {
    val configService = ConfigService.getService()
    if (configService.rateUsNotificationDelay != -1L) {
      delay(configService.rateUsNotificationDelay)
      if (configService.rateUsNotificationDelay != -1L && !project.isDisposed) {
        withContext(Dispatchers.Main) {
          if (!project.isDisposed) {
            RateUsNotification.showRateUsNotification()
          }
        }
      }
    }
  }

}
