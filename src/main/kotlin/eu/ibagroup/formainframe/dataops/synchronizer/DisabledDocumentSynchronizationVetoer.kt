package eu.ibagroup.formainframe.dataops.synchronizer

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentSynchronizationVetoer
import com.intellij.openapi.vfs.VirtualFile

class DisabledDocumentSynchronizationVetoer : FileDocumentSynchronizationVetoer() {

  override fun mayReloadFileContent(file: VirtualFile, document: Document): Boolean {
    return super.mayReloadFileContent(file, document)
  }

  override fun maySaveDocument(document: Document, isSaveExplicit: Boolean): Boolean {
    return super.maySaveDocument(document, isSaveExplicit)
  }

}