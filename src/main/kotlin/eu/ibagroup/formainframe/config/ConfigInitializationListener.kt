package eu.ibagroup.formainframe.config

import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.config.ConfigInitializationListener

interface ConfigInitializationListener {
  fun onConfigLoaded()

  companion object {
    @JvmField val CONFIGS_LOADED = Topic.create("configsLoaded", ConfigInitializationListener::class.java)
  }
}