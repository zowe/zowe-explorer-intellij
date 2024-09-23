/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */

package eu.ibagroup.formainframe.common.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.ui.laf.darcula.DarculaUIUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.ValidationInfo
import eu.ibagroup.formainframe.utils.nullable
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.text.JTextComponent

/**
 * Gets an UI icon according to what validation info returned
 */
fun ValidationInfo?.getIcon(): Icon? {
  return this?.let {
    if (it.warning) {
      AllIcons.General.Warning
    } else {
      AllIcons.General.Error
    }
  }
}

/**
 * Class for building validator for specified Jcomponent
 * @param component - component for which validator is being built
 * @param disposable - disposable for component
 */
class ComponentValidatorBuilder(
  private val component: JComponent,
  disposable: Disposable
) {

  private val nullableComponentValidator = ComponentValidator.getInstance(component).nullable
  private val componentValidatorWasNull = nullableComponentValidator == null
  private val componentValidator = nullableComponentValidator ?: ComponentValidator(disposable)

  init {
    if (componentValidatorWasNull) {
      component.putClientProperty(DarculaUIUtil.COMPACT_PROPERTY, true)
    }
  }

  /**
   * Method to setup validator for component
   * @param block - validation function to be performed
   * @return this component validator builder
   */
  fun setup(block: ComponentValidator.() -> Unit): ComponentValidatorBuilder {
    componentValidator.block()
    return this
  }

  /**
   * Method to stop validator on component
   * @param block - function block
   * @return an instance of component validator
   */
  fun finish(block: ComponentValidator.() -> Unit): ComponentValidator {
    if (componentValidatorWasNull) {
      componentValidator.block()
      if (component is JTextComponent) {
        componentValidator.andRegisterOnDocumentListener(component)
      }
      componentValidator.installOn(component)
    }
    return componentValidator
  }

}
