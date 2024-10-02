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

package eu.ibagroup.formainframe.explorer.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.JBColor
import com.intellij.ui.LayeredIcon
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.IconUtil
import com.intellij.util.containers.toMutableSmartList
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.RemoteQuery
import eu.ibagroup.formainframe.dataops.UnitRemoteQueryImpl
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.explorer.EXPLORER_NOTIFICATION_GROUP_ID
import eu.ibagroup.formainframe.explorer.ExplorerUnit
import eu.ibagroup.formainframe.telemetry.NotificationCompatibleException
import eu.ibagroup.formainframe.telemetry.NotificationsService
import eu.ibagroup.formainframe.utils.runTask
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.formainframe.vfs.MFVirtualFileSystem
import icons.ForMainframeIcons
import org.zowe.kotlinsdk.DatasetOrganization
import javax.swing.JLabel
import javax.swing.SwingConstants

/** PS dataset or a PDS dataset member representation as file node in the explorer tree view */
class FileLikeDatasetNode(
  file: MFVirtualFile,
  project: Project,
  parent: ExplorerTreeNode<ConnectionConfig, *>,
  unit: ExplorerUnit<ConnectionConfig>,
  treeStructure: ExplorerTreeStructureBase
) : ExplorerUnitTreeNodeBase<ConnectionConfig, MFVirtualFile, ExplorerUnit<ConnectionConfig>>(
  file, project, parent, unit, treeStructure
) {
  private val loadingIcon = AnimatedIcon.Default()
  private val undefinedFileIcon = AllIcons.FileTypes.Any_type
  private val migrDsIcon = IconUtil.addText(AllIcons.FileTypes.Any_type, "MIG")
  private val vsamDsIcon = IconUtil.addText(AllIcons.FileTypes.Any_type, "VS")
  private val plainDsIcon = IconUtil.addText(AllIcons.FileTypes.Any_type, "DS")
  private val aliasIcon = IconUtil.addText(AllIcons.FileTypes.Any_type, "ALI")
  private val dsMemberIcon = ForMainframeIcons.MemberIcon
  private val folderDsIcon = ForMainframeIcons.DatasetMask
  private val plainDsWithWarningIcon by lazy {
    val icon = LayeredIcon(3)
    icon.setIcon(plainDsIcon, 0)
    icon.setIcon(
      IconUtil.textToIcon("DS", JLabel(), JBUIScale.scale(6.0f)),
      1,
      SwingConstants.SOUTH_EAST
    )
    icon.setIcon(
      IconUtil.resizeSquared(AllIcons.General.Warning, 8),
      2,
      SwingConstants.SOUTH_WEST
    )
    icon
  }

  override fun isAlwaysLeaf(): Boolean {
    return !value.isDirectory
  }

  /**
   * Fetch mainframe attributes for the node if it is missing them.
   * @param initAttributes the initial attributes available for the node at the moment
   * @param dataOpsManager the data operations manager to run the operation and get some additional parameters
   * @param funcOnNotNeeded the function to run if it is not needed to fetch the attributes
   * @param funcOnComplete the function to run after the attributes are fetched
   */
  fun fetchAttributesForNodeIfMissing(
    initAttributes: RemoteDatasetAttributes,
    dataOpsManager: DataOpsManager,
    funcOnNotNeeded: () -> Unit,
    funcOnComplete: () -> Unit
  ) {
    if (!initAttributes.hasDsOrg && parent is DSMaskNode) {
      val isCustMigrVol = MFVirtualFileSystem.belongsToCustMigrVols(initAttributes.volser ?: "")
      if (!isCustMigrVol) {
        this.navigating = true
        this.update()
        val datasetFileFetchProvider =
          dataOpsManager.getFileFetchProvider<DSMask, RemoteQuery<ConnectionConfig, DSMask, Unit>, MFVirtualFile>(
            DSMask::class.java, RemoteQuery::class.java, MFVirtualFile::class.java
          )
        val connectionConfig: ConnectionConfig = unit.connectionConfig ?: return
        val singleElemQuery =
          UnitRemoteQueryImpl(
            DSMask(initAttributes.name, mutableListOf(), initAttributes.volser ?: ""),
            connectionConfig
          )
        val parentDSMaskNode = parent
        val dsFullListQuery = parentDSMaskNode.query ?: return
        val throwable = runTask(title = "Fetching attributes for ${initAttributes.name}", project = project) {
          var possibleThrowable: Throwable? = null
          try {
            datasetFileFetchProvider.fetchSingleElemAttributes(singleElemQuery, dsFullListQuery)
          } catch (throwable: Throwable) {
            possibleThrowable = if (throwable !is NotificationCompatibleException) {
              val title = "Error during single element attributes fetch"
              val detailsShort = "Message: ${throwable.message}"
              val detailsLong = "Message: ${throwable.message}\nCause: ${throwable.cause}"
              NotificationCompatibleException(title, detailsShort, detailsLong)
            } else {
              throwable
            }
          } finally {
            this.navigating = false
          }
          possibleThrowable
        }
        if (throwable != null) {
          NotificationsService.errorNotification(throwable, project)
          return
        }
        if (value.isDirectory) {
          val newChildNode = parentDSMaskNode.prepareChildNodeFromMFVirtualFile(value)
          parentDSMaskNode.refreshChildNode(this, newChildNode)
          newChildNode.refreshSimilarNodes()
        }
        funcOnComplete()
      } else {
        Notification(
          EXPLORER_NOTIFICATION_GROUP_ID,
          "Dataset is migrated",
          "It is impossible to fetch dataset attributes and contents for migrated dataset",
          NotificationType.WARNING
        ).let {
          Notifications.Bus.notify(it, project)
        }
      }
    } else {
      funcOnNotNeeded()
    }
  }

  /**
   * Process request to open the contents of the [FileLikeDatasetNode].
   * If attributes of the node is not defined due to some circumstances, it tries to acquire them.
   * In the result it may produce [LibraryNode] regarding the attributes from a mainframe
   */
  override fun navigate(requestFocus: Boolean) {
    val dataOpsManager = DataOpsManager.getService()
    val attributes = dataOpsManager.tryToGetAttributes(value) ?: return
    val contentSynchronizer = dataOpsManager.getContentSynchronizer(virtualFile)
    if (attributes is RemoteDatasetAttributes && contentSynchronizer != null) {
      fetchAttributesForNodeIfMissing(
        attributes,
        dataOpsManager,
        { super.navigate(requestFocus) },
        { super.navigate(requestFocus) })
    } else {
      super.navigate(requestFocus)
    }
  }

  override fun update(presentation: PresentationData) {
    when (val attributes = DataOpsManager.getService().tryToGetAttributes(value)) {
      is RemoteDatasetAttributes -> {
        if (this.navigating) {
          presentation.setIcon(AnimatedIcon.Default())
        } else {
          presentation.apply {
            setIcon(
              if (attributes.isMigrated) migrDsIcon
              else if (attributes.volser == "*ALIAS") aliasIcon
              else if (attributes.datasetInfo.datasetOrganization == DatasetOrganization.VS) vsamDsIcon
              else if (!attributes.hasDsOrg) plainDsWithWarningIcon
              else if (value.isDirectory) folderDsIcon
              else plainDsIcon
            )

            if (attributes.isMigrated) {
              forcedTextForeground = JBColor.GRAY
              tooltip = "This dataset is migrated. To make it available, select \"Recall\" in the context menu"
            } else if (!attributes.hasDsOrg) {
              tooltip =
                "The dataset properties are not fully fetched. Open it in the editor or check the properties to try to get attributes"
            }
          }
        }
      }

      is RemoteMemberAttributes -> {
        presentation.setIcon(if (this.navigating) loadingIcon else dsMemberIcon)
      }

      else -> {
        presentation.setIcon(if (this.navigating) loadingIcon else undefinedFileIcon)
      }
    }
    updateNodeTitleUsingCutBuffer(value.presentableName, presentation)
    val dataOpsManager = DataOpsManager.getService()
    getVolserIfPresent(dataOpsManager, value)
      ?.let { presentation.addText(it, SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES) }
  }

  override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
    return value.cachedChildren
      .map { FileLikeDatasetNode(value, notNullProject, this, unit, treeStructure) }
      .toMutableSmartList()
  }

  override fun getVirtualFile(): MFVirtualFile {
    return value
  }
}
