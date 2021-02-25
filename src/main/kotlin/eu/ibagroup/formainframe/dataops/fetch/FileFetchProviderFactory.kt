package eu.ibagroup.formainframe.dataops.fetch

import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.DataOpsComponentFactory
import eu.ibagroup.formainframe.dataops.Query

interface FileFetchProviderFactory : DataOpsComponentFactory<FileFetchProvider<*, *, *>>