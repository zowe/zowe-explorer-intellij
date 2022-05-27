package org.zowe.explorer.config

import com.intellij.configurationStore.getPersistentStateComponentStorageLocation
import com.intellij.conversion.*
import com.intellij.openapi.components.service
import java.io.File
import java.nio.file.Files
import kotlin.io.path.pathString

class ZoweConfigConverterProvider : ConverterProvider() {

  class ConverterImpl(private val context: ConversionContext) : ProjectConverter() {

    private val newConfigStorageName = "org.zowe.explorer.config.ConfigService"
    private val oldConfigStorageName = "by.iba.connector.services.ConfigService"
    private val oldConfigName = "iba_connector_config.xml"
    private lateinit var newConfigFile: File
    private lateinit var oldConfigFile: File

    override fun createRunConfigurationsConverter(): ConversionProcessor<RunManagerSettings> {

      return object : ConversionProcessor<RunManagerSettings>() {

        override fun isConversionNeeded(settings: RunManagerSettings?): Boolean {
          val newConfigStorage = getPersistentStateComponentStorageLocation(service<ConfigService>().javaClass)
          val newConfigPath = newConfigStorage?.pathString
          val newConfigPathSplitted = newConfigPath?.split("/")
          val oldConfigPath =
            newConfigPathSplitted
              ?.dropLast(1)
              ?.plus(oldConfigName)
              ?.joinToString("/")
          oldConfigFile = File(oldConfigPath)
          newConfigFile = File(newConfigPath)

          return !newConfigFile.exists() && oldConfigFile.exists()
        }

        override fun process(settings: RunManagerSettings?) {
          runCatching {
            val charset = Charsets.UTF_8
            val newContent =
              oldConfigFile
                .readText(charset)
                .replace(oldConfigStorageName.toRegex(), newConfigStorageName)
            newConfigFile.createNewFile()
            Files.write(newConfigFile.toPath(), newContent.toByteArray(charset))
          }
        }

      }
    }

  }

  override fun getConversionDescription(): String = "The plugin is going to migrate the config to the new version, ok?"

  override fun createConverter(context: ConversionContext) = ConverterImpl(context)

}
