package eu.ibagroup.formainframe.dataops.attributes

import com.intellij.openapi.util.io.FileAttributes
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.formainframe.vfs.MFVirtualFileSystem
import eu.ibagroup.formainframe.vfs.createAttributes

private fun String.trimUrl(): String {
  val lastSlashIdx = indexOfLast { it == '/' }
  return if (lastSlashIdx != -1) {
    substring(lastSlashIdx + 1)
  } else this
}

abstract class MFRemoteAttributesServiceBase<Attributes : MFRemoteFileAttributes<*>> :
  FsStructuringAttributesServiceBase<Attributes, MFVirtualFile>() {

  protected companion object {
    val fs = MFVirtualFileSystem.instance
    val fsModel = fs.model
    val fsRoot = fs.root
  }

  override val vFileClass = MFVirtualFile::class.java

  protected fun createPathChain(attributes: Attributes): List<Pair<String, FileAttributes>> {
    return listOf(
      Pair(attributes.url.trimUrl(), createAttributes(true)),
      Pair(subFolderName, createAttributes(directory = true))
    ).plus(continuePathChain(attributes))
  }

  override fun reassignAttributesToFile(file: MFVirtualFile, oldAttributes: Attributes, newAttributes: Attributes) {
    val urlDir = obtainAndRenameUrlDirIfNeeded(file, oldAttributes, newAttributes)
    reassignAttributesAfterUrlFolderRenaming(file, urlDir, oldAttributes, newAttributes)
  }

  protected abstract fun reassignAttributesAfterUrlFolderRenaming(
    file: MFVirtualFile,
    urlFolder: MFVirtualFile,
    oldAttributes: Attributes,
    newAttributes: Attributes
  )

  private fun obtainAndRenameUrlDirIfNeeded(
    file: MFVirtualFile,
    oldAttributes: Attributes,
    newAttributes: Attributes
  ): MFVirtualFile {
    return findOrCreate(fsRoot, Pair(newAttributes.url.trimUrl(), createAttributes(directory = true)))
  }

  protected lateinit var subDirectory: MFVirtualFile

  protected abstract fun continuePathChain(attributes: Attributes): List<Pair<String, FileAttributes>>

  override fun findOrCreateFileInternal(attributes: Attributes): MFVirtualFile {
    var current = fsRoot
    createPathChain(attributes).map { nameWithFileAttr ->
      findOrCreate(current, nameWithFileAttr).also { current = it }
    }[1].also { if (!this::subDirectory.isInitialized) subDirectory = it }
    return current
  }

  protected fun findOrCreate(current: MFVirtualFile, nameWithFileAttr: Pair<String, FileAttributes>): MFVirtualFile {
    return fsModel.findOrCreate(this, current, nameWithFileAttr.first, nameWithFileAttr.second)
  }

}