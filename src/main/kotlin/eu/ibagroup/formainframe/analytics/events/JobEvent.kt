package eu.ibagroup.formainframe.analytics.events

class JobEvent(
  private val jobAction: JobAction
) : AnalyticsEvent("jobs") {
  override fun getProps(): Map<String, String> {
    return mapOf(Pair("job_action", jobAction.toString()))
  }
}


enum class JobAction(val value: String) {
  SUBMIT("SUBMIT"),
  PURGE("PURGE"),
  HOLD("HOLD"),
  RELEASE("RELEASE");

  override fun toString(): String {
    return value
  }
}