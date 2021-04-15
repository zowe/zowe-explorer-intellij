package eu.ibagroup.formainframe.common.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import java.awt.Component

abstract class StatefulDialog<T : Any>(
  project: Project? = null,
  parentComponent: Component? = null,
  canBeParent: Boolean = true,
  ideModalityType: IdeModalityType = IdeModalityType.IDE,
  createSouth: Boolean = true
) : DialogWrapper(
  project,
  parentComponent,
  canBeParent,
  ideModalityType,
  createSouth
), StatefulComponent<T>

fun <T : Any> showUntilDone(
  initialState: T,
  factory: (state: T) -> StatefulDialog<T>,
  test: (T) -> Boolean
): T? {
  var dialog: StatefulDialog<T>
  var stateToInitializeDialog = initialState
  while (true) {
    dialog = factory(stateToInitializeDialog)
    if (dialog.showAndGet()) {
      stateToInitializeDialog = dialog.state
      if (test(stateToInitializeDialog)) {
        return stateToInitializeDialog
      }
    } else {
      break
    }
  }
  return null
}