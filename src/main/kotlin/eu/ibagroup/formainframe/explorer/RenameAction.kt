package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.ValidationInfo
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.fetchAdapter
import eu.ibagroup.formainframe.dataops.operations.RenameOperation
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.utils.clone
import eu.ibagroup.formainframe.utils.crudable.getAll
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import eu.ibagroup.formainframe.utils.service
import eu.ibagroup.formainframe.utils.validation.*

class RenameAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val selectedNode = e.getData(SELECTED_NODES)?.get(0)
    if (selectedNode != null) {
      val node = selectedNode.node
      var initialState = ""
      if (node is WorkingSetNode) {
        initialState = (selectedNode.node.value as WorkingSet).name
        val dialog = RenameDialog(e.project, "Working Set", initialState).withValidationOnInput {
          validateWorkingSetName(it,initialState, configCrudable)
        }.withValidationForBlankOnApply()
        if (dialog.showAndGet()) {
          val nodeValue = node.value
          val wsToUpdate = configCrudable.getByUniqueKey<WorkingSetConfig>(nodeValue.uuid)?.clone()
          if (wsToUpdate != null) {
            wsToUpdate.name = dialog.state
            configCrudable.update(wsToUpdate)
          }
        }
      } else if (node is DSMaskNode) {
        initialState = (selectedNode.node.value as DSMask).mask
        val dialog = RenameDialog(e.project, "Dataset Mask", initialState).withValidationOnInput {
          validateDatasetMask(it.text, it)
        }.withValidationForBlankOnApply()
        if (dialog.showAndGet()) {
          val parentValue = selectedNode.node.parent?.value as WorkingSet
          val wsToUpdate = configCrudable.getByUniqueKey<WorkingSetConfig>(parentValue.uuid)?.clone()
          if (wsToUpdate != null) {
            wsToUpdate.dsMasks.filter { it.mask == initialState }[0].mask = dialog.state
            configCrudable.update(wsToUpdate)
          }
        }
      } else if (node is LibraryNode || node is FileLikeDatasetNode) {
        val attributes = selectedNode.attributes
        var type = ""
        if (attributes is RemoteDatasetAttributes) {
          initialState = attributes.datasetInfo.name
          type = "Dataset"
        } else if (attributes is RemoteMemberAttributes) {
          initialState = attributes.memberInfo.name
          type = "Member"
        }
        val dialog = RenameDialog(e.project, type, initialState).withValidationOnInput {
          if (attributes is RemoteDatasetAttributes) {
            validateDatasetNameOnInput(it)
          } else {
            validateMemberName(it)
          }
        }.withValidationForBlankOnApply()
        val file = node.virtualFile
        if (dialog.showAndGet() && file != null) {
          service<DataOpsManager>(node.explorer.componentManager).performOperation(
            operation = RenameOperation(
              file = file,
              newName = dialog.state
            ),
            callback = fetchAdapter {
              onSuccess {
                node.parent?.cleanCacheIfPossible()
              }
              onThrowable {
                println(it)
              }
            }
          )
        }
      } else if (selectedNode.node is UssDirNode && selectedNode.node.isConfigUssPath) {
        initialState = selectedNode.node.value.path
        val dialog = RenameDialog(e.project, "Directory", initialState).withValidationOnInput {
          validateUssMask(it.text, it)
        }.withValidationForBlankOnApply()
        if (dialog.showAndGet()) {
          val parentValue = selectedNode.node.parent?.value as WorkingSet
          val wsToUpdate = configCrudable.getByUniqueKey<WorkingSetConfig>(parentValue.uuid)?.clone()
          if (wsToUpdate != null) {
            wsToUpdate.ussPaths.filter { it.path == initialState }[0].path = dialog.state
            configCrudable.update(wsToUpdate)
          }
        }

      } else if (selectedNode.node is UssDirNode || selectedNode.node is UssFileNode) {
        val attributes = selectedNode.attributes as RemoteUssAttributes
        val file = selectedNode.file
        val dialog = RenameDialog(e.project, if (attributes.isDirectory) "Directory" else "File", attributes.name).withValidationOnInput {
          validateUssFileName(it)
        }.withValidationForBlankOnApply()
        if (dialog.showAndGet() && file != null) {
          service<DataOpsManager>(node.explorer.componentManager).performOperation(
            operation = RenameOperation(
              file = selectedNode.file,
              newName = dialog.state
            ),
            callback = fetchAdapter {
              onSuccess {
                node.parent?.cleanCacheIfPossible()
              }
              onThrowable {
                println(it)
              }
            }
          )
        }
      }

    }
  }

  override fun update(e: AnActionEvent) {
    val selectedNodes = e.getData(SELECTED_NODES)
    e.presentation.isEnabledAndVisible = selectedNodes != null && selectedNodes.size == 1
    if (e.presentation.isEnabledAndVisible) {
      val selectedNode = selectedNodes?.get(0)
      if (selectedNode != null && (selectedNode.node is DSMaskNode || (selectedNode.node is UssDirNode && selectedNode.node.isConfigUssPath))) {
        e.presentation.text = "Edit"
      } else {
        e.presentation.text = "Rename"
      }
    }
  }


  override fun isDumbAware(): Boolean {
    return true
  }
}