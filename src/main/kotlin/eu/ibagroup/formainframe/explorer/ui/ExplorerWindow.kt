package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBScrollPane
import javax.swing.JComponent
import javax.swing.JScrollPane
import javax.swing.JTree

class ExplorerWindow(parentDisposable: Disposable) : SimpleToolWindowPanel(true, true), Disposable {

  private var wsTree: JTree? = null
  private var toolbarComponent: JComponent? = null
  private var scrollPane: JScrollPane? = null

  init {
    Disposer.register(parentDisposable, this)

    if (wsTree != null) {
      scrollPane = JBScrollPane(wsTree).apply { setContent(this) }
    }
  }

  override fun dispose() {
    wsTree = null
    toolbarComponent = null
    scrollPane = null
  }
}