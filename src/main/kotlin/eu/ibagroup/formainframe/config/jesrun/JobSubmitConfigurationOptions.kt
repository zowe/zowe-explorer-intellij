package eu.ibagroup.formainframe.config.jesrun

import com.intellij.execution.configurations.RunConfigurationOptions
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Property
import eu.ibagroup.formainframe.utils.MfFileType

class JobSubmitConfigurationOptions: RunConfigurationOptions() {

  @get:Property(surroundWithTag = false)
  val jobSubmitFileType = enum(MfFileType.MEMBER).provideDelegate(this, "jobSubmitFileType")
  @get:Property(surroundWithTag = false)
  val jobSubmitFilePath = string("").provideDelegate(this, "jobSubmitFilePath")
  @get:Property(surroundWithTag = false)
  val jobSubmitMemberName = string("").provideDelegate(this, "jobSubmitMemberName")
  @get:Property(surroundWithTag = false)
  val connectionConfigId = string("").provideDelegate(this, "connectionConfigId")

  fun getJobSubmitFileType() = jobSubmitFileType.getValue(this)
  fun setJobSubmitFileType(newFileType: MfFileType) {
    jobSubmitFileType.setValue(this, newFileType)
  }

  fun getJobSubmitFilePath(): String = jobSubmitFilePath.getValue(this) ?: ""
  fun setJobSubmitFilePath(newFilePath: String) {
    jobSubmitFilePath.setValue(this, newFilePath)
  }

  fun getJobSubmitMemberName() = jobSubmitMemberName.getValue(this)
  fun setJobSubmitMemberName(newMemberName: String?) {
    jobSubmitMemberName.setValue(this, newMemberName)
  }


  fun getConnectionConfigId() = connectionConfigId.getValue(this)
  fun setConnectionConfigId(newConnectionConfigId: String) {
    connectionConfigId.setValue(this, newConnectionConfigId)
  }
}
