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

import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.explorer.EXPLORER_NOTIFICATION_GROUP_ID

/** "Rate us" notification functionality */
class RateUsNotification {

 companion object {

   const val PLUGIN_REVIEW_LINK = "https://plugins.jetbrains.com/plugin/16353-for-mainframe/reviews"

   /** Show the "Rate us" notification */
   fun showRateUsNotification() {
     val configService = ConfigService.getService()
     @Suppress("DialogTitleCapitalization")
     Notification(
       EXPLORER_NOTIFICATION_GROUP_ID,
       "For Mainframe: Rate us",
       "Like our plug-in so far? Leave a review!",
       NotificationType.INFORMATION
     )
       .addAction(NotificationAction.createSimpleExpiring("Rate") {
         configService.rateUsNotificationDelay = -1
         BrowserUtil.browse(PLUGIN_REVIEW_LINK)
       })
       .addAction(NotificationAction.createSimpleExpiring("Later") {})
       .addAction(NotificationAction.createSimpleExpiring("Dismiss") {
         configService.rateUsNotificationDelay = -1
       })
       .let {
         Notifications.Bus.notify(it)
       }
   }

 }

}
