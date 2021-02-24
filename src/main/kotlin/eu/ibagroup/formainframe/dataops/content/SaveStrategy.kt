package eu.ibagroup.formainframe.dataops.content

import com.intellij.openapi.vfs.VirtualFile

typealias SaveStrategy = (
  lastSuccessfulState: ByteArray,
  currentRemoteState: ByteArray,
  currentFile: VirtualFile
) -> Boolean