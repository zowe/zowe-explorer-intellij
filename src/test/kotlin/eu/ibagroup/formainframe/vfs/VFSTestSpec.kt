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

package eu.ibagroup.formainframe.vfs

import io.kotest.core.spec.style.ShouldSpec

class VFSTestSpec : ShouldSpec({
  context("vfs module: MFVirtualFileSystemModel") {
    // findFileByPath(path: String)
    should("find file by path") {}
    should("find directory by path") {}
    should("check invalid path") {}
    // refreshAndFindFileByPath(path: String)
    should("find file by path without refresh") {}
    should("find file by path with refresh") {}
    // deleteFile(requestor: Any?, vFile: MFVirtualFile)
    should("delete plain file") {}
    should("delete directory with files inside") {}
    should("throw exception during delete process") {}
    // moveFile(requestor: Any?, vFile: MFVirtualFile, newParent: MFVirtualFile)
    should("move file without replace") {}
    // moveFileAndReplace(requestor: Any?, vFile: MFVirtualFile, newParent: MFVirtualFile)
    should("move file with replace") {}
    should("throw exception during move or copy process") {}
    // renameFile(requestor: Any?, vFile: MFVirtualFile, newName: String)
    should("rename virtual file") {}
    // createChildDirectory(requestor: Any?, vDir: MFVirtualFile, dirName: String)
    should("create child directory") {}
    // createChildFile(requestor: Any?, vDir: MFVirtualFile, fileName: String)
    should("create child file") {}
    should("throw exception during create child process as it tries to create the child not in directory") {}
    should("throw exception during create child process as it tries to create the child that already exists") {}
    // exists(file: MFVirtualFile)
    should("check that provided file exists") {}
    should("check that provided file not exists") {}
    // setWritable(file: MFVirtualFile, writableFlag: Boolean)
    should("set file writable") {}
    should("not change file writable flag") {}
    // copyFile(requestor: Any?, virtualFile: MFVirtualFile, newParent: MFVirtualFile, copyName: String)
    should("copy file without replace") {}
    // copyFileAndReplace
    should("copy file with replace") {}
    // contentsToByteArray(file: MFVirtualFile)
    should("convert file contents to byte array") {}
    should("convert file contents to byte array with awaiting for the file contents") {}
    // getIdForStorageAccess(file: MFVirtualFile)
    should("get file storage ID") {}
    should("throw an exception when tries to get storage ID for a directory") {}
    should("throw an exception when tries to get storage ID for a file but couldn't get symlink") {}
    // putInitialContentIfPossible(file: MFVirtualFile, content: ByteArray)
    should("put initial content in a virtual file") {}
    should("not put initial content in a virtual file when there is no initial content") {}
    // getOutputStream
    should("get output stream and then closes it with file changes") {}
    // getLength
    should("get file content bytes length") {}
    should("fail and get 0 bytes length") {}
    // isFileValid
    should("check that file is valid") {}
    should("check that file is not valid") {}
    should("check that file is not valid when disposed") {}
    // getParent
    should("get file parent") {}
    should("not get file parent for a root file") {}
    // getChildrenList
    should("get valid children list") {}
  }
  context("vfs module: MFVirtualFile") {
    // getPath
    should("get file path") {}
    should("not get invalid file path") {}
    // getCanonicalFile
    should("get canonical file from a symlink") {}
    should("return null when tries to get canonical file from not a symlink") {}
    // getChildren
    should("get children of a directory") {}
    // findChild
    should("find child by its name in a directory") {}
    // getExtension
    should("get file extension") {}
    should("get file name when file has no extension") {}
    // getFileType
    should("get file type as plain text when file extension matches file name") {}
    should("get file type as plain text when file type is unknown") {}
    should("get file type provided by superclass") {}
    // getNameWithoutExtension
    should("get file name without extension") {}
    should("get file name without extension when there is no extension") {}
    // equals
    should("check whether two files are equal") {}
    should("check whether two files are not equal") {}
  }
})
