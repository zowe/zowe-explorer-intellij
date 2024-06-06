/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.EditorDataProvider
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorComposite
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorNavigatable
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.JComponent

open class TestFileEditorManager : FileEditorManager() {
  override fun getComposite(file: VirtualFile): FileEditorComposite? {
    TODO("Not yet implemented")
  }

  override fun openFile(file: VirtualFile, focusEditor: Boolean): Array<FileEditor> {
    TODO("Not yet implemented")
  }

  override fun openFile(file: VirtualFile): MutableList<FileEditor> {
    TODO("Not yet implemented")
  }

  override fun closeFile(file: VirtualFile) {
    TODO("Not yet implemented")
  }

  override fun openTextEditor(descriptor: OpenFileDescriptor, focusEditor: Boolean): Editor? {
    TODO("Not yet implemented")
  }

  override fun getSelectedTextEditor(): Editor? {
    TODO("Not yet implemented")
  }

  override fun isFileOpen(file: VirtualFile): Boolean {
    TODO("Not yet implemented")
  }

  override fun getOpenFiles(): Array<VirtualFile> {
    TODO("Not yet implemented")
  }

  override fun getOpenFilesWithRemotes(): MutableList<VirtualFile> {
    TODO("Not yet implemented")
  }

  override fun getSelectedFiles(): Array<VirtualFile> {
    TODO("Not yet implemented")
  }

  override fun getSelectedEditors(): Array<FileEditor> {
    TODO("Not yet implemented")
  }

  override fun getSelectedEditor(file: VirtualFile): FileEditor? {
    TODO("Not yet implemented")
  }

  override fun getEditors(file: VirtualFile): Array<FileEditor> {
    TODO("Not yet implemented")
  }

  override fun getAllEditors(file: VirtualFile): Array<FileEditor> {
    TODO("Not yet implemented")
  }

  override fun getAllEditors(): Array<FileEditor> {
    TODO("Not yet implemented")
  }

  override fun getAllEditorList(file: VirtualFile): MutableList<FileEditor> {
    TODO("Not yet implemented")
  }

  override fun addTopComponent(editor: FileEditor, component: JComponent) {
    TODO("Not yet implemented")
  }

  override fun removeTopComponent(editor: FileEditor, component: JComponent) {
    TODO("Not yet implemented")
  }

  override fun addBottomComponent(editor: FileEditor, component: JComponent) {
    TODO("Not yet implemented")
  }

  override fun removeBottomComponent(editor: FileEditor, component: JComponent) {
    TODO("Not yet implemented")
  }

  override fun openFileEditor(descriptor: FileEditorNavigatable, focusEditor: Boolean): MutableList<FileEditor> {
    TODO("Not yet implemented")
  }

  override fun getProject(): Project {
    TODO("Not yet implemented")
  }

  override fun registerExtraEditorDataProvider(provider: EditorDataProvider, parentDisposable: Disposable?) {
    TODO("Not yet implemented")
  }

  override fun getData(dataId: String, editor: Editor, caret: Caret): Any? {
    TODO("Not yet implemented")
  }

  override fun setSelectedEditor(file: VirtualFile, fileEditorProviderId: String) {
    TODO("Not yet implemented")
  }

  override fun runWhenLoaded(editor: Editor, runnable: Runnable) {
    TODO("Not yet implemented")
  }

}
