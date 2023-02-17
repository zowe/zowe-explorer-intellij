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
import eu.ibagroup.formainframe.explorer.FilesWorkingSet
import eu.ibagroup.formainframe.explorer.actions.DuplicateMemberAction
import eu.ibagroup.formainframe.explorer.actions.RenameAction
import eu.ibagroup.formainframe.utils.*
import javax.swing.JComponent
import javax.swing.JTextField

/**
 * Base class to create an instance of rename dialog object
 * @param type represents a virtual file type - file or directory
 * @param selectedNode represents the current node object
 * @param currentAction represents the current action to be performed - rename or force rename
 * @param state represents the current state
 */
class RenameDialog(project: Project?,
                   type: String,
                   private val selectedNode: NodeData<*>,
                   private val currentAction: AnAction,
                   override var state: String
) : DialogWrapper(project),
  StatefulComponent<String> {

  private val node = selectedNode.node

  /**
   * Creates UI component of the object
   */
  override fun createCenterPanel(): JComponent {
    return panel {
      row {
        label("New name: ")
        textField()
          .bindText(this@RenameDialog::state)
          .validationOnApply { validateOnBlank(it) ?: validateOnInput(it) }
          .apply { focused() }
      }
    }
  }

  /**
   * Initialization of the object. It's called first
   */
  init {
    title = if (currentAction is DuplicateMemberAction) "Duplicate $type" else "Rename $type"
    init()
  }

  /**
   * Sets validation rules for text field
   */
  private fun validateOnInput(component: JTextField): ValidationInfo? {
    val attributes = selectedNode.attributes
    when (node) {
      is DSMaskNode -> {
        return validateDatasetMask(component.text, component) ?: validateWorkingSetMaskName(
          component,
          node.parent?.value as FilesWorkingSet
        )
      }
      is LibraryNode, is FileLikeDatasetNode -> {
        return if (attributes is RemoteDatasetAttributes) {
          validateDatasetNameOnInput(component)
        } else {
          validateMemberName(component)
        }
      }
      is UssDirNode -> {
        return if (node.isConfigUssPath) {
          validateUssMask(component.text, component) ?: validateWorkingSetMaskName(
            component,
            node.parent?.value as FilesWorkingSet
          )
        } else {
          if (currentAction is RenameAction) {
            validateUssFileName(component) ?: validateUssFileNameAlreadyExists(component, selectedNode)
          } else {
            validateUssFileName(component)
          }
        }
      }
      is UssFileNode -> {
        return if (currentAction is RenameAction) {
          validateUssFileName(component) ?: validateUssFileNameAlreadyExists(component, selectedNode)
        } else {
          validateUssFileName(component)
        }
      }
    }
    return null
  }

  /**
   * Sets validation rule on blank for text field
   */
  private fun validateOnBlank(component: JTextField): ValidationInfo? {
    return validateForBlank(component)
  }

}
