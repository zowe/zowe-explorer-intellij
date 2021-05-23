package eu.ibagroup.formainframe.analytics.events

class WorkingSetEvent(
  override var actionType: ActionType
) : CrudAnalyticsEvent("Working Sets", actionType)