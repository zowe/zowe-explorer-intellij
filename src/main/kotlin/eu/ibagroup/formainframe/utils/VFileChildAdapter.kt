package eu.ibagroup.formainframe.utils

import com.intellij.openapi.vfs.VirtualFile

class VFileChildAdapter private constructor() : Child {

  private var file: VirtualFile? = null

  constructor(file: VirtualFile) : this() {
    this.file = file
  }

  private val parentAdapter by lazy {
    VFileChildAdapter()
  }

  override val parent: Child?
    get() {
      val currentFile = file ?: return null
      return parentAdapter.also { it.file = currentFile.parent }
    }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is VFileChildAdapter) return false

    if (file != other.file) return false

    return true
  }

  override fun hashCode(): Int {
    return file?.hashCode() ?: 0
  }

}