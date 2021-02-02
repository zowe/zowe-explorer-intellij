package eu.ibagroup.formainframe.ui

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.layout.panel
import javax.swing.JComponent

class DummyDialog : DialogWrapper(false) {

  override fun createCenterPanel(): JComponent {
    return panel {

    }
  }

}