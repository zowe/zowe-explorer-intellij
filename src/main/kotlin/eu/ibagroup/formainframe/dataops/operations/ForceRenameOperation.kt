package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.UnitOperation
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.explorer.Explorer

data class ForceRenameOperation(
    val file: VirtualFile,
    val attributes: FileAttributes,
    val newName: String,
    val override: Boolean,
    val explorer: Explorer<*>?
) : UnitOperation
