/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.utils

import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBTextField
import eu.ibagroup.formainframe.config.ConfigState
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.makeCrudableWithoutListeners
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.config.ws.JobsFilter
import eu.ibagroup.formainframe.config.ws.UssPath
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.explorer.FilesWorkingSet
import eu.ibagroup.r2z.CodePage
import eu.ibagroup.r2z.annotations.ZVersion
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import java.util.stream.Stream
import javax.swing.JTextField

class UtilsTestSpec : ShouldSpec({
  context("utils module: validationFunctions") {
    context("validateForBlank") {
      val jTextField = JTextField()

      // validateForBlank(text: String, component: JComponent)
      should("check that text is not blank") {
        val actual = validateForBlank("text", jTextField)
        val expected = null

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("check that text is blank and the validation info object is returned") {
        val actual = validateForBlank("", jTextField)
        val expected = ValidationInfo("This field must not be blank", jTextField)

        assertSoftly {
          actual shouldBe expected
        }
      }
      // validateForBlank(component: JTextField)
      should("check that text is not blank in a component") {
        jTextField.text = "text"
        val actual = validateForBlank(jTextField)
        val expected = null

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("check that text is blank in a component and validation info object is returned") {
        jTextField.text = null
        val actual = validateForBlank(jTextField)
        val expected = ValidationInfo("This field must not be blank", jTextField)

        assertSoftly {
          actual shouldBe expected
        }
      }
    }
    context("validateConnectionName") {
      val jTextField = JTextField()
      val mockCrud = spyk(makeCrudableWithoutListeners(false) { ConfigState() })

      should("validate connection name when there are no other connections") {
        jTextField.text = "a"
        val initialConName = "initialName"

        every { mockCrud.getAll(ConnectionConfig::class.java) } returns Stream.of()

        val actual = validateConnectionName(jTextField, initialConName, mockCrud)
        val expected = null

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate connection name when there are other connections and the name is unique") {
        jTextField.text = "b1"
        val initialConName = "b"

        every { mockCrud.getAll(ConnectionConfig::class.java) } returns Stream.of(
          ConnectionConfig(
            uuid = "con", name = "a", url = "https://found.com",
            isAllowSelfSigned = false, codePage = CodePage.IBM_1047, zVersion = ZVersion.ZOS_2_1
          ),
          ConnectionConfig(
            uuid = "con1", name = "a1", url = "https://found1.com",
            isAllowSelfSigned = false, codePage = CodePage.IBM_1047, zVersion = ZVersion.ZOS_2_1
          )
        )

        val actual = validateConnectionName(jTextField, initialConName, mockCrud)
        val expected = null

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate connection name when there are other connections and the name is not unique") {
        jTextField.text = "a"
        val initialConName = null

        every { mockCrud.getAll(ConnectionConfig::class.java) } returns Stream.of(
          ConnectionConfig(
            uuid = "con", name = "a", url = "https://found.com",
            isAllowSelfSigned = false, codePage = CodePage.IBM_1047, zVersion = ZVersion.ZOS_2_1
          ),
          ConnectionConfig(
            uuid = "con1", name = "a1", url = "https://found1.com",
            isAllowSelfSigned = false, codePage = CodePage.IBM_1047, zVersion = ZVersion.ZOS_2_1
          )
        )

        val actual = validateConnectionName(jTextField, initialConName, mockCrud)
        val expected = ValidationInfo(
          "You must provide unique connection name. Connection ${jTextField.text} already exists.",
          jTextField
        )

        assertSoftly {
          actual shouldBe expected
        }
      }
    }
    context("validateWorkingSetName") {
      val jTextField = JTextField()
      val mockCrud = spyk(makeCrudableWithoutListeners(false) { ConfigState() })

      should("validate working set name when there are no other working sets") {
        jTextField.text = "a1"
        val initialConName = "a"

        every { mockCrud.getAll(WorkingSetConfig::class.java) } returns Stream.of()

        val actual = validateWorkingSetName(jTextField, initialConName, mockCrud, WorkingSetConfig::class.java)
        val expected = null

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate working set name when there are other working sets and the name is unique") {
        jTextField.text = "b"
        val initialConName = null

        every { mockCrud.getAll(WorkingSetConfig::class.java) } returns Stream.of(
          WorkingSetConfig(uuid = "ws", name = "a", connectionConfigUuid = "con"),
          WorkingSetConfig(uuid = "ws1", name = "a1", connectionConfigUuid = "con1")
        )

        val actual = validateWorkingSetName(jTextField, initialConName, mockCrud, WorkingSetConfig::class.java)
        val expected = null

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate working set name when there are other working sets and the name is not unique") {
        jTextField.text = "a"
        val initialConName = "a1"

        every { mockCrud.getAll(WorkingSetConfig::class.java) } returns Stream.of(
          WorkingSetConfig(uuid = "ws", name = "a", connectionConfigUuid = "con"),
          WorkingSetConfig(uuid = "ws1", name = "a1", connectionConfigUuid = "con1")
        )

        val actual = validateWorkingSetName(jTextField, initialConName, mockCrud, WorkingSetConfig::class.java)
        val expected = ValidationInfo(
          "You must provide unique working set name. Working Set ${jTextField.text} already exists.",
          jTextField
        )

        assertSoftly {
          actual shouldBe expected
        }
      }
    }
    context("validateWorkingSetMaskName") {
      val jTextField = JTextField()
      val mockWs = mockk<FilesWorkingSet>()

      should("validate working set mask name when there are no other working set masks") {
        jTextField.text = "/a1"

        every { mockWs.ussPaths } returns listOf()
        every { mockWs.masks } returns listOf()

        val actual = validateWorkingSetMaskName(jTextField, mockWs)
        val expected = null

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate working set mask name when there are other working set masks and the mask is unique") {
        jTextField.text = "MASK.MASK"

        every { mockWs.ussPaths } returns listOf(UssPath("/path1"), UssPath("/path2"))
        every { mockWs.masks } returns listOf(DSMask("MASK1", mutableListOf<String>()))

        val actual = validateWorkingSetMaskName(jTextField, mockWs)
        val expected = null

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate working set mask name when there are other working set masks and the mask is not unique") {
        jTextField.text = "/path1"

        every { mockWs.ussPaths } returns listOf(UssPath("/path1"), UssPath("/path2"))
        every { mockWs.masks } returns listOf(DSMask("MASK.MASK", mutableListOf<String>()))
        every { mockWs.name } returns "Ws name"

        val actual = validateWorkingSetMaskName(jTextField, mockWs)
        val expected = ValidationInfo(
          "You must provide unique mask in working set. Working Set " +
            "\"${mockWs.name}\" already has mask - ${jTextField.text}", jTextField
        )

        assertSoftly {
          actual shouldBe expected
        }
      }
    }
    // validatePrefixAndOwner
    should("validate that the job owner and prefix are provided and the fields does not match the regular expression") {}
    should("validate that the job owner and prefix are provided and the fields are exceed the 8 characters rule") {}
    should("validate that the job owner and prefix are provided and they are valid") {}
    // validateJobId
    should("validate that the job ID is provided and it exceeds the 8 characters length") {}
    should("validate that the job ID is provided and it does not match the regular expression") {}
    should("validate that the job ID is provided and it is valid") {}
    context("validateJobFilter") {
      val component = JBTextField()

      should("validate that the job filter does not provide owner but provides prefix") {
        val actual = validateJobFilter("TEST", "", "", component, false)
        val expected = ValidationInfo("You must provide either an owner and a prefix or a job ID.", component)

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate that the job filter does not provide prefix but provides owner") {
        val actual = validateJobFilter("", "TEST", "", component, false)
        val expected = ValidationInfo("You must provide either an owner and a prefix or a job ID.", component)

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate that the job filter provide prefix and owner") {
        val actual = validateJobFilter("TEST", "TEST", "", component, false)
        val expected = null

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate that the job filter provides only job ID") {
        val actual = validateJobFilter("", "", "TEST", component, true)
        val expected = null

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate that the job filter provides all fields") {
        val actual = validateJobFilter("TEST", "TEST", "TEST", component, true)
        val expected = ValidationInfo("You must provide either an owner and a prefix or a job ID.", component)

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate that the job filter provides only the blank fields") {
        val actual = validateJobFilter("", "", "", component, false)
        val expected = ValidationInfo("You must provide either an owner and a prefix or a job ID.", component)

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate that the job filter is already exist") {
        val prefix = "EXIST"
        val owner = "EXIST"
        val jobId = ""
        val isJobId = false
        val jobFilters =
          listOf(
            JobsFilter(owner, prefix, jobId),
            JobsFilter("DIFFRNT", "*", "")
          )

        val actual = validateJobFilter(prefix, owner, jobId, jobFilters, component, isJobId)
        val expected = ValidationInfo("Job Filter with provided data already exists.", component)

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate that the job filter is already exist and there were no changes to the existing filter") {
        val prefix = "EXIST"
        val owner = "EXIST"
        val jobId = ""
        val initJobFilter = JobsFilter(prefix, owner, jobId)
        val isJobId = false
        val jobFilters =
          listOf(
            JobsFilter(owner, prefix, jobId),
            JobsFilter("DIFFRNT", "*", "")
          )

        val actual = validateJobFilter(initJobFilter, prefix, owner, jobId, jobFilters, component, isJobId)
        val expected = null

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate that the job filter is changed") {
        val prefix = "EXIST"
        val owner = "EXIST"
        val jobId = ""
        val initJobFilter = JobsFilter(prefix, owner, jobId)
        val isJobId = false
        val jobFilters =
          listOf(
            JobsFilter(owner, prefix, jobId),
            JobsFilter("DIFFRNT", "*", "")
          )
        val newPrefix = "NEXIST"

        val actual = validateJobFilter(initJobFilter, newPrefix, owner, jobId, jobFilters, component, isJobId)
        val expected = null

        assertSoftly {
          actual shouldBe expected
        }
      }
    }
    // validateZosmfUrl
    should("validate correct URL") {}
    should("validate wrong URL") {}
    // validateFieldWithLengthRestriction
    should("validate that text does not exceed the specified length") {}
    should("validate that text exceeds the specified length") {}
    // validateDatasetMask
    should("validate that long dataset mask matches all the rules") {}
    should("validate that short dataset mask matches all the rules") {}
    should("validate that dataset mask does not match the 44 characters long rule") {}
    should("validate that dataset mask does not match the qualifier 1 to 8 rule") {}
    should("validate that dataset mask does not match the blank rule") {}
    should("validate that dataset mask does not match the regular expression rule") {}
    should("validate that dataset mask does not match the asterisks rule") {}
    // validateUssMask
    should("validate that USS path when it is valid") {}
    should("validate that USS path when it is blank") {}
    should("validate that USS path when it does not match the regular expression") {}
    // validateUssFileName
    should("validate that USS file name when it is valid") {}
    should("validate that USS path when it exceeds the 255 characters long rule") {}
    should("validate that USS path when it contains the forbidden symbol") {}
    // validateUssFileNameAlreadyExists
    should("validate that the USS file name is not already exist") {}
    should("validate that the USS file name is already exist") {}
    should("validate that the USS directory name is already exist") {}
    // validateDataset
    should("validate that the provided dataset parameters are valid") {}
    // validateDatasetNameOnInput
    should("check that the dataset name is valid") {}
    should("validate that the dataset name exceeds the 44 characters length rule") {}
    should("validate that the dataset name exceeds the 8 characters per HLQ rule") {}
    should("validate that the dataset name does not match the regular expression") {}
    should("validate the dataset name is blank") {}
    // validateVolser
    should("validate the VOLSER does not match the regular expression") {}
    should("check the VOLSER is valid") {}
    // validateForGreaterValue
    should("validate that the number is greater than the provided one") {}
    should("validate that the number is not greater than the provided one") {}
    // validateMemberName
    should("validate that the dataset member name exceeds the 8 character length rule") {}
    should("validate that the dataset member name does not match the dataset member name first letter regular expression") {}
    should("validate that the dataset member name does not match the dataset member name regular expression") {}
  }
  context("utils module: retrofitUtils") {
    // cancelByIndicator
    should("cancel the call on the progress indicator finish") {}
  }
  context("utils module: miscUtils") {
    // debounce
    should("run a block of code after the debounce action") {}
  }
})
