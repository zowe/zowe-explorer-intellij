/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.DialogWrapper.IdeModalityType
import org.zowe.explorer.common.ui.StatefulComponent
import java.awt.Component

/**
 * Base class for dialogs with deferred (lazy) construction of the underlying [DialogWrapper].
 *
 * The actual [DialogWrapper] instance is not created until [showAndGet] is called, which
 * allows subclasses to delay potentially expensive UI initialization until the dialog is
 * actually displayed. The dialog's current input/output state is tracked via [StatefulComponent].
 *
 * @param T the type that represents the dialog's state.
 * @param project the [Project] context passed to the underlying [DialogWrapper], or `null`
 *   for a project-agnostic dialog.
 * @param parentComponent the AWT [Component] to use as a parent for the dialog window, or
 *   `null` to use the IDE frame.
 * @param canBeParent whether this dialog may serve as a parent for other modal dialogs.
 * @param ideModalityType the [IdeModalityType] controlling the modality scope of the dialog.
 * @param createSouth whether to create the default south panel (OK/Cancel buttons).
 */
abstract class LazyDialog<T : Any>(
  project: Project? = null,
  parentComponent: Component? = null,
  canBeParent: Boolean = true,
  ideModalityType: IdeModalityType = IdeModalityType.IDE,
  createSouth: Boolean = true
) : StatefulComponent<T> {

  /**
   * Creates and returns a fully initialised [DialogWrapper] instance.
   *
   * Called each time [showAndGet] is invoked. Implementations should build
   * and configure the dialog here, using the current [state] as needed.
   */
  protected abstract fun produceDialog(): DialogWrapper

  /**
   * Produces the dialog via [produceDialog] and shows it modally.
   *
   * @return `true` if the user confirmed the dialog (clicked OK), `false` otherwise.
   */
  fun showAndGet(): Boolean {
    return produceDialog().showAndGet()
  }
}