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

/** Analytics dev configurations. */
internal val analyticsProperties by lazy {
  Properties().apply {
    load(AnalyticsService::class.java.classLoader.getResourceAsStream("analytics.properties"))
  }
}

/**
 * Implementation of analytics service using Segment service.
 * @author Valiantsin Krus
 */
@State(
  name = "eu.ibagroup.formainframe.analytics.AnalyticsService",
  storages = [Storage(value = "for-mainframe-analytics.xml", exportable = true)]
)
class AnalyticsServiceImpl : AnalyticsService, PersistentStateComponent<AnalyticsState> {

  companion object {
    val myState = AnalyticsState()
  }

  private val credentialAttributes = CredentialAttributes("formainframeAnalyticsCredentials")

  /**
   * Created segment analytics instance.
   * @see createAnalytics
   */
  val analytics: Analytics by lazy {
    createAnalytics()
  }

  /** Loads the state of analytics configurations from configuration file. */
  override fun loadState(state: AnalyticsState) {
    XmlSerializerUtil.copyBean(state, myState)
  }

  /**
   * Creates unique key for analytics and register it with credential attributes in intellij credentials storage.
   * @return created unique key in UUID format.
   */
  private fun createAnalyticsUuid(): String {
    val analyticsUuid = UUID.randomUUID().toString()
    service<PasswordSafe>().setPassword(credentialAttributes, UUID.randomUUID().toString())
    return analyticsUuid
  }

  /**
   * Finds unique key of analytics in intellij credentials storage by credential attributes.
   * @throws UUIDNotFoundException
   * @return analytics unique key if it was found (throws exception otherwise).
   */
  private fun getAnalyticsUuid(): String {
    return service<PasswordSafe>().getPassword(credentialAttributes) ?: throw UUIDNotFoundException()
  }

  /**
   * Checks if the analytics unique key has already registered by looking in intellij credentials storage.
   * @return true if uuid was found (not null, not empty) and false otherwise.
   */
  private fun isAnalyticsUuidExists(): Boolean {
    val analyticsUuid = service<PasswordSafe>().getPassword(credentialAttributes)
    return analyticsUuid != null && analyticsUuid != ""
  }

  /**
   * Real user ID in segment database.
   *
   * The real user can be only one. This restriction was made to bypass
   * the limit on the number of users in the free version of segment.
   * All users that are registered in database are stored in f_users
   * table (from fake_users).
   */
  private fun getAnalyticsBasicUserId(): String {
    return analyticsProperties.getProperty("segment.basic.user.id")
  }

  /**
   * Creates instance of segment analytics to track events.
   * @return created instance of segment analytics class.
   */
  private fun createAnalytics(): Analytics {
    return Analytics.builder(analyticsProperties.getProperty("segment.api.key")).build()
  }

  private val licenseProvider = PolicyProvider.getService()

  /** Defines if the user enable analytics. */
  override var isAnalyticsEnabled: Boolean
    get() = state.isAnalyticsEnabled
    set(value) {
      state.isAnalyticsEnabled = value
    }

  /** Defines if the user is acknowledged with the last version of analytics policy. */
  override var isUserAcknowledged: Boolean
    get() = state.lastAcknowledgedPolicyVersion >= licenseProvider.version
    set(value) {
      val analyticsVersion = licenseProvider.version
      state.lastAcknowledgedPolicyVersion = if (value) {
        analyticsVersion
      } else {
        POLICY_NOT_ACKNOWLEDGED
      }
    }

  /**
   * Track event if all necessary conditions (analytics enabled, user acknowledged) are met.
   * @see AnalyticsService.trackAnalyticsEvent
   */
  override fun trackAnalyticsEvent(eventName: String, eventProperties: Map<String, String>) {

    if (!isAnalyticsEnabled || !isUserAcknowledged) return

    if (!isAnalyticsUuidExists())
      identifyAnalyticsUser()


    enqueueAnalytics(eventName, eventProperties)
  }

  /**
   * @see AnalyticsService.trackAnalyticsEvent
   */
  override fun trackAnalyticsEvent(event: AnalyticsEvent) {
    trackAnalyticsEvent(event.eventName, event.getProps())
  }

  /**
   * Tracks analytics event.
   * @param eventName name of the event to track
   * @param eventProperties map of the properties specified for the specific action.
   */
  private fun enqueueAnalytics(eventName: String, eventProperties: Map<String, String> = emptyMap()) {
    val mutableEventProps = eventProperties.toMutableMap()
    mutableEventProps["f_user_id"] = getAnalyticsUuid()
    analytics.enqueue(
      TrackMessage.builder(eventName)
        .userId(getAnalyticsBasicUserId())
        .properties(mutableEventProps)
    )
  }

  /**
   * Creates fake user in f_users table in segment database.
   */
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
