/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.explorer.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DoNotAskOption
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.messages.MessagesService
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Function
import com.intellij.util.PairFunction
import java.awt.Component
import javax.swing.Icon
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JTextField

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

open class TestMessagesService : MessagesService {
  override fun showChooseDialog(
    project: Project?,
    parentComponent: Component?,
    message: String?,
    title: String?,
    values: Array<String?>?,
    initialValue: String?,
    icon: Icon?
  ): Int {
    TODO("Not yet implemented")
  }

  override fun showEditableChooseDialog(
    message: String?,
    title: String?,
    icon: Icon?,
    values: Array<String?>?,
    initialValue: String?,
    validator: InputValidator?
  ): String? {
    TODO("Not yet implemented")
  }

  override fun showErrorDialog(project: Project?, message: String?, title: String) {
    TODO("Not yet implemented")
  }

  override fun showInputDialog(
    project: Project?,
    parentComponent: Component?,
    message: String?,
    title: String?,
    icon: Icon?,
    initialValue: String?,
    validator: InputValidator?,
    selection: TextRange?,
    comment: String?
  ): String? {
    TODO("Not yet implemented")
  }

  override fun showInputDialogWithCheckBox(
    message: String?,
    title: String?,
    checkboxText: String?,
    checked: Boolean,
    checkboxEnabled: Boolean,
    icon: Icon?,
    initialValue: String?,
    validator: InputValidator?
  ): Pair<String?, Boolean?> {
    TODO("Not yet implemented")
  }

  override fun showMessageDialog(
    project: Project?,
    parentComponent: Component?,
    message: String?,
    title: String?,
    options: Array<String>,
    defaultOptionIndex: Int,
    focusedOptionIndex: Int,
    icon: Icon?,
    doNotAskOption: DoNotAskOption?,
    alwaysUseIdeaUI: Boolean,
    helpId: String?
  ): Int {
    TODO("Not yet implemented")
  }

  override fun showMoreInfoMessageDialog(
    project: Project?,
    message: String?,
    title: String?,
    moreInfo: String?,
    options: Array<String?>?,
    defaultOptionIndex: Int,
    focusedOptionIndex: Int,
    icon: Icon?
  ): Int {
    TODO("Not yet implemented")
  }

  override fun showMultilineInputDialog(
    project: Project?,
    message: String?,
    title: String?,
    initialValue: String?,
    icon: Icon?,
    validator: InputValidator?
  ): String? {
    TODO("Not yet implemented")
  }

  override fun showPasswordDialog(
    project: Project?,
    message: String?,
    title: String?,
    icon: Icon?,
    validator: InputValidator?
  ): String? {
    TODO("Not yet implemented")
  }

  override fun showPasswordDialog(
    parentComponent: Component,
    message: String?,
    title: String?,
    icon: Icon?,
    validator: InputValidator?
  ): CharArray? {
    TODO("Not yet implemented")
  }

  override fun showTextAreaDialog(
    textField: JTextField?,
    title: String?,
    dimensionServiceKey: String?,
    parser: Function<in String?, out MutableList<String?>?>?,
    lineJoiner: Function<in MutableList<String?>?, String?>?
  ) {
    TODO("Not yet implemented")
  }

  override fun showTwoStepConfirmationDialog(
    message: String?,
    title: String?,
    options: Array<String?>?,
    checkboxText: String?,
    checked: Boolean,
    defaultOptionIndex: Int,
    focusedOptionIndex: Int,
    icon: Icon?,
    exitFunc: PairFunction<in Int?, in JCheckBox?, Int?>?
  ): Int {
    TODO("Not yet implemented")
  }

}
