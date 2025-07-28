/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.v3.actions.filter.ds

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import org.zowe.explorer.explorer.ui.LibraryNode
import org.zowe.explorer.utils.validateMemberPattern
import javax.swing.JComponent

/**
 * Dialog to specify or edit a filter for [LibraryNode] to filter members in a dataset
 * @param project the project to show the dialog in
 * @param parentNode the [LibraryNode] to specify or edit the filter in
 */
class FilterChildrenDialog(
  project: Project?,
  private var parentNode: LibraryNode
) : DialogWrapper(project) {
  private var newFilter = parentNode.savedFilter

  init {
    title = "Filter Children Elements"
    init()
  }

  /** Create a dialog to specify a new filter for a [LibraryNode] */
  override fun createCenterPanel(): JComponent? {
    return panel {
      row {
        label("New filter: ")
        textField()
          .bindText(::newFilter)
          .validationOnApply {
            if (it.text.isBlank() || it.text.isEmpty()) null else validateMemberPattern(it)
          }
          .focused()
          .applyToComponent { selectAll() }
      }
    }
  }

  /**
   * Wait for the user to finish the input.
   * If the filter is specified and applied, returns the [newFilter],
   * otherwise - the saved filter for the node is returned
   */
  fun waitForUserInput(): String {
    return if (showAndGet()) newFilter.uppercase() else parentNode.savedFilter
  }
}