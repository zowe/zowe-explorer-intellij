package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.UnitOperation
import eu.ibagroup.formainframe.dataops.attributes.VFileInfoAttributes

data class RenameOperation(
  val file: VirtualFile,
  val attributes: VFileInfoAttributes,
  val newName: String
) : UnitOperation