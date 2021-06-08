package eu.ibagroup.formainframe.dataops.attributes

import eu.ibagroup.formainframe.utils.Copyable
import eu.ibagroup.formainframe.utils.PasteAcceptor
import eu.ibagroup.r2z.XIBMDataType

interface FileAttributes : Cloneable, Copyable, PasteAcceptor {

  val name: String

  val length: Long

  var contentMode: XIBMDataType

  public override fun clone(): FileAttributes

  override val isCopyPossible: Boolean
    get() = true

  override val isPastePossible: Boolean
    get() = false

}