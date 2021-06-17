package eu.ibagroup.formainframe.analytics.events

abstract class AnalyticsEvent(
  open var eventName: String
) {
  abstract fun getProps(): Map<String, String>
}

open class CrudAnalyticsEvent(
  override var eventName: String,
  open var actionType: ActionType
) : AnalyticsEvent(eventName){
  override fun getProps(): Map<String, String> {
    return mutableMapOf(Pair("action_type", actionType.toString()))
  }
}

enum class ActionType(val value: String) {
  CREATE("CREATE"),
  EDIT("EDIT"),
  UPDATE("UPDATE"),
  DELETE("DELETE"),
  READ("READ");

  override fun toString(): String {
    return value
  }
}