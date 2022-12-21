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
import eu.ibagroup.formainframe.explorer.ui.NodeData
import eu.ibagroup.formainframe.explorer.ui.UssDirNode
import eu.ibagroup.formainframe.explorer.ui.UssFileNode
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.formainframe.vfs.MFVirtualFileSystem
import eu.ibagroup.r2z.DatasetOrganization
import eu.ibagroup.r2z.annotations.ZVersion
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import java.util.stream.Stream
import javax.swing.JTextField

class UtilsTestSpec : ShouldSpec({
  context("utils module: validationFunctions") {
    context("validateForBlank") {
      val jTextField = JTextField()

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
            isAllowSelfSigned = false, zVersion = ZVersion.ZOS_2_1
          ),
          ConnectionConfig(
            uuid = "con1", name = "a1", url = "https://found1.com",
            isAllowSelfSigned = false, zVersion = ZVersion.ZOS_2_1
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
        val existingName = "aaaa"
        val initialConName = "initialName"

        every { mockCrud.getAll(ConnectionConfig::class.java) } returns Stream.of(
          ConnectionConfig(
            uuid = "con", name = "a", url = "https://found.com",
            isAllowSelfSigned = false, zVersion = ZVersion.ZOS_2_1
          ),
          ConnectionConfig(
            uuid = "con1", name = "a1", url = "https://found1.com",
            isAllowSelfSigned = false, zVersion = ZVersion.ZOS_2_1
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
    context("validateZosmfUrl") {
      val component = JTextField()

      should("validate correct URL") {
        component.text = "https://some.url"
        val actual = validateZosmfUrl(component)
        val expected = null

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate wrong URL") {
        component.text = "wrong url\""
        val actual = validateZosmfUrl(component)
        val expected = ValidationInfo("Please provide a valid URL to z/OSMF. Example: https://myhost.com:10443", component)

        assertSoftly {
          actual shouldBe expected
        }
      }
    }
    context("validateFieldWithLengthRestriction") {
      val component = JTextField()

      should("validate that text does not exceed the specified length") {
        component.text = "ewrtyugifkhuf"
        val lengthLimit = 10
        val fieldName = "Test field"
        val actual = validateFieldWithLengthRestriction(component, lengthLimit, fieldName)
        val expected = ValidationInfo("$fieldName length must not exceed $lengthLimit characters.")

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate that text exceeds the specified length") {
        component.text = "ewrtyugifkhuf"
        val lengthLimit = 15
        val fieldName = "Test field"
        val actual = validateFieldWithLengthRestriction(component, lengthLimit, fieldName)
        val expected = null

        assertSoftly {
          actual shouldBe expected
        }
      }
    }
    context("validateDatasetMask") {
      val jTextField = JTextField()

      should("validate that long dataset mask matches all the rules") {
        val dsMaskName = "LONGDATA.SET1234.**.*NAME*.EXAM%%%%.%%%%PLE%"
        val actual = validateDatasetMask(dsMaskName, jTextField)
        val expected = null

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate that short dataset mask matches all the rules") {
        val dsMaskName = "**.*ST.LI*.%%"
        val actual = validateDatasetMask(dsMaskName, jTextField)
        val expected = null

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate that dataset mask does not match the 44 characters long rule") {
        val dsMaskName = "A2345678.A2345678.A2345678.A2345678.A23456789"
        val actual = validateDatasetMask(dsMaskName, jTextField)
        val expected = ValidationInfo("Dataset mask length must not exceed 44 characters", jTextField)

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate that dataset mask does not match the qualifier 1 to 8 rule") {
        val dsMaskName = "A2.A23.A23456789"
        val actual = validateDatasetMask(dsMaskName, jTextField)
        val expected = ValidationInfo("Qualifier must be in 1 to 8 characters", jTextField)

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate that dataset mask does not match the blank rule") {
        val dsMaskName = "     "
        val actual = validateDatasetMask(dsMaskName, jTextField)
        val expected = ValidationInfo("Enter valid dataset mask", jTextField)

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate that dataset mask does not match the regular expression rule") {
        val dsMaskName = "A234.A234!"
        val actual = validateDatasetMask(dsMaskName, jTextField)
        val expected = ValidationInfo("Enter valid dataset mask", jTextField)

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate that dataset mask does not match the asterisks rule") {
        val dsMaskName = "**.A*B*"
        val actual = validateDatasetMask(dsMaskName, jTextField)
        val expected = ValidationInfo("Invalid asterisks in the qualifier", jTextField)

        assertSoftly {
          actual shouldBe expected
        }
      }
    }
    context("validateUssMask") {
      val jTextField = JTextField()

      should("validate that USS path when it is valid") {
        val ussMaskName = "/u/validMask"
        val actual = validateUssMask(ussMaskName, jTextField)
        val expected = null

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate that USS path when it is blank") {
        val ussMaskName = "   "
        val actual = validateUssMask(ussMaskName, jTextField)
        val expected = ValidationInfo("Provide a valid USS path", jTextField)

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate that USS path when it does not match the regular expression") {
        val ussMaskName = "invalidName"
        val actual = validateUssMask(ussMaskName, jTextField)
        val expected = ValidationInfo("Provide a valid USS path", jTextField)

        assertSoftly {
          actual shouldBe expected
        }
      }
    }
    context("validateUssFileName") {
      val jTextField = JTextField()

      should("validate that USS file name when it is valid") {
        jTextField.text = "validName"
        val actual = validateUssFileName(jTextField)
        val expected = null

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate that USS path when it exceeds the 255 characters long rule") {
        jTextField.text = "invalidName".repeat(24)
        val actual = validateUssFileName(jTextField)
        val expected = ValidationInfo("Filename must not exceed 255 characters.", jTextField)

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate that USS path when it contains the forbidden symbol") {
        jTextField.text = "/invalidName"
        val actual = validateUssFileName(jTextField)
        val expected = ValidationInfo("Filename must not contain reserved '/' symbol.", jTextField)

        assertSoftly {
          actual shouldBe expected
        }
      }
    }
    context("validateUssFileNameAlreadyExists") {
      val jTextField = JTextField()
      val mockFileNode = mockk<UssFileNode>()
      val mockDirNode = mockk<UssDirNode>()
      mockkObject(MFVirtualFileSystem)
      every { MFVirtualFileSystem.instance } returns mockk()
      val mockVirtualFile = mockk<MFVirtualFile>()

      should("validate that the USS file name is not already exist") {
        jTextField.text = "notExist"

        val mockNode = spyk(
          NodeData(
            node = mockFileNode,
            file = mockVirtualFile,
            attributes = null
          )
        )

        every { mockFileNode.parent?.children } returns listOf(mockFileNode)
        every { mockFileNode.value.filenameInternal } returns "filename"

        val actual = validateUssFileNameAlreadyExists(jTextField, mockNode)
        val expected = null

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate that the USS file name is already exist") {
        jTextField.text = "filename"

        val mockNode = spyk(
          NodeData(
            node = mockFileNode,
            file = mockVirtualFile,
            attributes = null
          )
        )

        every { mockFileNode.parent?.children } returns listOf(mockFileNode)
        every { mockFileNode.value.filenameInternal } returns "filename"


        val actual = validateUssFileNameAlreadyExists(jTextField, mockNode)
        val expected =
          ValidationInfo("Filename already exists. Please specify another filename.", jTextField).asWarning()

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate that the USS directory name is already exist") {
        jTextField.text = "dirname"

        val mockNode = spyk(
          NodeData(
            node = mockDirNode,
            file = mockVirtualFile,
            attributes = null
          )
        )

        every { mockDirNode.parent?.children } returns listOf(mockDirNode)
        every { mockDirNode.value.path } returns "dirname"

        val actual = validateUssFileNameAlreadyExists(jTextField, mockNode)
        val expected = ValidationInfo(
          "Directory name already exists. Please specify another directory name.",
          jTextField
        ).asWarning()

        assertSoftly {
          actual shouldBe expected
        }
      }
    }
    context("validateDataset") {
      should("validate that the provided dataset parameters are valid") {
        val datasetName = JTextField("name")
        val datasetOrganization = DatasetOrganization.PO
        val primaryAllocation = JTextField("1")
        val secondaryAllocation = JTextField("5")
        val directoryBlocks = JTextField("2")
        val recordLength = JTextField("80")
        val blockSize = JTextField("400")
        val averageBlockLength = JTextField("2000")
        val advancedParameters = JTextField("volser")

        val actual = validateDataset(
          datasetName,
          datasetOrganization,
          primaryAllocation,
          secondaryAllocation,
          directoryBlocks,
          recordLength,
          blockSize,
          averageBlockLength,
          advancedParameters
        )
        val expected = null

        assertSoftly {
          actual shouldBe expected
        }
      }
    }
    // validateDatasetNameOnInput
    should("check that the dataset name is valid") {}
    should("validate that the dataset name exceeds the 44 characters length rule") {}
    should("validate that the dataset name exceeds the 8 characters per HLQ rule") {}
    should("validate that the dataset name does not match the regular expression") {}
    should("validate the dataset name is blank") {}
    context("validateVolser") {
      val component = JTextField()

      should("validate the VOLSER does not match the regular expression") {
        component.text = "zmf04^"
        val actual = validateVolser(component)
        val expected = ValidationInfo("Enter a valid volume serial", component)

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("check the VOLSER is valid") {
        component.text = "zmf046"
        val actual = validateVolser(component)
        val expected = null

        assertSoftly {
          actual shouldBe expected
        }
      }
    }
    context("validateForGreaterValue") {
      val component = JTextField()

      should("validate that the number is greater than the provided one") {
        component.text = "15"
        val value = 10
        val actual = validateForGreaterValue(component, value)
        val expected = null

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate that the number is not greater than the provided one") {
        component.text = "5"
        val value = 10
        val actual = validateForGreaterValue(component, value)
        val expected = ValidationInfo("Enter a number grater than $value", component)

        assertSoftly {
          actual shouldBe expected
        }
      }
    }
    context("validateMemberName") {
      val component = JTextField()

      should("validate that the dataset member name exceeds the 8 character length rule") {
        component.text = "MEMBERNAME"
        val actual = validateMemberName(component)
        val expected = ValidationInfo("Member name must not exceed 8 characters.", component)

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate that the dataset member name does not match the dataset member name first letter regular expression") {
        component.text = "1MEMBER"
        val actual = validateMemberName(component)
        val expected = ValidationInfo("Member name should start with A-Z a-z or national characters", component)

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate that the dataset member name does not match the dataset member name regular expression") {
        component.text = "MEMBER^"
        val actual = validateMemberName(component)
        val expected = ValidationInfo("Member name should contain only A-Z a-z 0-9 or national characters", component)

        assertSoftly {
          actual shouldBe expected
        }
      }
    }
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
