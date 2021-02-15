package eu.ibagroup.formainframe.config

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.TabbedConfigurable
import eu.ibagroup.formainframe.config.connect.ui.ConnectionConfigurable
import eu.ibagroup.formainframe.config.ws.ui.WSConfigurable

class MainframeConfigurable : TabbedConfigurable() {

  override fun getDisplayName(): String {
    return "For Mainframe"
  }

  private lateinit var connectionConfigurable: ConnectionConfigurable
  private lateinit var wsConfigurable: WSConfigurable

  override fun createConfigurables(): MutableList<Configurable> {
    return mutableListOf(
      ConnectionConfigurable().also { connectionConfigurable = it },
      WSConfigurable {
        connectionConfigurable.openAddDialog = true
      }.also { wsConfigurable = it }
    )
  }

  override fun reset() {
    ConfigSandbox.instance.fetch()
    super.reset()
  }

  override fun cancel() {
    configurables.forEach { it.cancel() }
  }

}