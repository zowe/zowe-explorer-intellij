package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.application.EDT
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CollectionComboBoxModel
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import eu.ibagroup.formainframe.tso.config.TSOSessionConfig
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.initialize
import eu.ibagroup.formainframe.utils.validateTsoSessionSelection
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.stream.Stream
import javax.swing.JList
import javax.swing.ListCellRenderer

class SelectTSOSessionDialogTestSpec : WithApplicationShouldSpec({

  afterSpec {
    clearAllMocks()
  }

  context("explorer module: ui/SelectTSOSessionDialog") {

    lateinit var dialog: SelectTSOSessionDialog

    val state = SelectTSOSessionDialogState(null)

    val crudableMock = mockk<Crudable>()
    every { crudableMock.getAll(TSOSessionConfig::class.java) } answers {
      Stream.of(TSOSessionConfig())
    }

    val createCenterPanelMethod = DialogWrapper::class.java.getDeclaredMethod("createCenterPanel")
    createCenterPanelMethod.isAccessible = true

    beforeEach {
      mockkStatic(::initialize)
      every { initialize(any()) } returns Unit

      dialog = runBlocking {
        withContext(Dispatchers.EDT) {
          SelectTSOSessionDialog(mockk(), crudableMock, state)
        }
      }

      mockkStatic(::validateTsoSessionSelection)
      every { validateTsoSessionSelection(any(), any()) } returns null
    }

    afterEach {
      unmockkAll()
    }

    // createCenterPanel
    should("create panel") {
      val panel = runBlocking {
        withContext(Dispatchers.EDT) {
          createCenterPanelMethod.invoke(dialog) as? DialogPanel
        }
      }

      assertSoftly {
        panel shouldNotBe null
      }
    }
    should("validate panel") {
      val panel = runBlocking {
        withContext(Dispatchers.EDT) {
          createCenterPanelMethod.invoke(dialog) as? DialogPanel
        }
      }
      panel?.registerValidators { }
      panel?.validateAll()

      verify { validateTsoSessionSelection(any(), any()) }
    }
    should("render TSO sessions combobox") {
      val panel = runBlocking {
        withContext(Dispatchers.EDT) {
          createCenterPanelMethod.invoke(dialog) as? DialogPanel
        }
      }

      val comboBox = panel?.components?.filterIsInstance<ComboBox<TSOSessionConfig>>()?.firstOrNull()

      @Suppress("UNCHECKED_CAST")
      val renderer = comboBox?.renderer as? ListCellRenderer<TSOSessionConfig>

      val comboBoxModel = CollectionComboBoxModel(listOf<TSOSessionConfig>())

      val component = renderer?.getListCellRendererComponent(
        JList(comboBoxModel),
        TSOSessionConfig(),
        0,
        true,
        true
      )

      assertSoftly {
        comboBox shouldNotBe null
        renderer shouldNotBe null
        component shouldNotBe null
      }
    }

  }

})