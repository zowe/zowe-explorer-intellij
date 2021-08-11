package eu.ibagroup.formainframe.analytics.events

class MigrateEvent(
  private val migrateActionType: MigrateActionType
) : AnalyticsEvent("migrations") {
  override fun getProps(): Map<String, String> {
    return mapOf(Pair("actionType", migrateActionType.toString()))
  }
}

enum class MigrateActionType(val value: String) {
  MIGRATE("MIGRATE"),
  RECALL("RECALL");

  override fun toString(): String {
    return value
  }
}