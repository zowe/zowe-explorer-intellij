package eu.ibagroup.formainframe.dataops.exceptions

class ErrorBodyAllocationException(
  override val message: String,
  val errorParams: Map<*, *>
) : Throwable(message) {
  override fun toString(): String {
    return "${message}\n\n" +
        "CATEGORY: ${errorParams["category"]}\n" +
        "MESSAGE: ${errorParams["message"]}\n" +
        "RETURN CODE: ${errorParams["rc"]}\n" +
        "REASON: ${errorParams["reason"]}\n" +
        "STACK:\n${errorParams["stack"]}"
  }
}
