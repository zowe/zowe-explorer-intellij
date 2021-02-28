package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.vfs.VirtualFile

interface Operation {

  val files: List<VirtualFile>

}