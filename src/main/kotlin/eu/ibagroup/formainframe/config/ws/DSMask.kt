package eu.ibagroup.formainframe.config.ws

import java.util.*

class DSMask {

  var mask = ""

  var excludes: MutableList<String> = ArrayList()

  var volser = ""

  var isSingle = false

  constructor()

  constructor(mask: String, excludes: MutableList<String>) {
    this.mask = mask
    this.excludes = excludes
  }

  constructor(mask: String, excludes: MutableList<String>, volser: String, isSingle: Boolean) {
    this.mask = mask
    this.excludes = excludes
    this.volser = volser
    this.isSingle = isSingle
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false
    val dsMask = other as DSMask
    return isSingle == dsMask.isSingle && mask == dsMask.mask && excludes == dsMask.excludes && volser == dsMask.volser
  }

  override fun hashCode(): Int {
    return Objects.hash(mask, excludes, volser, isSingle)
  }

  override fun toString(): String {
    return "DSMask{" +
        "mask='" + mask + '\'' +
        ", excludes=" + excludes +
        ", volser='" + volser + '\'' +
        ", isSingle=" + isSingle +
        '}'
  }

}