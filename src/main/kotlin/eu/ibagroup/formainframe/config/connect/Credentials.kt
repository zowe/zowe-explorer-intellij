package eu.ibagroup.formainframe.config.connect

import eu.ibagroup.formainframe.utils.crudable.annotations.Column
import eu.ibagroup.formainframe.utils.crudable.annotations.ForeignKey

class Credentials {
  @Column(unique = true)
  @ForeignKey(foreignClass = ConnectionConfig::class)
  var connectionConfigUuid = ""

  @Column
  var username = ""

  @Column
  var password = ""

  constructor()
  constructor(connectionConfigUuid: String, username: String, password: String) {
    this.connectionConfigUuid = connectionConfigUuid
    this.username = username
    this.password = password
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false
    val that = other as Credentials
    if (connectionConfigUuid != that.connectionConfigUuid) return false
    return if (username != that.username) false else password == that.password
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + connectionConfigUuid.hashCode()
    result = 31 * result + username.hashCode()
    result = 31 * result + password.hashCode()
    return result
  }

  override fun toString(): String {
    return "Credentials{" +
        "connectionConfigUuid='" + connectionConfigUuid + '\'' +
        ", username='" + username + '\'' +
        ", password='" + password + '\'' +
        '}'
  }
}