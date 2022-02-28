/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops.synchronizer

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
