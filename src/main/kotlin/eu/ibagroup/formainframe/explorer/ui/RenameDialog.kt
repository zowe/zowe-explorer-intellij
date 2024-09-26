/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import eu.ibagroup.formainframe.common.ui.StatefulComponent
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.explorer.actions.DuplicateMemberAction
import eu.ibagroup.formainframe.explorer.actions.RenameAction
import eu.ibagroup.formainframe.utils.*
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
          .onApply { state = state.uppercaseIfNeeded() }
          .focused()
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
    component.text = component.text.uppercaseIfNeeded()

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
        return if (currentAction is RenameAction) {
          validateUssFileName(component) ?: validateUssFileNameAlreadyExists(component, selectedNodeData)
        } else {
          validateUssFileName(component)
        }
      }
    }
    return null
  }

  /**
   * Convert the string to upper case if partitioned dataset, sequential dataset or dataset member is selected
   */
  private fun String.uppercaseIfNeeded(): String {
    return if (node is LibraryNode || node is FileLikeDatasetNode) {
      this.uppercase()
    } else {
      this
    }
  }

}
