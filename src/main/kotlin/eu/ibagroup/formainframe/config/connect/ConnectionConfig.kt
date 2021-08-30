package eu.ibagroup.formainframe.config.connect

import eu.ibagroup.formainframe.utils.crudable.EntityWithUuid
import eu.ibagroup.formainframe.utils.crudable.annotations.Column
import eu.ibagroup.formainframe.utils.crudable.annotations.ForeignKey
import eu.ibagroup.r2z.CodePage
import eu.ibagroup.r2z.annotations.ZVersion
import org.jetbrains.annotations.NotNull

class ConnectionConfig : EntityWithUuid {
  @Column
  var name = ""

  @Column
  var url = ""

  @Column
  var isAllowSelfSigned = true

  @Column
  var codePage = CodePage.IBM_1047

  @Column
  var zVersion = ZVersion.ZOS_2_1

  constructor() {}

  constructor(
    uuid: @NotNull String,
    name: String,
    url: String,
    isAllowSelfSigned: Boolean,
    codePage: CodePage,
    zVersion: ZVersion
  ) : super(uuid) {
    this.name = name
    this.url = url
    this.isAllowSelfSigned = isAllowSelfSigned
    this.codePage = codePage
    this.zVersion = zVersion
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false

    other as ConnectionConfig

    if (name != other.name) return false
    if (url != other.url) return false
    if (isAllowSelfSigned != other.isAllowSelfSigned) return false
    if (codePage != other.codePage) return false
    if (zVersion != other.zVersion) return false

    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + name.hashCode()
    result = 31 * result + url.hashCode()
    result = 31 * result + isAllowSelfSigned.hashCode()
    result = 31 * result + codePage.hashCode()
    result = 31 * result + zVersion.hashCode()
    return result
  }

  override fun toString(): String {
    return "ConnectionConfig(name='$name', url='$url', isAllowSelfSigned=$isAllowSelfSigned, codePage=$codePage, zVersion=$zVersion)"
  }


}