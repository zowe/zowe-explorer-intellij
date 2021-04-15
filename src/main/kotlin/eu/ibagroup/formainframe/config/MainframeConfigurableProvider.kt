package eu.ibagroup.formainframe.config

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider

class MainframeConfigurableProvider : ConfigurableProvider() {
  override fun createConfigurable(): Configurable {
    return MainframeConfigurable()
  }
}