package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.project.Project
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.explorer.ExplorerUnit
import eu.ibagroup.formainframe.explorer.ExplorerViewSettings
import eu.ibagroup.formainframe.vfs.MFVirtualFile

abstract class RemoteMFFileCacheNode<Value : Any, R : Any, U : ExplorerUnit>(
  value: Value,
  project: Project,
  parent: ExplorerTreeNodeBase<*>,
  unit: U,
  explorerViewSettings: ExplorerViewSettings
) : FileCacheNode<Value, R, RemoteQuery<R>, MFVirtualFile, U>(value, project, parent, unit, explorerViewSettings){

  override val queryClass = RemoteQuery::class.java

  override val vFileClass = MFVirtualFile::class.java

}