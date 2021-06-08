package eu.ibagroup.formainframe.utils

class UnorderedPair<T>(val first: T, val second: T) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is UnorderedPair<*>) return false



    return true
  }

  override fun hashCode(): Int {
    return first.hashCode() + second.hashCode()
  }

}