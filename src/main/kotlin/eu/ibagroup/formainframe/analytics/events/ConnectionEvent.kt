package eu.ibagroup.formainframe.analytics.events

class ConnectionEvent(
  override var actionType: ActionType
) : CrudAnalyticsEvent("connections", actionType)