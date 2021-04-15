package eu.ibagroup.formainframe.config.connect

import eu.ibagroup.formainframe.utils.crudable.EntityWithUuid
import eu.ibagroup.formainframe.utils.crudable.annotations.Column

class UrlConnection : EntityWithUuid {
  @Column
  var url = ""

  @Column
  var isAllowSelfSigned = false

  constructor()
  constructor(uuid: String, url: String, isAllowSelfSigned: Boolean) : super(uuid) {
    this.url = url
    this.isAllowSelfSigned = isAllowSelfSigned
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false
    if (!super.equals(other)) return false
    val that = other as UrlConnection
    return if (isAllowSelfSigned != that.isAllowSelfSigned) false else url == that.url
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + url.hashCode()
    result = 31 * result + if (isAllowSelfSigned) 1 else 0
    return result
  }

  override fun toString(): String {
    return "ZOSMFUrlConnection{" +
        "uuid='" + uuid + '\'' +
        ", url='" + url + '\'' +
        ", isAllowSelfSigned=" + isAllowSelfSigned +
        '}'
  }
}