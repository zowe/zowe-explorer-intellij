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

/**
 * Represents exception that is compatible to the plugin's notifications
 * @property title the title of the exception to show in notification
 * @property detailsShort a short detailed information about the exception occurred
 * @property detailsLong an extended detailed information about the exception occurred
 */
class NotificationCompatibleException(
  val title: String = UNKNOWN_ERROR,
  val detailsShort: String = "No details",
  val detailsLong: String = UNKNOWN_ERROR
) : Exception(detailsShort + '\n' + detailsLong)
