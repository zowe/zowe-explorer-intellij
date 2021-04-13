package eu.ibagroup.formainframe.dataops.synchronizer

import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.rd.util.ConcurrentHashMap
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.VFileInfoAttributes
import eu.ibagroup.formainframe.utils.ChannelExecutor
import eu.ibagroup.formainframe.utils.QueueExecutor
import kotlinx.coroutines.channels.Channel

private val CHANNEL_DELAY = service<ConfigService>().autoSaveDelay

abstract class AbstractAttributedContentSynchronizer<Attributes : VFileInfoAttributes>(
  dataOpsManager: DataOpsManager
) : AbstractQueuedContentSynchronizer(dataOpsManager) {

  protected val attributesService by lazy {
    dataOpsManager.getAttributesService(attributesClass, vFileClass)
  }

  protected val fileToConfigMap = ConcurrentHashMap<VirtualFile, SyncProvider>()

  protected abstract fun execute(syncProvider: SyncProvider)

  @Suppress("UNCHECKED_CAST")
  override fun buildExecutorForFile(
    providerFactory: (QueueExecutor<Unit>) -> SyncProvider
  ): Pair<QueueExecutor<Unit>, SyncProvider> {
    val executor = ChannelExecutor<Unit>(Channel(Channel.CONFLATED), CHANNEL_DELAY)
    val syncProvider = providerFactory(executor)
    fileToConfigMap[syncProvider.file] = syncProvider
    executor.launch { execute(syncProvider) }
    return Pair(executor, syncProvider)
  }

  override fun accepts(file: VirtualFile): Boolean {
    return vFileClass.isAssignableFrom(file::class.java) && attributesService.getAttributes(file) != null
  }

  protected abstract val vFileClass: Class<out VirtualFile>

  protected abstract val attributesClass: Class<out Attributes>

  override fun removeSync(file: VirtualFile) {
    super.removeSync(file)
    fileToConfigMap.remove(file)
  }

}