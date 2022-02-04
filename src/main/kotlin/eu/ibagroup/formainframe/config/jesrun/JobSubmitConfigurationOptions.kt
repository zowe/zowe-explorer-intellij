package eu.ibagroup.formainframe.config.jesrun

import com.intellij.execution.configurations.RunConfigurationOptions
import eu.ibagroup.formainframe.utils.MfFileType

class JobSubmitConfigurationOptions: RunConfigurationOptions() {

  val jobSubmitFileType = enum(MfFileType.MEMBER).provideDelegate(this, "file type")
  val jobSubmitFilePath = string("").provideDelegate(this, "file path")
  val jobSubmitMemberName = string("").provideDelegate(this, "member name")
  val connectionConfigId = string("").provideDelegate(this, "connection id")

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
    jobSubmitMemberName.setValue(this, newConnectionConfigId)
  }
}
