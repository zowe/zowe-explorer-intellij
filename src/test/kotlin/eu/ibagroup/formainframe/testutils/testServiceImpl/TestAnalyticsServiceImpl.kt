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

package eu.ibagroup.formainframe.testutils.testServiceImpl

import eu.ibagroup.formainframe.analytics.AnalyticsService
import eu.ibagroup.formainframe.analytics.events.AnalyticsEvent

open class TestAnalyticsServiceImpl : AnalyticsService {

  /**
   * Test instance for the AnalyticsService.
   * Defines default behaviour of the service at initialization stage.
   * All the test class methods use this implementation, so it makes this easier to redefine in a test case
   */
  var testInstance: AnalyticsService = object : AnalyticsService {
    override var isAnalyticsEnabled = false
    override var isUserAcknowledged = true

    override fun trackAnalyticsEvent(eventName: String, eventProperties: Map<String, String>) {
      return
    }

    override fun trackAnalyticsEvent(event: AnalyticsEvent) {
      return
    }
  }

  override var isAnalyticsEnabled = this.testInstance.isAnalyticsEnabled
  override var isUserAcknowledged = this.testInstance.isUserAcknowledged

  override fun trackAnalyticsEvent(eventName: String, eventProperties: Map<String, String>) {
    return this.testInstance.trackAnalyticsEvent(eventName, eventProperties)
  }

  override fun trackAnalyticsEvent(event: AnalyticsEvent) {
    return this.testInstance.trackAnalyticsEvent(event)
  }
}
