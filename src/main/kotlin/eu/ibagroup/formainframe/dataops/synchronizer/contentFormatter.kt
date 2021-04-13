package eu.ibagroup.formainframe.dataops.synchronizer

private const val NEW_LINE = "\n"

fun String.removeLastNewLine(): String {
  return if (endsWith(NEW_LINE)) {
    removeSuffix(NEW_LINE)
  } else {
    this
  }
}

fun String.addNewLine(): String {
  return this + NEW_LINE
}