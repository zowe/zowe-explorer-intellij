package eu.ibagroup.formainframe.config.jesrun

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.icons.AllIcons

class JobSubmitConfigurationType: ConfigurationType {

  companion object {
    val ID = "JobSubmitConfiguration"
  }

  override fun getDisplayName() = "Job Submission"

  override fun getConfigurationTypeDescription() = "Configuration type for submitting jobs"

  override fun getIcon() = AllIcons.Actions.Run_anything

  override fun getId() = ID

  override fun getConfigurationFactories(): Array<ConfigurationFactory> {
    return arrayOf(JobSubmitConfigurationFactory(this))
  }
}
