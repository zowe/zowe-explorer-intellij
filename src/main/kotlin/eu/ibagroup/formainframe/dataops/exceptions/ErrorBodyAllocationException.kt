package eu.ibagroup.formainframe.dataops.exceptions

class ErrorBodyAllocationException(
    override val message: String,
    val errorParams: Map<*, *>
    ) : Throwable(message)