package eu.ibagroup.formainframe.dataops.attributes

interface VFileInfoAttributes : Cloneable {

  val name: String

  val length: Long

  public override fun clone(): VFileInfoAttributes

}