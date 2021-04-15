package eu.ibagroup.formainframe.dataops.synchronizer

import com.intellij.openapi.editor.toolbar.floating.AbstractFloatingToolbarProvider

private const val ACTION_GROUP = "eu.ibagroup.formainframe.dataops.synchronizer.SyncActionGroup"

class SyncToolbarProvider : AbstractFloatingToolbarProvider(ACTION_GROUP) {
  override val autoHideable = true
  override val priority = 1
}