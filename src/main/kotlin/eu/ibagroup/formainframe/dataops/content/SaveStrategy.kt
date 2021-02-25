package eu.ibagroup.formainframe.dataops.content

import com.intellij.openapi.vfs.VirtualFile

fun interface SaveStrategy {
  fun decide(
    file: VirtualFile,
    lastSuccessfulState: ByteArray,
    currentRemoteState: ByteArray
  ): Boolean
}