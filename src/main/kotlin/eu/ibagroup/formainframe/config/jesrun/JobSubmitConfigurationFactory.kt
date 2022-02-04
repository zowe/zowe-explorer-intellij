package eu.ibagroup.formainframe.config.jesrun

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project

class JobSubmitConfigurationFactory(type: ConfigurationType): ConfigurationFactory(type) {
  override fun createTemplateConfiguration(project: Project): RunConfiguration {
    return JobSubmitConfiguration(project, this, "Job submit configuration")
  }

  override fun getId() = JobSubmitConfigurationType.ID

  override fun getOptionsClass() = JobSubmitConfigurationOptions::class.java

  override fun isEditableInDumbMode(): Boolean {
    return true
  }
}
