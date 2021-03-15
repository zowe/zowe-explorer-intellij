package eu.ibagroup.formainframe.explorer.ui

import com.intellij.CommonBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.layout.panel
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.Action
import javax.swing.JComponent


class OverwriteRenameFileDecisionDialog(
  private val conflicts: List<Pair<VirtualFile, VirtualFile>>,
  project: Project,
  parentDisposable: Disposable,
) : DialogWrapper(project, true), Disposable {

  private enum class Choice {
    RENAME, OVERWRITE, OVERWRITE_REST, SKIP, SKIP_REST
  }

  private val currentElementIdx = AtomicInteger(0)

  init {
    if (conflicts.isEmpty()) {
      throw IllegalArgumentException("Useless for empty conflicts list")
    }
    Disposer.register(parentDisposable, this)
    redraw()
  }

  private var panel: JComponent? = null

  override fun doOKAction() {
    if (isCloseOnOkButton()) {
      super.doOKAction()
    } else {
      redraw()
    }
  }

  private fun isCloseOnOkButton(): Boolean {
    return currentElementIdx.get() == conflicts.size - 1
      || currentChoice == Choice.OVERWRITE_REST
      || currentChoice == Choice.SKIP_REST
  }

  private var currentChoice = Choice.RENAME

  private fun redraw() {
    panel?.updateUI()
    val currentPair = conflicts[currentElementIdx.get()]
    val destinationFile = currentPair.first
    val sourceFile = currentPair.second
    title = "From file ${sourceFile.name} to ${destinationFile.name}"
    if (isCloseOnOkButton()) {
      okAction.putValue(Action.NAME, CommonBundle.getOkButtonText())
    } else {
      okAction.putValue(Action.NAME, "Next")
    }
  }

  override fun createCenterPanel(): JComponent {
    val currentPair = conflicts[currentElementIdx.get()]
    val destinationFile = currentPair.first
    val sourceFile = currentPair.second
    return panel {
      row(separated = true) {
        radioButton(
          text = "Rename",
          getter = { currentChoice == Choice.RENAME },
          setter = {
            if (it) {
              currentChoice = Choice.RENAME
              redraw()
            }
          }
        )
        cell {
          label("New name")
          //textField()
        }
      }
    }.also { panel = it }
  }

  override fun dispose() {
    super.dispose()
  }
}