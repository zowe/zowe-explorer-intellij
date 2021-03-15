package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.vfs.VirtualFile

data class RenameOperation(val file: VirtualFile, val newName : String) : Operation {

  override val files: List<VirtualFile> = listOf(file)

}
