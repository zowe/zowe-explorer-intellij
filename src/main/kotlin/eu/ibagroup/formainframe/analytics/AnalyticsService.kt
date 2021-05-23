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