package eu.ibagroup.formainframe.config

import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.Credentials
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.JesWorkingSetConfig

/** Class to handle an action according to the class that is provided with the "invoke" operator */
internal abstract class ConfigClassActionDecider<R> {

  /** Handle action when the ConnectionConfig class is provided */
  abstract fun onConnectionConfig(): R

  /** Handle action when the FilesWorkingSetConfig class is provided */
  abstract fun onFilesWorkingSetConfig(): R

  /** Handle action when the JesWorkingSetConfig class is provided */
  abstract fun onJesWorkingSetConfig(): R

  /** Handle action when the Credentials class is provided */
  open fun onCredentials(): R {
    return onElse()
  }

  /** Handle action when some different class is provided */
  abstract fun onElse(): R

  /**
   * Handle action according to the class provided when the decider is invoked
   * @param clazz the class to invoke the handler by
   */
  operator fun invoke(clazz: Class<*>): R {
    return when (clazz) {
      ConnectionConfig::class.java -> onConnectionConfig()
      FilesWorkingSetConfig::class.java -> onFilesWorkingSetConfig()
      JesWorkingSetConfig::class.java -> onJesWorkingSetConfig()
      Credentials::class.java -> onCredentials()
      else -> onElse()
    }
  }

}