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

fun ValidationInfo?.getIcon(): Icon? {
  return this?.let {
    if (it.warning) {
      AllIcons.General.Warning
    } else {
      AllIcons.General.Error
    }
  }
}

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

  fun setup(block: ComponentValidator.() -> Unit): ComponentValidatorBuilder {
    componentValidator.block()
    return this
  }

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