/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.explorer.ui

import com.intellij.openapi.project.Project
import org.zowe.explorer.dataops.RemoteQuery
import org.zowe.explorer.explorer.ExplorerUnit
import org.zowe.explorer.vfs.MFVirtualFile

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
