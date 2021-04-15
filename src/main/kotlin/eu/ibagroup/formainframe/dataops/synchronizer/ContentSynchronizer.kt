package eu.ibagroup.formainframe.dataops.synchronizer

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.progress.DumbProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

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