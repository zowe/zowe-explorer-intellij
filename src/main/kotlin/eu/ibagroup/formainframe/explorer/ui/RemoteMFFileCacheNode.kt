package eu.ibagroup.formainframe.explorer.ui

import eu.ibagroup.formainframe.dataops.fetch.RemoteQuery
import eu.ibagroup.formainframe.explorer.ExplorerUnit
import eu.ibagroup.formainframe.explorer.ExplorerViewSettings
import eu.ibagroup.formainframe.vfs.MFVirtualFile

abstract class RemoteMFFileCacheNode<Value : Any, R : Any, U : ExplorerUnit>(
  value: Value,
  unit: U,
  explorerViewSettings: ExplorerViewSettings
) : FileCacheNode<Value, R, RemoteQuery<R>, MFVirtualFile, U>(value, unit, explorerViewSettings){

  override val queryClass = RemoteQuery::class.java

  override val vFileClass = MFVirtualFile::class.java

}