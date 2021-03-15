package eu.ibagroup.formainframe.dataops.attributes

import com.intellij.openapi.util.io.FileAttributes
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.formainframe.vfs.MFVirtualFileSystem
import eu.ibagroup.formainframe.vfs.createAttributes

private fun String.trimUrl(): String {
  val lastSlashIdx = indexOfLast { it == '/' }
  return if (lastSlashIdx != -1) {
    substring(lastSlashIdx + 1)
  } else this
}

abstract class MFRemoteAttributesServiceBase<Attributes : MFRemoteFileAttributes<*>>(
  dataOpsManager: DataOpsManager
) : FsStructuringAttributesServiceBase<Attributes, MFVirtualFile>(dataOpsManager) {

  protected companion object {
    val fs = MFVirtualFileSystem.instance
    val fsModel = fs.model
    val fsRoot = fs.root
  }

  override val vFileClass = MFVirtualFile::class.java

  protected fun createPathChain(attributes: Attributes): List<PathElementSeed> {
    return listOf(
      PathElementSeed(name = attributes.url.trimUrl(), fileAttributes = createAttributes(true)),
      PathElementSeed(name = subFolderName, fileAttributes = createAttributes(true))
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
    return findOrCreate(
      fsRoot, PathElementSeed(
        name = newAttributes.url.trimUrl(),
        fileAttributes = createAttributes(directory = true)
      )
    )
  }

  protected lateinit var subDirectory: MFVirtualFile

  protected abstract fun continuePathChain(attributes: Attributes): List<PathElementSeed>

  override fun findOrCreateFileInternal(attributes: Attributes): MFVirtualFile {
    var current = fsRoot
    createPathChain(attributes).map { seed ->
      findOrCreate(current, seed).also { current = it }
    }[1].also { if (!this::subDirectory.isInitialized) subDirectory = it }
    return current
  }

  protected fun findOrCreate(current: MFVirtualFile, seed: PathElementSeed): MFVirtualFile {
    return fsModel.findOrCreate(this, current, seed.name, seed.fileAttributes).apply(seed.postCreateAction)
  }

}

data class PathElementSeed(
  val name: String,
  val fileAttributes: FileAttributes,
  val postCreateAction: MFVirtualFile.() -> Unit = {}
)