package org.zowe.explorer.explorer.ui

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CollectionComboBoxModel
import org.zowe.explorer.tso.config.TSOSessionConfig
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.utils.initialize
import org.zowe.explorer.utils.validateTsoSessionSelection
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import java.util.stream.Stream
import javax.swing.JList
import javax.swing.ListCellRenderer

class SelectTSOSessionDialogTestSpec: ShouldSpec({

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

      dialog = SelectTSOSessionDialog(mockk(), crudableMock, state)

      mockkStatic(::validateTsoSessionSelection)
      every { validateTsoSessionSelection(any(), any()) } returns null
    }

    afterEach {
      unmockkAll()
    }

    // createCenterPanel
    should("create panel") {
      val panel = createCenterPanelMethod.invoke(dialog) as? DialogPanel

      assertSoftly {
        panel shouldNotBe null
      }
    }
    should("validate panel") {
      val panel = createCenterPanelMethod.invoke(dialog) as? DialogPanel
      panel?.registerValidators {  }
      panel?.validateAll()

      verify { validateTsoSessionSelection(any(), any()) }
    }
    should("render TSO sessions combobox") {
      val panel = createCenterPanelMethod.invoke(dialog) as? DialogPanel

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
