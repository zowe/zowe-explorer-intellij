package eu.ibagroup.formainframe.config.connect

import eu.ibagroup.formainframe.utils.crudable.EntityWithUuid
import eu.ibagroup.formainframe.utils.crudable.annotations.Column
import eu.ibagroup.formainframe.utils.crudable.annotations.ForeignKey

class ConnectionConfig : EntityWithUuid {
  @Column
  var name = ""

  @Column
  @ForeignKey(foreignClass = UrlConnection::class)
  var urlConnectionUuid = ""

  constructor() {}
  constructor(uuid: String, name: String, urlConnectionUuid: String) : super(uuid) {
    this.name = name
    this.urlConnectionUuid = urlConnectionUuid
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false
    if (!super.equals(other)) return false
    val that = other as ConnectionConfig
    return if (name != that.name) false else urlConnectionUuid == that.urlConnectionUuid
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + name.hashCode()
    result = 31 * result + urlConnectionUuid.hashCode()
    return result
  }

  override fun toString(): String {
    return "ConnectionConfig{" +
        "name='" + name + '\'' +
        ", urlConnectionUuid='" + urlConnectionUuid + '\'' +
        ", uuid='" + uuid + '\'' +
        '}'
  }
}