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

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import eu.ibagroup.formainframe.common.message
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.explorer.EXPLORER_NOTIFICATION_GROUP_ID

const val UNKNOWN_ERROR = "Unknown error"

/** @see NotificationsService */
class NotificationsServiceImpl : NotificationsService {

  /**
   * Get a title string for the [CallException]
   * @param t the [CallException] to get the title basing on
   * @return the title string
   */
  private fun getTitleForCallException(t: CallException): String {
    val rawTitle = (
      t.errorParams
        ?.getOrDefault("message", t.headMessage)
        ?: t.headMessage
      ) as String
    return rawTitle
      .replaceFirstChar { it.uppercase() }
      .split(".")
      .first()
  }

  /**
   * Form three error notification parameters from the provided exception.
   * The title will be either the [custTitle] or the "throwable.message" cut to the 30 characters long one + "...".
   * If none provided, the "Handleable exception occurred" title is used.
   * The short details will be either the [custDetailsShort] or the "throwable.message" if it is not used in the title,
   * or the "throwable.cause.message" cut to the 100 characters long one + "...". If none of the options are available,
   * the [UNKNOWN_ERROR] is used.
   * The long details will be either the [custDetailsLong] or the "throwable.message" + "throwable.cause.message"
   * divided by two new lines. If none of the options are provided, the [UNKNOWN_ERROR] is used
   * @param throwable the throwable to form the parameters from
   * @param custTitle the custom title to use if provided
   * @param custDetailsShort the custom details short to use if provided
   * @param custDetailsLong the custom details long to use if provided
   * @return the [Triple] of the final title, short and long details message
   */
  private fun formNotificationCompatibleTripleFromException(
    throwable: Throwable,
    custTitle: String?,
    custDetailsShort: String?,
    custDetailsLong: String?
  ): Triple<String, String, String> {
    val titleFromMessage =
      if ((throwable.message?.length ?: 0) > 30) throwable.message?.substring(0, 30) + "..." else throwable.message
    val title = custTitle
      ?: titleFromMessage
      ?: "Handleable exception occurred"

    val throwableMessageToUseInDetailsShort = if (custTitle != null) throwable.message else null
    val causeMessage = throwable.cause?.message
    val causeMessageShort =
      if ((causeMessage?.length ?: 0) > 100) causeMessage?.substring(0, 100) + "..." else causeMessage
    val detailsShort = custDetailsShort
      ?: throwableMessageToUseInDetailsShort
      ?: causeMessageShort
      ?: UNKNOWN_ERROR

    val composedExceptionMessage =
      if (throwable.message != null) "${throwable.message}\n\n$causeMessage" else causeMessage
    val detailsLong = custDetailsLong ?: composedExceptionMessage ?: UNKNOWN_ERROR

    return Triple(title, detailsShort, detailsLong)
  }

  /**
   * Transform the [Throwable] instance to the [NotificationCompatibleException]
   * @param t the [Throwable] to transform
   * @param custTitle a custom title to use instead of the [Throwable]'s one
   * @param custDetailsShort a custom short details to use instead of the [Throwable]'s one
   * @param custDetailsLong a custom long details text to use instead of the [Throwable]'s one
   * @return the [NotificationCompatibleException] transformed instance
   */
  private fun makeThrowableNotificationCompatible(
    t: Throwable,
    custTitle: String? = null,
    custDetailsShort: String? = null,
    custDetailsLong: String? = null
  ): NotificationCompatibleException {
    val (title, detailsShort, detailsLong) = when {

      t is RuntimeException && (t is ProcessCanceledException || t.cause is ProcessCanceledException) ->
        Triple(
          custTitle ?: "Error in plugin \"For-Mainframe\"",
          custDetailsShort ?: message("explorer.cancel.by.user.error"),
          custDetailsLong ?: UNKNOWN_ERROR
        )

      t is CallException -> Triple(
        custTitle ?: getTitleForCallException(t),
        custDetailsShort ?: t.message?.split("\n")?.first() ?: UNKNOWN_ERROR,
        custDetailsLong ?: t.message ?: UNKNOWN_ERROR
      )

      t !is NotificationCompatibleException ->
        formNotificationCompatibleTripleFromException(t, custTitle, custDetailsShort, custDetailsLong)

      else -> Triple(t.title, t.detailsShort, t.detailsLong)

    }
    return NotificationCompatibleException(title, detailsShort, detailsLong)
  }

  override fun notifyError(
    t: Throwable,
    project: Project?,
    custTitle: String?,
    custDetailsShort: String?,
    custDetailsLong: String?
  ) {
    val notificationCompatibleException =
      makeThrowableNotificationCompatible(t, custTitle, custDetailsShort, custDetailsLong)

    val errorNotification = Notification(
      EXPLORER_NOTIFICATION_GROUP_ID,
      notificationCompatibleException.title,
      notificationCompatibleException.detailsShort,
      NotificationType.ERROR
    )

    if (notificationCompatibleException.detailsLong != UNKNOWN_ERROR) {
      errorNotification.addAction(object : NotificationAction("More") {
        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
          Messages.showErrorDialog(
            project,
            notificationCompatibleException.detailsLong,
            notificationCompatibleException.title
          )
        }
      })
    }

    Notifications.Bus.notify(errorNotification)
  }

}
