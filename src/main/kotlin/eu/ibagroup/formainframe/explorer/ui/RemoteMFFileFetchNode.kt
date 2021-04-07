package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.project.Project
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.explorer.ExplorerUnit
import eu.ibagroup.formainframe.vfs.MFVirtualFile

abstract class RemoteMFFileFetchNode<Value : Any, R : Any, U : ExplorerUnit>(
  value: Value,
  project: Project,
  parent: ExplorerTreeNode<*>,
  unit: U,
  treeStructure: ExplorerTreeStructureBase
) : FileFetchNode<Value, R, RemoteQuery<R, Unit>, MFVirtualFile, U>(value, project, parent, unit, treeStructure){

  override val queryClass = RemoteQuery::class.java

  override val vFileClass = MFVirtualFile::class.java

}