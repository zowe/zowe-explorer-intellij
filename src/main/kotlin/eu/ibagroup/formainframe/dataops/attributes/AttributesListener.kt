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

package eu.ibagroup.formainframe.dataops.attributes

import com.intellij.openapi.vfs.VirtualFile

/** Attributes listener interface that provides the basic info for attributes listener handlers */
interface AttributesListener {

  fun onCreate(attributes: FileAttributes, file: VirtualFile)

  fun onUpdate(oldAttributes: FileAttributes, newAttributes: FileAttributes, file: VirtualFile)

  fun onDelete(attributes: FileAttributes, file: VirtualFile)

}

typealias AttributesCallback<A, F> = (attributes: A, file: F) -> Unit
typealias UpdateAttributesCallback<A, F> = (oldAttributes: A, newAttributes: A, file: F) -> Unit

/**
 * Class to adapt attribute event handlers
 * @param attributesClass the attributes class to cast the attributes being manipulated to the appropriate class
 * @param fileClass virtual file class to cast the file where the attributes are manipulated to the appropriate class
 */
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

  @PublishedApi
  internal val listener
    get() = object : AttributesListener {
      /**
       * Handle onCreate callback when the attributes and the file have the appropriate classes to cast to
       * @param attributes the attributes to cast and use in callback
       * @param file the file to cast and use in callback
       */
      override fun onCreate(attributes: FileAttributes, file: VirtualFile) {
        if (attributesClass.isAssignableFrom(attributes::class.java) && fileClass.isAssignableFrom(file::class.java)) {
          val ourAttributes = attributesClass.cast(attributes)
          val ourFile = fileClass.cast(file)
          onCreateCallback(ourAttributes, ourFile)
        }
      }

      /**
       * Handle onUpdate callback when the attributes and the file have the appropriate classes to cast to
       * @param oldAttributes the old attributes to cast and use in callback
       * @param newAttributes the new attributes to cast and use in callback
       * @param file the file to cast and use in callback
       */
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

      /**
       * Handle onDelete callback when the attributes and the file have the appropriate classes to cast to
       * @param attributes the attributes to cast and use in callback
       * @param file the file to cast and use in callback
       */
      override fun onDelete(attributes: FileAttributes, file: VirtualFile) {
        if (attributesClass.isAssignableFrom(attributes::class.java) && fileClass.isAssignableFrom(file::class.java)) {
          val ourAttributes = attributesClass.cast(attributes)
          val ourFile = fileClass.cast(file)
          onDeleteCallback(ourAttributes, ourFile)
        }
      }
    }
}

/**
 * Make attributes adapter with the predefined handlers. Default ones will be picked if there are no custom attributes handlers
 * @param init the custom attribute adapters to initiate the handlers from
 */
inline fun <reified Attributes : FileAttributes, reified VFile : VirtualFile> attributesListener(
  init: AttributesAdapter<Attributes, VFile>.() -> Unit
): AttributesListener {
  return AttributesAdapter(Attributes::class.java, VFile::class.java).apply(init).listener
}
