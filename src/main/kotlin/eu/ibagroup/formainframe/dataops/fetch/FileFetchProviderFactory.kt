package eu.ibagroup.formainframe.dataops.fetch

import com.intellij.openapi.extensions.ExtensionPointName
import eu.ibagroup.formainframe.explorer.Explorer

interface FileFetchProviderFactory {

  companion object {
    @JvmStatic
    val EP = ExtensionPointName.create<FileFetchProviderFactory>("eu.ibagroup.formainframe.fileDataProviderFactory")
  }

  fun buildProvider(explorer: Explorer): FileFetchProvider<*, *, *>

}