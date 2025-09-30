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

package org.zowe.explorer.v3.components.files

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import org.zowe.explorer.common.ui.StatefulComponent
import org.zowe.explorer.utils.*
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import javax.swing.JComponent

// TODO: doc
class RenameUssEntityDialog(
  project: Project,
  ussNode: ExplorerTreeNode,
  override var state: String
) : DialogWrapper(project), StatefulComponent<String> {
  override fun createCenterPanel(): JComponent {
    return panel {
      row {
        label("New name: ")
        textField()
          .bindText(this@RenameUssEntityDialog::state)
//          .validationOnApply { validateForBlank(it) ?: validateOnInput(it) }
          .validationOnApply { validateForBlank(it) }
          .focused()
      }
    }
  }

  init {
    title = when (ussNode) {
      is UssFilterNode -> "Change Filter"
      is UssFileNode -> "Rename File"
      is UssFolderNode -> "Rename Folder"
      else -> throw Exception("Unknow entity: $ussNode")
    }
    init()
  }

  // TODO: refactor, finalize
//  /**
//   * Validate a new name for the selected node component
//   */
//  private fun validateOnInput(component: JTextField): ValidationInfo? {
//    validateForTheSameValue(attributes?.name, component)?.let { return it }
//    when (node) {
//      is UssDirNode, is UssFileNode -> {
//        return validateUssFileName(component) ?: validateUssFileNameAlreadyExists(component, selectedNodeData)
//      }
//    }
//    return null
//  }
}
