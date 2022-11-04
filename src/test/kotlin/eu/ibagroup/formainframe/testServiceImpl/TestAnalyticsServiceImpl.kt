package eu.ibagroup.formainframe.testServiceImpl

import eu.ibagroup.formainframe.analytics.AnalyticsService
import eu.ibagroup.formainframe.analytics.events.AnalyticsEvent

class TestAnalyticsServiceImpl : AnalyticsService {

  /**
   * Test instance for the AnalyticsService.
   * Defines default behaviour of the service at initialization stage.
   * All the test class methods use this implementation, so it makes this easier to redefine in a test case
   */
  var testInstance: AnalyticsService = object : AnalyticsService {
    override var isAnalyticsEnabled = false
    override var isUserAcknowledged = true

    override fun trackAnalyticsEvent(eventName: String, eventProperties: Map<String, String>) {
      TODO("Not yet implemented")
    }

    override fun trackAnalyticsEvent(event: AnalyticsEvent) {
      TODO("Not yet implemented")
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
