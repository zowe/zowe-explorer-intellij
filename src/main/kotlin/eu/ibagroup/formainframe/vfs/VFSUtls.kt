package eu.ibagroup.formainframe.vfs

private val encodeSplitRegex = Regex(
  "((?<=\\[)|(?=\\[)|(?<=])|(?=])|(?<=\\|)|(?=\\|))"
)

private val decodeSplitRegex = Regex(
  "((?<=\\[5B])|(?=\\[5B])|(?<=(\\[7C]))|(?=(\\[7C]))|(?<=(\\[5D]))|(?=(\\[5D])))"
)

fun encodeFilename(toEncode: String): String {
  return toEncode.split(encodeSplitRegex).joinToString(separator = "") {
    when (it) {
      "[" -> "[5B]"
      "]" -> "[5D]"
      "|" -> "[7C]"
      else -> it
    }
  }
}

fun decodeFilename(toDecode: String): String {
  return toDecode.split(decodeSplitRegex).joinToString(separator = "") {
    when (it) {
      "[5B]" -> "["
      "[5D]" -> "]"
      "[7C]" -> "|"
      else -> it
    }
  }
}

