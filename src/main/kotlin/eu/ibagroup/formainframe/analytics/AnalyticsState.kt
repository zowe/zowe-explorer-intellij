package eu.ibagroup.formainframe.analytics

data class AnalyticsState(
  var isAnalyticsEnabled: Boolean = false,
  var lastAcknowledgedPolicyVersion: Int = POLICY_NOT_ACKNOWLEDGED,
  var wasIdentified: Boolean = false,
)

const val POLICY_NOT_ACKNOWLEDGED = 0