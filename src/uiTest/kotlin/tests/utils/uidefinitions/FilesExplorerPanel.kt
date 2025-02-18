/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   IBA Group
 *   Uladzislau Kalesnikau
 */

package tests.utils.uidefinitions

import com.intellij.driver.sdk.ui.components.*
import com.intellij.driver.client.Driver
import tests.utils.*
import tests.utils.uidefinitions.dialogs.UnsecureConnectionDialog
import tests.utils.uidefinitions.dialogs.AddConnectionDialog
import tests.utils.uidefinitions.dialogs.ErrorCreatingConnectionDialog
import java.awt.event.KeyEvent

enum class ActionMenuPoints(val point: String) {
  CONNECTION("Connection"),
  WORKING_SET("Working Set"),
}

/** File explorer panel wrapper. Provides functionalities to work with File Explorer view elements */
class FilesExplorerPanel(val driver: Driver) {

  lateinit var explorerView: UiComponent
  lateinit var fileExplorerTree: JTreeUiComponent
  lateinit var deleteWsDialog: DialogUiComponent
  lateinit var deleteFilesDialog: DialogUiComponent
  lateinit var plusDropdownList: JListUiComponent
  lateinit var rightClickMenu: PopupMenuUiComponent

  val plusButton by lazy { explorerView.actionButton { byAttribute("myicon", "add.svg") } }

  init {
    driver.ideFrame {
      explorerView = x("//div[@class='SimpleToolWindowPanel' and div[@class='FileExplorerView']]")
      plusDropdownList = popup().list { byClass("MyList") }
      fileExplorerTree = tree("//div[@class='DnDAwareTree']")
      rightClickMenu = popupMenu()
      deleteWsDialog = dialog(locator = { and(byClass("MyDialog"), contains(".='Confirm File Working Set(s) Deletion'")) })
      deleteFilesDialog = dialog(title = "Confirm Files Deletion")
    }
  }

  /**
   * Open a respective dialog by the "+" button in explorer view
   * @param point the menu point to select
   */
  fun openDialogByPlusButtonInExplorer(point: ActionMenuPoints) {
    plusButton.click()
    if (point == ActionMenuPoints.CONNECTION || point == ActionMenuPoints.WORKING_SET) {
      plusDropdownList.clickItem(point.point)
    } else {
      throw IllegalArgumentException("Unsupported point: $point")
    }
  }

  /**
   * Imitate the right mouse click on the element on the [row].
   * Will select the [menuPoint], and, if provided, the [submenuPoint] if the menu is a drop-down list
   */
  fun selectRightClickMenuItem(row: Int, menuPoint: String, submenuPoint: String? = null) {
    fileExplorerTree.rightClickRow(row)
    rightClickMenu.select(menuPoint)
    if (submenuPoint != null) {
      rightClickMenu.select(submenuPoint)
    }
  }

  /** Delete a working set from the explorer tree by the [rowNumber] */
  fun deleteWorkingSet(rowNumber: Int) {
    selectRightClickMenuItem(rowNumber, "Delete...")
    val yesButton = deleteWsDialog.actionButton { byVisibleText("Yes") }
    yesButton.click()
  }

  /**
   * Wait 15 seconds for the row by the provided [rowToBeLoaded] number to change from "loaded..." to something else.
   * Will throw if the row is not loaded after the timeout
   */
  fun waitForTreeToLoadRow(rowToBeLoaded: Int) {
    var loadingRow = fileExplorerTree.collectExpandedPaths()[rowToBeLoaded]
    if (loadingRow.path.last().contains("loading…")) {
      var isLoadingFinished = false
      var isTimeoutExceeded = false
      var currentCounter = 15
      while (!isLoadingFinished && !isTimeoutExceeded) {
        loadingRow = fileExplorerTree.collectExpandedPaths()[rowToBeLoaded]
        if (loadingRow.path.last().contains("loading…")) {
          Thread.sleep(1000)
          currentCounter--
          if (currentCounter == 0) {
            isTimeoutExceeded = true
          }
        } else {
          isLoadingFinished = true
        }
      }
      // Incorrect IntelliJ behavior
      if (!isLoadingFinished && isTimeoutExceeded) {
        throw Exception("Tree is not loaded in 15 seconds")
      }
    }
  }

  // TODO: load more and expanded paths handling
  /** Delete all mask elements, where the mask is in the [maskRowInTree] line */
  fun deleteAllMaskElements(maskRowInTree: Int) {
    val mask = fileExplorerTree.collectExpandedPaths()[maskRowInTree]
    if (mask.path.size != 2) {
      throw Exception("The provided mask row ($maskRowInTree) is not a mask")
    }
    val startElemIdx = maskRowInTree + 1
    waitForTreeToLoadRow(startElemIdx)
    val currentTreePath = fileExplorerTree.collectExpandedPaths()
    val elemToDeleteStart = currentTreePath[startElemIdx]
    if (!elemToDeleteStart.path.last().contains("No items found")) {
      var nextElemIdx = startElemIdx + 1
      while (nextElemIdx <= currentTreePath.size - 1) {
        val nextTreeElem = currentTreePath[nextElemIdx]
        if (nextTreeElem.path.size != 3) {
          break
        }
        nextElemIdx++
      }
      val elementsToDeleteCount = nextElemIdx - startElemIdx

      if (elementsToDeleteCount > 1) {
        fileExplorerTree.clickRow(startElemIdx)
        driver.ideFrame {
          keyboard { pressing(KeyEvent.VK_SHIFT) { fileExplorerTree.clickRow(startElemIdx + elementsToDeleteCount - 1) } }
        }
      }
      selectRightClickMenuItem(startElemIdx, "Delete…")
      deleteFilesDialog.isVisible()
      assert(deleteFilesDialog.allTextAsString().contains("Are you sure want to delete $elementsToDeleteCount file(s)?"))
      val deleteFilesYesButton = deleteFilesDialog.actionButton { byVisibleText("Yes") }
      deleteFilesYesButton.click()

      // To wait for the delete dialog to finish
      Thread.sleep(3000)
      waitForTreeToLoadRow(startElemIdx)
      val noItemsFoundElem = fileExplorerTree.collectExpandedPaths()[startElemIdx]
      assert(noItemsFoundElem.path.last().contains("No items found"))
    }
  }

  // TODO: move to some abstraction when ready
  /**
   * Create a valid connection in a Files Explorer view
   * @param ideDriver the IDE driver to work with other components
   * @param connectionName the connection name to use
   * @param scheme the HTTP scheme to use in the connection
   * @param host the host to connect to
   * @param port the port to connect to
   * @param username the username to connect to the server with
   * @param password the password to connect to the server with
   * @param isAllowSelfSignedStr if "true", the "Allow self-signed certificates" option will be marked
   */
  fun createValidConnection(
    ideDriver: Driver,
    connectionName: String,
    scheme: String = UI_TEST_HTTP_SCHEME,
    host: String = UI_TEST_HOST,
    port: String = UI_TEST_PORT,
    username: String = UI_TEST_USERNAME,
    password: String = UI_TEST_PASSWORD,
    isAllowSelfSignedStr: String = UI_TEST_ALLOW_SELF_SIGNED
  ) {
    val filesExplorerPanel = FilesExplorerPanel(ideDriver)
    filesExplorerPanel.openDialogByPlusButtonInExplorer(ActionMenuPoints.CONNECTION)
    val addConnectionDialog = AddConnectionDialog(ideDriver)
    val unsecureConnectionDialog = UnsecureConnectionDialog(ideDriver)
    val (url, isAllowSelfSigned) = prepareConnectionInfo(scheme, host, port, isAllowSelfSignedStr)
    addConnectionDialog.fillDialog(connectionName, url, username, password, isAllowSelfSigned)
    if (isAllowSelfSigned) {
      unsecureConnectionDialog.proceedButton.click()
    }
    addConnectionDialog.okButton.click()
    if (!url.contains("https") || isAllowSelfSigned) {
      unsecureConnectionDialog.proceedButton.click()
    }
    // TODO: find a more suitable way to track the connection is checked
    Thread.sleep(3000)
  }

  // TODO: move to some abstraction when ready
  /**
   * Create an invalid connection in a Files Explorer view
   * @param ideDriver the IDE driver to work with other components
   * @param connectionName the connection name to use
   * @param scheme the HTTP scheme to use in the connection
   * @param host the host to connect to
   * @param port the port to connect to
   * @param username the username to connect to the server with
   * @param password the password to connect to the server with
   * @param isAllowSelfSignedStr if "true", the "Allow self-signed certificates" option will be marked
   */
  fun createInvalidConnection(
    connectionName: String,
    ideDriver: Driver,
    scheme: String = UI_TEST_HTTP_SCHEME,
    host: String = UI_TEST_HOST,
    port: String = UI_TEST_PORT,
    username: String = UI_TEST_USERNAME,
    password: String = UI_TEST_PASSWORD,
    isAllowSelfSignedStr: String = UI_TEST_ALLOW_SELF_SIGNED
  ) {
    createValidConnection(ideDriver, connectionName, scheme, host, port, username, password, isAllowSelfSignedStr)
    ErrorCreatingConnectionDialog(ideDriver).yesButton.click()
  }
}
