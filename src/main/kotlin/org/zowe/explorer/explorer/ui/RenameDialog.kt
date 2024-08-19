/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.explorer.ui

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import org.zowe.explorer.common.ui.StatefulComponent
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.explorer.actions.DuplicateMemberAction
import org.zowe.explorer.explorer.actions.RenameAction
import org.zowe.explorer.utils.*
import javax.swing.JComponent
import javax.swing.JTextField

/**
 * Base class to create an instance of rename dialog object
 * @param type represents a virtual file type - file or directory
 * @param selectedNodeData represents the current node object
 * @param currentAction represents the current action to be performed - rename or force rename
 * @param state represents the current state
 */
class RenameDialog(
  project: Project?,
  type: String,
  private val selectedNodeData: NodeData<*>,
  private val currentAction: AnAction,
  override var state: String
) : DialogWrapper(project),
  StatefulComponent<String> {

  companion object {

    // TODO: Remove when it becomes possible to mock class constructor with init section.
    /** Wrapper for init() method. It is necessary only for test purposes for now. */
    private fun initialize(init: () -> Unit) {
      init()
    }
  }

  private val node = selectedNodeData.node

  /**
   * Creates UI component of the object
   */
  override fun createCenterPanel(): JComponent {
    return panel {
      row {
        label("New name: ")
        textField()
          .bindText(this@RenameDialog::state)
          .validationOnApply { validateForBlank(it) ?: validateOnInput(it) }
          .apply { focused() }
      }
    }
  }

  /**
   * Initialization of the object. It's called first
   */
  init {
    title = if (currentAction is DuplicateMemberAction) "Duplicate $type" else "Rename $type"
    initialize { init() }
  }

  /**
   * Validate a new name for the selected node component
   */
  private fun validateOnInput(component: JTextField): ValidationInfo? {
    val attributes = selectedNodeData.attributes

    validateForTheSameValue(attributes?.name, component)?.let { return it }

    when (node) {
      is LibraryNode, is FileLikeDatasetNode -> {
        return if (attributes is RemoteDatasetAttributes) {
          validateDatasetNameOnInput(component)
        } else {
          validateMemberName(component)
        }
      }

      is UssDirNode, is UssFileNode -> {
        return validateUssFileName(component) ?: validateUssFileNameAlreadyExists(component, selectedNodeData)
      }
    }
    return null
  }

}
