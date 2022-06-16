/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.analytics

import com.intellij.openapi.application.ApplicationManager
import eu.ibagroup.formainframe.analytics.events.AnalyticsEvent


interface AnalyticsService {

  companion object {
    @JvmStatic
    val instance: AnalyticsService
      get() = ApplicationManager.getApplication().getService(AnalyticsService::class.java)
  }


  var isAnalyticsEnabled: Boolean
  var isUserAcknowledged: Boolean

  fun trackAnalyticsEvent(eventName: String, eventProperties: Map<String, String> = emptyMap())
  fun trackAnalyticsEvent(event: AnalyticsEvent)


}
