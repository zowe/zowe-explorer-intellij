package eu.ibagroup.formainframe.explorer

import com.intellij.ide.projectView.ViewSettings

interface ExplorerViewSettings : ViewSettings {

  val showVolser
    get() = false

  val showMasksAndPathAsSeparateDirs
    get() = true

  val showWorkingSetInfo
    get() = false

  val flattenUssDirectories
    get() = true

}