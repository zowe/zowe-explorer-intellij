package eu.ibagroup.formainframe.dataops.synchronizer

import com.intellij.openapi.vfs.VirtualFile

fun interface SaveStrategy {
  fun decide(
    file: VirtualFile,
    lastSuccessfulState: ByteArray,
    currentRemoteState: ByteArray
  ): Boolean
}