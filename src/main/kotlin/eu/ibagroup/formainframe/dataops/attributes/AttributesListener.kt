package eu.ibagroup.formainframe.dataops.attributes

import com.intellij.openapi.vfs.VirtualFile

interface AttributesListener {

  fun onCreate(attributes: FileAttributes, file: VirtualFile)

  fun onUpdate(oldAttributes: FileAttributes, newAttributes: FileAttributes, file: VirtualFile)

  fun onDelete(attributes: FileAttributes, file: VirtualFile)

}

typealias AttributesCallback<A, F> = (attributes: A, file: F) -> Unit
typealias UpdateAttributesCallback<A, F> = (oldAttributes: A, newAttributes: A, file: F) -> Unit

class AttributesAdapter<Attributes : FileAttributes, VFile : VirtualFile> @PublishedApi internal constructor(
  private val attributesClass: Class<out Attributes>,
  private val fileClass: Class<out VFile>
) {

  private var onCreateCallback: AttributesCallback<Attributes, VFile> = { _, _ -> }
  private var onUpdateCallback: UpdateAttributesCallback<Attributes, VFile> = { _, _, _ -> }
  private var onDeleteCallback: AttributesCallback<Attributes, VFile> = { _, _ -> }

  fun onCreate(callback: AttributesCallback<Attributes, VFile>) {
    onCreateCallback = callback
  }

  fun onUpdate(callback: UpdateAttributesCallback<Attributes, VFile>) {
    onUpdateCallback = callback
  }

  fun onDelete(callback: AttributesCallback<Attributes, VFile>) {
    onDeleteCallback = callback
  }

  private fun performIfOurInstances(
    attributes: FileAttributes,
    file: VirtualFile,
    callback: AttributesCallback<Attributes, VFile>
  ) {
    if (attributesClass.isAssignableFrom(attributes::class.java) && fileClass.isAssignableFrom(file::class.java)
    ) {
      val ourAttributes = attributesClass.cast(attributes)
      val ourFile = fileClass.cast(file)
      callback(ourAttributes, ourFile)
    }
  }

  @PublishedApi
  internal val listener
    get() = object : AttributesListener {
      override fun onCreate(attributes: FileAttributes, file: VirtualFile) {
        if (attributesClass.isAssignableFrom(attributes::class.java) && fileClass.isAssignableFrom(file::class.java)) {
          val ourAttributes = attributesClass.cast(attributes)
          val ourFile = fileClass.cast(file)
          onCreateCallback(ourAttributes, ourFile)
        }
      }

      override fun onUpdate(oldAttributes: FileAttributes, newAttributes: FileAttributes, file: VirtualFile) {
        if (attributesClass.isAssignableFrom(oldAttributes::class.java)
          && attributesClass.isAssignableFrom(newAttributes::class.java)
          && fileClass.isAssignableFrom(file::class.java)
        ) {
          val ourOldAttributes = attributesClass.cast(oldAttributes)
          val ourNewAttributes = attributesClass.cast(newAttributes)
          val ourFile = fileClass.cast(file)
          onUpdateCallback(ourOldAttributes, ourNewAttributes, ourFile)
        }
      }

      override fun onDelete(attributes: FileAttributes, file: VirtualFile) {
        if (attributesClass.isAssignableFrom(attributes::class.java) && fileClass.isAssignableFrom(file::class.java)) {
          val ourAttributes = attributesClass.cast(attributes)
          val ourFile = fileClass.cast(file)
          onDeleteCallback(ourAttributes, ourFile)
        }
      }
    }
}

inline fun <reified Attributes : FileAttributes, reified VFile : VirtualFile> attributesListener(
  init: AttributesAdapter<Attributes, VFile>.() -> Unit
): AttributesListener {
  return AttributesAdapter(Attributes::class.java, VFile::class.java).apply(init).listener
}