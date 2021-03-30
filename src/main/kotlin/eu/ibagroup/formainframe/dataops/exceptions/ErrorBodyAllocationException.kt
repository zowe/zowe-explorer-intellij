package eu.ibagroup.formainframe.dataops.exceptions

class ErrorBodyAllocationException(
    override val message: String,
    val errorParams: Map<*, *>
    ) : Throwable(message)

fun getFullAllocationErrorString(errorBodyAllocationException: ErrorBodyAllocationException): String {
    return "${errorBodyAllocationException.message}\n\n" +
            "CATEGORY: ${errorBodyAllocationException.errorParams["category"]}\n" +
            "MESSAGE: ${errorBodyAllocationException.errorParams["message"]}\n" +
            "RETURN CODE: ${errorBodyAllocationException.errorParams["rc"]}\n" +
            "REASON: ${errorBodyAllocationException.errorParams["reason"]}\n" +
            "STACK:\n${errorBodyAllocationException.errorParams["stack"]}"
}