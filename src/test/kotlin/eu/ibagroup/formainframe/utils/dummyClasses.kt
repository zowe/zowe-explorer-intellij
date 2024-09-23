/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */

package eu.ibagroup.formainframe.utils

import com.intellij.openapi.editor.Document
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.encoding.EncodingManager
import java.nio.charset.Charset

open class TestEncodingManager: EncodingManager() {
  override fun isNative2Ascii(virtualFile: VirtualFile): Boolean {
    TODO("Not yet implemented")
  }

  override fun isNative2AsciiForPropertiesFiles(): Boolean {
    TODO("Not yet implemented")
  }

  override fun getDefaultCharset(): Charset {
    TODO("Not yet implemented")
  }

  override fun getEncoding(virtualFile: VirtualFile?, useParentDefaults: Boolean): Charset? {
    TODO("Not yet implemented")
  }

  override fun setEncoding(virtualFileOrDir: VirtualFile?, charset: Charset?) {
    TODO("Not yet implemented")
  }

  override fun getDefaultCharsetForPropertiesFiles(virtualFile: VirtualFile?): Charset? {
    TODO("Not yet implemented")
  }

  override fun getDefaultConsoleEncoding(): Charset {
    TODO("Not yet implemented")
  }

  override fun getFavorites(): MutableCollection<Charset> {
    TODO("Not yet implemented")
  }

  override fun setNative2AsciiForPropertiesFiles(virtualFile: VirtualFile?, native2Ascii: Boolean) {
    TODO("Not yet implemented")
  }

  override fun getDefaultCharsetName(): String {
    TODO("Not yet implemented")
  }

  override fun setDefaultCharsetForPropertiesFiles(virtualFile: VirtualFile?, charset: Charset?) {
    TODO("Not yet implemented")
  }

  override fun getCachedCharsetFromContent(document: Document): Charset? {
    TODO("Not yet implemented")
  }
}
