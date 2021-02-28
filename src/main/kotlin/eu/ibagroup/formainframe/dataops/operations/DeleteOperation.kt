package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.vfs.VirtualFile

data class DeleteOperation(override val files: List<VirtualFile>) : Operation {

}