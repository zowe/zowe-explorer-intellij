package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.layout.panel
import eu.ibagroup.formainframe.common.ui.DialogMode
import eu.ibagroup.formainframe.common.ui.DialogState
import eu.ibagroup.formainframe.common.ui.StatefulComponent
import eu.ibagroup.r2z.Dataset
import javax.swing.JComponent

class DatasetInfoDialog(project: Project?, override var state : InfoState = InfoState()) : DialogWrapper(project), StatefulComponent<InfoState> {


  override fun createCenterPanel(): JComponent {
    return panel {
      row {
        label("Dataset name ${state.dataset?.name}")
        //TODO
      }
    }
  }

  init {
    title = "Datset Info"
    init()
  }

}

class InfoState(override var mode: DialogMode = DialogMode.CREATE) : DialogState {

  var dataset: Dataset? = null

}