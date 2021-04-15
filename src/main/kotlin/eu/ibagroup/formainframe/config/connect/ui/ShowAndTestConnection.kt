//package eu.ibagroup.formainframe.config.connect.ui
//
//import com.intellij.icons.AllIcons
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.ui.MessageDialogBuilder
//import eu.ibagroup.formainframe.api.api
//import eu.ibagroup.formainframe.dataops.exceptions.CallException
//import eu.ibagroup.formainframe.utils.cancelByIndicator
//import eu.ibagroup.formainframe.utils.crudable.Crudable
//import eu.ibagroup.formainframe.utils.runTask
//import eu.ibagroup.r2z.InfoAPI
//import java.awt.Component
//
//class ShowAndTestConnection(
//  private val state: ConnectionDialogState,
//  private val crudable: Crudable,
//  private val project: Project? = null,
//  private val parentComponent: Component? = null
//) {
//
//  private lateinit var dialog: ConnectionDialog
//
//  fun showUntilTested(): ConnectionDialogState? {
//
//    while (true) {
//      dialog = ConnectionDialog(state = state, crudable = crudable)
//      if (dialog.showAndGet()) {
//        val state = dialog.state
//        try {
//          performTestRequest(state.connectionUrl, state.isAllowSsl, project)
//          return state
//        } catch (t: Throwable) {
//          val confirmMessage = "Do you want to add it anyway?"
//          val tMessage = t.message
//          val message = if (tMessage != null) {
//            "$tMessage\n\n$confirmMessage"
//          } else {
//            confirmMessage
//          }
//          val addAnyway = MessageDialogBuilder
//            .yesNo(
//              title = "Error Creating Connection",
//              message = message
//            ).icon(AllIcons.General.ErrorDialog)
//            .run {
//              if (parentComponent != null) {
//                ask(parentComponent)
//              } else {
//                ask(project)
//              }
//            }
//          if (addAnyway) {
//            return state
//          }
//        }
//      } else {
//        break
//      }
//    }
//    return null
//  }
//
//  private fun performTestRequest(url: String, isAllowSelfSigned: Boolean, project: Project?) {
//    runTask(
//      title = "Testing Connection to $url",
//      project = project
//    ) {
//      return@runTask try {
//        val response = api<InfoAPI>(url, isAllowSelfSigned)
//          .getSystemInfo()
//          .cancelByIndicator(it)
//          .execute()
//        if (response.isSuccessful) {
//          null
//        } else {
//          CallException(response, "Cannot connect to z/OSMF Server")
//        }
//      } catch (t: Throwable) {
//        it.cancel()
//        t
//      }
//    }?.also { throw it }
//  }
//}