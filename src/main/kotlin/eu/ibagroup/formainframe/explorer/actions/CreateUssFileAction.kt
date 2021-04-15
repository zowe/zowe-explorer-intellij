package eu.ibagroup.formainframe.explorer.actions

import eu.ibagroup.formainframe.explorer.ui.CreateFileDialogState
import eu.ibagroup.formainframe.explorer.ui.emptyFileState

class CreateUssFileAction : CreateUssEntityAction() {

  override val fileType: CreateFileDialogState
    get() = emptyFileState

  override val ussFileType: String
    get() = "USS file"
}