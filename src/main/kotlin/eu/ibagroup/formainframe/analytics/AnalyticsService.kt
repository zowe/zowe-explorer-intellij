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

package eu.ibagroup.formainframe.analytics

import com.intellij.openapi.components.service
import eu.ibagroup.formainframe.analytics.events.AnalyticsEvent

/**
 * Service for collecting analytics
 * @author Valiantsin Krus
 */
interface AnalyticsService {

  companion object {
    @JvmStatic
    fun getService(): AnalyticsService = service()
  }

  /** Defines if user has allowed to use analytics. */
  var isAnalyticsEnabled: Boolean

  /** Defines if the user has been notified with analytics policy dialog at least once. */
  var isUserAcknowledged: Boolean

  /**
   * Tracks analytics event by event name and event properties
   * @param eventName name of the event to track
   * @param eventProperties map of the properties specified for the specific action.
   */
  fun trackAnalyticsEvent(eventName: String, eventProperties: Map<String, String> = emptyMap())

  /**
   * Tracks analytics event by AnalyticsEvent instance.
   * @param event instance of AnalyticsEvent implementation class.
   * @see AnalyticsEvent
   */
  fun trackAnalyticsEvent(event: AnalyticsEvent)

}
