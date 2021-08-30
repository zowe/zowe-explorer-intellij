package eu.ibagroup.formainframe.dataops.synchronizer

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.progress.DumbProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.r2z.CodePage
import eu.ibagroup.r2z.XIBMDataType
import eu.ibagroup.r2z.annotations.ZVersion

enum class AcceptancePolicy {
  IF_EMPTY_ONLY,
  FORCE_REWRITE
}

interface ContentSynchronizer {

  companion object {
    @JvmField
    val EP = ExtensionPointName.create<ContentSynchronizerFactory>("eu.ibagroup.formainframe.contentSynchronizer")
  }

  fun accepts(file: VirtualFile): Boolean

  fun isAlreadySynced(file: VirtualFile): Boolean

  fun startSync(
    file: VirtualFile,
    project: Project,
    acceptancePolicy: AcceptancePolicy,
    saveStrategy: SaveStrategy,
    removeSyncOnThrowable: (file: VirtualFile, t: Throwable) -> Boolean,
    progressIndicator: ProgressIndicator = DumbProgressIndicator.INSTANCE
  )

  fun triggerSync(file: VirtualFile)

  fun startSyncIfNeeded(
    file: VirtualFile,
    project: Project,
    acceptancePolicy: AcceptancePolicy,
    saveStrategy: SaveStrategy,
    removeSyncOnThrowable: (file: VirtualFile, t: Throwable) -> Boolean,
    progressIndicator: ProgressIndicator = DumbProgressIndicator.INSTANCE
  ) {
    if (!isAlreadySynced(file)) {
      startSync(file, project, acceptancePolicy, saveStrategy, removeSyncOnThrowable, progressIndicator)
    }
  }

  fun removeSync(file: VirtualFile)

}

fun updateDataTypeWithEncoding(connectionConfig: ConnectionConfig, oldDataType: XIBMDataType) : XIBMDataType {
  return if (connectionConfig.zVersion == ZVersion.ZOS_2_4 && oldDataType.encoding != null && oldDataType.encoding != CodePage.IBM_1047 && oldDataType.type == XIBMDataType.Type.TEXT) {
    XIBMDataType(oldDataType.type, connectionConfig.codePage)
  } else {
    oldDataType
  }
}