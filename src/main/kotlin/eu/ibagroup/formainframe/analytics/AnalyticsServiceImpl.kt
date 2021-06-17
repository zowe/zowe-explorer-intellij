package eu.ibagroup.formainframe.analytics

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil
import com.segment.analytics.Analytics
import com.segment.analytics.messages.TrackMessage
import eu.ibagroup.formainframe.analytics.events.AnalyticsEvent
import java.util.*

internal val analyticsProperties by lazy {
  Properties().apply {
    load(AnalyticsService::class.java.classLoader.getResourceAsStream("analytics.properties"))
  }
}

@State(
  name = "eu.ibagroup.formainframe.analytics.AnalyticsService",
  storages = [Storage(value = "for-mainframe-analytics.xml", exportable = true)]
)
class AnalyticsServiceImpl : AnalyticsService, PersistentStateComponent<AnalyticsState> {

  companion object {
    val myState = AnalyticsState()
  }

  private val credentialAttributes = CredentialAttributes("formainframeAnalyticsCredentials")

  val analytics: Analytics by lazy {
    createAnalytics()
  }


  override fun loadState(state: AnalyticsState) {
    XmlSerializerUtil.copyBean(state, myState)
  }

  private fun createAnalyticsUuid(): String {
    val analyticsUuid = UUID.randomUUID().toString()
    service<PasswordSafe>().setPassword(credentialAttributes, UUID.randomUUID().toString())
    return analyticsUuid
  }

  private fun getAnalyticsUuid(): String {
    return service<PasswordSafe>().getPassword(credentialAttributes) ?: throw UUIDNotFoundException()
  }

  private fun isAnalyticsUuidExists(): Boolean {
    val analyticsUuid = service<PasswordSafe>().getPassword(credentialAttributes)
    return analyticsUuid != null && analyticsUuid != ""
  }

  private fun getAnalyticsBasicUserId(): String {
    return analyticsProperties.getProperty("segment.basic.user.id")
  }

  private fun createAnalytics(): Analytics {
    return Analytics.builder(analyticsProperties.getProperty("segment.api.key")).build()
  }

  private val licenceProvider = service<PolicyProvider>()

  override var isAnalyticsEnabled: Boolean
    get() = state.isAnalyticsEnabled
    set(value) {
      state.isAnalyticsEnabled = value
    }
  override var isUserAcknowledged: Boolean
    get() = state.lastAcknowledgedPolicyVersion >= licenceProvider.version
    set(value) {
      val analyticsVersion = licenceProvider.version
      state.lastAcknowledgedPolicyVersion = if (value) {
        analyticsVersion
      } else {
        POLICY_NOT_ACKNOWLEDGED
      }
    }


  override fun trackAnalyticsEvent(eventName: String, eventProperties: Map<String, String>) {

    if (!isAnalyticsEnabled || !isUserAcknowledged) return

    if (!isAnalyticsUuidExists())
      identifyAnalyticsUser()


    enqueueAnalytics(eventName, eventProperties)
  }

  override fun trackAnalyticsEvent(event: AnalyticsEvent) {
    trackAnalyticsEvent(event.eventName, event.getProps())
  }

  private fun enqueueAnalytics(eventName: String, eventProperties: Map<String, String> = emptyMap()) {
    val mutableEventProps = eventProperties.toMutableMap()
    mutableEventProps["f_user_id"] = getAnalyticsUuid()
    analytics.enqueue(
      TrackMessage.builder(eventName)
        .userId(getAnalyticsBasicUserId())
        .properties(mutableEventProps)
    )
  }

  private fun identifyAnalyticsUser() {
    if (!myState.wasIdentified) {
      if (!isAnalyticsUuidExists())
        createAnalyticsUuid()

      enqueueAnalytics("f_users")

      myState.wasIdentified = true
    }
  }

  override fun getState(): AnalyticsState {
    return myState
  }

}

class UUIDNotFoundException : Exception("UUID for analytics cannot be found")