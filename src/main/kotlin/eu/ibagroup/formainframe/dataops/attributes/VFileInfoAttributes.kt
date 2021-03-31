package eu.ibagroup.formainframe.dataops.attributes

import eu.ibagroup.r2z.XIBMDataType

interface VFileInfoAttributes : Cloneable {

  val name: String

  val length: Long

  var contentMode: XIBMDataType

  public override fun clone(): VFileInfoAttributes

}