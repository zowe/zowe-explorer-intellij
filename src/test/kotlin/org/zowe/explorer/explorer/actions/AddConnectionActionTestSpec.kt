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
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.explorer.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.BalloonBuilder
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.config.connect.ui.zosmf.ConnectionDialog
import org.zowe.explorer.config.connect.ui.zosmf.ConnectionDialogState
import org.zowe.explorer.explorer.ACTION_TOOLBAR
import org.zowe.explorer.explorer.ui.EXPLORER_VIEW
import org.zowe.explorer.explorer.ui.FileExplorerView
import org.zowe.explorer.explorer.ui.JesExplorerView
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.utils.crudable.Crudable
import javax.swing.JComponent
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener

class AddConnectionActionTestSpec : AppInitShouldSpec("explorer/actions/AddConnectionAction", {
  context("all functions") {
    context("actionPerformed") {
      var didShowHint = false
      var didAddNewConnection = false
      var didTriggerAddWorkingSetAction = false
      var didTriggerAddJesWorkingSetAction = false

      var hyperlinkListener: HyperlinkListener? = null

      mockkObject(ConnectionDialog.Companion)

      val mockedBalloonBuilder = mockk<BalloonBuilder> {
        every {
          setHideOnClickOutside(any())
        } returns mockk {
          every {
            setHideOnLinkClick(any())
          } returns mockk {
            every {
              createBalloon()
            } returns mockk {
              every {
                show(any<RelativePoint>(), any())
              } answers {
                didShowHint = true
              }
            }
          }
        }
      }
      val mockedPopupFactory = mockk<JBPopupFactory>()
      mockkStatic(JBPopupFactory::getInstance)
      every { JBPopupFactory.getInstance() } returns mockedPopupFactory

      mockkConstructor(AddWorkingSetAction::class)
      every {
        anyConstructed<AddWorkingSetAction>().actionPerformed(any())
      } answers {
        didTriggerAddWorkingSetAction = true
      }

      mockkConstructor(AddJesWorkingSetAction::class)
      every {
        anyConstructed<AddJesWorkingSetAction>().actionPerformed(any())
      } answers {
        didTriggerAddJesWorkingSetAction = true
      }

      val credentialService = CredentialService.getService()
      every { credentialService.setCredentials(any<String>(), any<String>(), any<CharArray>()) } returns Unit

      val configServiceCrudable = mockk<Crudable> {
        every { nextUniqueValue<ConnectionConfig, String>(ConnectionConfig::class.java) } returns "test"
        every {
          add(any<ConnectionConfig>())
        } answers {
          didAddNewConnection = true
          mockk()
        }
      }
      val configService = ConfigService.getService()
      every { configService.crudable } returns configServiceCrudable

      val eventMock = mockk<AnActionEvent> {
        every { project } returns mockk()
      }

      val addConnectionAction = AddConnectionAction()

      beforeEach {
        didShowHint = false
        didAddNewConnection = false
        didTriggerAddWorkingSetAction = false
        didTriggerAddJesWorkingSetAction = false

        hyperlinkListener = null

        every {
          ConnectionDialog
            .showAndTestConnection(any<Crudable>(), null, any<Project>(), any<ConnectionDialogState>())
        } returns mockk {
          every { connectionConfig } returns mockk {
            every { uuid } returns "test_uuid"
          }
          every { username } returns "TESTUSER"
          every { password } returns "TESTPASS".toCharArray()
        }

        every { eventMock.getData(EXPLORER_VIEW) } returns mockk<FileExplorerView> {
          every { myTree } returns mockk {
            every { isEmpty } returns true
          }
        }
        every { eventMock.getData(ACTION_TOOLBAR) } returns mockk {
          every { component } returns mockk {
            every { components } returns arrayOf(
              mockk<JComponent> {
                every { width } returns 2
                every { height } returns 1
                every { parent } returns null
                every { isShowing } returns false
              }
            )
          }
        }
        every {
          mockedPopupFactory.createHtmlTextBalloonBuilder(any<String>(), any(), any())
        } returns mockedBalloonBuilder
      }

      should("add a new connection in the File Explorer view, providing a hint that it is possible to create a new working set in the empty view, and triggering the Add Working Set action") {
        var didBuildCorrectHint = false

        every {
          mockedPopupFactory.createHtmlTextBalloonBuilder(any<String>(), any(), any())
        } answers {
          val hintText = firstArg<String>()
          didBuildCorrectHint =
            hintText == "Now you can add working set to browse<br> z/OS datasets and USS files.<br><a href\"\">Click here to add...</a>"
          hyperlinkListener = thirdArg<HyperlinkListener>()
          mockedBalloonBuilder
        }

        addConnectionAction.actionPerformed(eventMock)

        assertSoftly {
          didAddNewConnection shouldBe true
          didBuildCorrectHint shouldBe true
          didShowHint shouldBe true
          hyperlinkListener shouldNotBe null
        }

        hyperlinkListener?.hyperlinkUpdate(mockk { every { eventType } returns HyperlinkEvent.EventType.ACTIVATED })

        assertSoftly {
          didTriggerAddWorkingSetAction shouldBe true
          didTriggerAddJesWorkingSetAction shouldBe false
        }
      }

      should("add a new connection in the Jes Explorer view, providing a hint that it is possible to create a new working set in the empty view, and triggering the Add JES Working Set action") {
        var didBuildCorrectHint = false

        every {
          mockedPopupFactory.createHtmlTextBalloonBuilder(any<String>(), any(), any())
        } answers {
          val hintText = firstArg<String>()
          didBuildCorrectHint =
            hintText == "Now you can add working set to browse<br> z/OS jobs.<br><a href\"\">Click here to add...</a>"
          hyperlinkListener = thirdArg<HyperlinkListener>()
          mockedBalloonBuilder
        }

        every { eventMock.getData(EXPLORER_VIEW) } returns mockk<JesExplorerView> {
          every { myTree } returns mockk {
            every { isEmpty } returns true
          }
        }

        addConnectionAction.actionPerformed(eventMock)

        assertSoftly {
          didAddNewConnection shouldBe true
          didBuildCorrectHint shouldBe true
          didShowHint shouldBe true
          hyperlinkListener shouldNotBe null
        }

        hyperlinkListener?.hyperlinkUpdate(mockk { every { eventType } returns HyperlinkEvent.EventType.ACTIVATED })

        assertSoftly {
          didTriggerAddWorkingSetAction shouldBe false
          didTriggerAddJesWorkingSetAction shouldBe true
        }
      }

      should("add a new connection in an unknown view, providing a hint that it is possible to create a new working set in the empty view, and not triggering an action in the unknown view") {
        var didBuildCorrectHint = false

        every {
          mockedPopupFactory.createHtmlTextBalloonBuilder(any<String>(), any(), any())
        } answers {
          val hintText = firstArg<String>()
          didBuildCorrectHint =
            hintText == "Now you can add working set to browse<br> null.<br><a href\"\">Click here to add...</a>"
          hyperlinkListener = thirdArg<HyperlinkListener>()
          mockedBalloonBuilder
        }

        every { eventMock.getData(EXPLORER_VIEW) } returns mockk {
          every { myTree } returns mockk {
            every { isEmpty } returns true
          }
        }

        addConnectionAction.actionPerformed(eventMock)

        assertSoftly {
          didAddNewConnection shouldBe true
          didBuildCorrectHint shouldBe true
          didShowHint shouldBe true
          hyperlinkListener shouldNotBe null
        }

        hyperlinkListener?.hyperlinkUpdate(mockk { every { eventType } returns HyperlinkEvent.EventType.ACTIVATED })

        assertSoftly {
          didTriggerAddWorkingSetAction shouldBe false
          didTriggerAddJesWorkingSetAction shouldBe false
        }
      }

      should("add a new connection and not produce a hint when there is no components in the explorer's toolbar") {
        every { eventMock.getData(ACTION_TOOLBAR) } returns mockk {
          every { component } returns mockk {
            every { components } returns arrayOf()
          }
        }

        addConnectionAction.actionPerformed(eventMock)

        assertSoftly {
          didAddNewConnection shouldBe true
          didShowHint shouldBe false
          hyperlinkListener shouldBe null
        }
      }

      should("add a new connection and not produce a hint when there is no toolbar") {
        every { eventMock.getData(ACTION_TOOLBAR) } returns null

        addConnectionAction.actionPerformed(eventMock)

        assertSoftly {
          didAddNewConnection shouldBe true
          didShowHint shouldBe false
          hyperlinkListener shouldBe null
        }
      }

      should("add a new connection and not produce a hint when there is no active explorer") {
        every { eventMock.getData(EXPLORER_VIEW) } returns null

        addConnectionAction.actionPerformed(eventMock)

        assertSoftly {
          didAddNewConnection shouldBe true
          didShowHint shouldBe false
          hyperlinkListener shouldBe null
        }
      }

      should("not add a new connection and not produce a hint when a user cancelled the Add Connection dialog") {
        every {
          ConnectionDialog
            .showAndTestConnection(any<Crudable>(), null, any<Project>(), any<ConnectionDialogState>())
        } returns null

        addConnectionAction.actionPerformed(eventMock)

        assertSoftly {
          didAddNewConnection shouldBe false
          didShowHint shouldBe false
        }
      }
    }
  }
})
