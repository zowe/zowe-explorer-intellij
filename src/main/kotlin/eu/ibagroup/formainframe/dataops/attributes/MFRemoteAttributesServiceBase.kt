/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.dataops.attributes

import com.intellij.openapi.util.io.FileAttributes
import eu.ibagroup.formainframe.config.connect.ConnectionConfigBase
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.utils.runWriteActionInEdtAndWait
import eu.ibagroup.formainframe.utils.sendTopic
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.formainframe.vfs.MFVirtualFileSystem
import eu.ibagroup.formainframe.vfs.createAttributes
import java.io.IOException
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/** Trim the URL to get the element name after the last slash */
private fun String.trimUrl(): String {
  val lastSlashIdx = indexOfLast { it == '/' }
  return if (lastSlashIdx != -1) {
    substring(lastSlashIdx + 1)
  } else this
}

/**
 * Base abstract service class to handle attributes on virtual file
 * @param dataOpsManager data ops manager to get component manager
 */
abstract class MFRemoteAttributesServiceBase<Connection: ConnectionConfigBase, Attributes : MFRemoteFileAttributes<Connection, *>>(
  val dataOpsManager: DataOpsManager
) : AttributesService<Attributes, MFVirtualFile> {

  private val lock = ReentrantReadWriteLock()
  private val fileToAttributesMap = HashMap<MFVirtualFile, Attributes>()
  private val attributesToFileMap = HashMap<Attributes, MFVirtualFile>()

  override val vFileClass = MFVirtualFile::class.java

  abstract val subFolderName: String

  protected lateinit var subDirectory: MFVirtualFile

  protected companion object {
    val fs = MFVirtualFileSystem.instance
    val fsModel = fs.model
    val fsRoot = fs.root
  }

  /**
   * Find or create the virtual file by path element seed
   * @param current the current directory to search the file in
   * @param seed the path element seed to search for the file by
   */
  protected fun findOrCreate(current: MFVirtualFile, seed: PathElementSeed): MFVirtualFile {
    return fsModel.findOrCreate(this, current, seed.name, seed.fileAttributes).apply(seed.postCreateAction)
  }

  /**
   * Obtain or rename a URL directory creating the new path element if it is not in the file system
   * @param newAttributes the new file attributes to find or create the directory by
   */
  private fun obtainAndRenameUrlDirIfNeeded(newAttributes: Attributes): MFVirtualFile {
    return findOrCreate(
      fsRoot,
      PathElementSeed(
        name = newAttributes.url.trimUrl(),
        fileAttributes = createAttributes(directory = true)
      )
    )
  }

  /**
   * Reassign the attributes of the file after a URL folder renaming
   * @param file the file to reassign the attributes
   * @param oldAttributes the old attributes to compare with the new attributes
   * @param newAttributes the new attributes to compare with the old ones and rename if they are changed
   */
  protected abstract fun reassignAttributesAfterUrlFolderRenaming(
    file: MFVirtualFile,
    oldAttributes: Attributes,
    newAttributes: Attributes
  )

  /**
   * Assign the new attributes to the file. It obtains and renames a URL directory if needed and reassigns the attributes after the URL directory obtained
   * @param file the file to reassign attributes to
   * @param oldAttributes the old attributes for some additional comparisons
   * @param newAttributes the new attributes to assign to the file
   */
  private fun reassignAttributesToFile(file: MFVirtualFile, oldAttributes: Attributes, newAttributes: Attributes) {
    obtainAndRenameUrlDirIfNeeded(newAttributes)
    runWriteActionInEdtAndWait {
      reassignAttributesAfterUrlFolderRenaming(file, oldAttributes, newAttributes)
    }
  }

  protected abstract fun buildUniqueAttributes(attributes: Attributes): Attributes

  /**
   * Get virtual file by the attributes
   * @param attributes the attributes to search for a virtual file by
   */
  override fun getVirtualFile(attributes: Attributes): MFVirtualFile? {
    return lock.read { attributesToFileMap[buildUniqueAttributes(attributes)] }
  }

  /**
   * Get attributes by the virtual file
   * @param file the virtual file to search for attributes by
   */
  override fun getAttributes(file: MFVirtualFile): Attributes? {
    return lock.read { fileToAttributesMap[file] }
  }

  /**
   * Update attributes internal function to directly update the file attributes. Uses old attributes for some additional comparisons
   * @param file the file to update attributes for
   * @param oldAttributes the old attributes for additional comparison
   * @param newAttributes the new attributes to assign to the file
   */
  private fun updateAttributesInternal(
    file: MFVirtualFile,
    oldAttributes: Attributes,
    newAttributes: Attributes
  ) {
    lock.write {
      attributesToFileMap.remove(buildUniqueAttributes(oldAttributes))
      reassignAttributesToFile(file, oldAttributes, newAttributes)
      fileToAttributesMap[file] = newAttributes
      attributesToFileMap[buildUniqueAttributes(newAttributes)] = file
      sendTopic(AttributesService.ATTRIBUTES_CHANGED, dataOpsManager.componentManager)
        .onUpdate(oldAttributes, newAttributes, file)
    }
  }

  @Throws(IOException::class)
  protected abstract fun mergeAttributes(oldAttributes: Attributes, newAttributes: Attributes): Attributes

  protected abstract fun continuePathChain(attributes: Attributes): List<PathElementSeed>

  /**
   * Create a path chain from the provided file or folder attributes. Uses the sub folder name to describe, for which system the path chain is being created
   * @param attributes the attributes to build the path chain from
   */
  protected fun createPathChain(attributes: Attributes): List<PathElementSeed> {
    return listOf(
      PathElementSeed(name = attributes.url.trimUrl(), fileAttributes = createAttributes(true)),
      PathElementSeed(name = subFolderName, fileAttributes = createAttributes(true))
    )
      .plus(continuePathChain(attributes))
  }

  /**
   * Find or create file function for internal processing purposes. Also initializes the subdirectory as the system path element if it is not initialized yet
   * @param attributes the attributes to find or create the file by
   */
  private fun findOrCreateFileInternal(attributes: Attributes): MFVirtualFile {
    var current = fsRoot
    createPathChain(attributes)
      .map { seed ->
        findOrCreate(current, seed).also { current = it }
      }[1]
      .also { if (!this::subDirectory.isInitialized) subDirectory = it }
    return current
  }

  /**
   * Get or create virtual file by the provided attributes. Also sends ATTRIBUTES_CHANGED topic message for the created file, triggering "onCreate" event
   * @param attributes the attributes to get or create the virtual file by
   */
  override fun getOrCreateVirtualFile(attributes: Attributes): MFVirtualFile {
    return lock.write {
      getVirtualFile(attributes)?.let {
        if (!it.isValid) return@let
        val oldAttributes = getAttributes(it)
        if (oldAttributes != null) {
          updateAttributesInternal(it, oldAttributes, mergeAttributes(oldAttributes, attributes))
          return it
        }
      }

      val createdFile = findOrCreateFileInternal(attributes)
      fileToAttributesMap[createdFile] = attributes
      attributesToFileMap[buildUniqueAttributes(attributes)] = createdFile
      sendTopic(AttributesService.ATTRIBUTES_CHANGED).onCreate(attributes, createdFile)

      createdFile
    }
  }

  /**
   * Update attributes for the file
   * @param file the file to update attributes for
   * @param newAttributes the new attributes to assign to the file
   */
  override fun updateAttributes(file: MFVirtualFile, newAttributes: Attributes) {
    lock.write { getAttributes(file)?.let { updateAttributesInternal(file, it, newAttributes) } }
  }

  /**
   * Clear the file attributes from the attributes service. Also sends ATTRIBUTES_CHANGED topic message, triggering "onDelete" event for the file
   * @param file the file to clean attributes of
   */
  override fun clearAttributes(file: MFVirtualFile) {
    lock.write {
      getAttributes(file)?.let {
        attributesToFileMap.remove(buildUniqueAttributes(it))
        fileToAttributesMap.remove(file)
        sendTopic(AttributesService.ATTRIBUTES_CHANGED, dataOpsManager.componentManager).onDelete(it, file)
      }
    }
  }

}

/** Path element wrapper class. Stores the file name, attributes and post create action */
data class PathElementSeed(
  val name: String,
  val fileAttributes: FileAttributes,
  val postCreateAction: MFVirtualFile.() -> Unit = {}
)
