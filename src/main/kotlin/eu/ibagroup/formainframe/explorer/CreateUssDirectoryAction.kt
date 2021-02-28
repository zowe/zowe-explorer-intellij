package eu.ibagroup.formainframe.explorer

import eu.ibagroup.formainframe.explorer.ui.CreateFileDialogState
import eu.ibagroup.formainframe.explorer.ui.emptyDirState

class CreateUssDirectoryAction : CreateUssEntityAction() {

  override val fileType: CreateFileDialogState
    get() = emptyDirState

  override val ussFileType: String
    get() = "USS directory"
}