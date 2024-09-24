/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */

package org.zowe.explorer.explorer.ui

import com.intellij.openapi.project.Project
import org.zowe.explorer.config.connect.ConnectionConfigBase
import org.zowe.explorer.dataops.RemoteQuery
import org.zowe.explorer.explorer.ExplorerUnit
import org.zowe.explorer.vfs.MFVirtualFile

abstract class RemoteMFFileFetchNode<Connection: ConnectionConfigBase, Value : Any, R : Any, U : ExplorerUnit<Connection>>(
  value: Value,
  project: Project,
  parent: ExplorerTreeNode<Connection, *>,
  unit: U,
  treeStructure: ExplorerTreeStructureBase
) : FileFetchNode<Connection, Value, R, RemoteQuery<Connection, R, Unit>, MFVirtualFile, U>(value, project, parent, unit, treeStructure){

  override val queryClass = RemoteQuery::class.java

  override val vFileClass = MFVirtualFile::class.java

}
