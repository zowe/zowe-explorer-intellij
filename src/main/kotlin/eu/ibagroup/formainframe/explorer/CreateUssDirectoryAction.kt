package eu.ibagroup.formainframe.explorer

import eu.ibagroup.formainframe.explorer.ui.CreateFileState
import eu.ibagroup.formainframe.explorer.ui.emptyDirState

class CreateUssDirectoryAction : CreateUssEntityAction() {

  override val fileType: CreateFileState
    get() = emptyDirState

  override val ussFileType: String
    get() = "USS directory"
}