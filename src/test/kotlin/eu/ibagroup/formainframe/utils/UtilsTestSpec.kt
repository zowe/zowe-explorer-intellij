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

package eu.ibagroup.formainframe.utils

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.TaskInfo
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.wm.ex.ProgressIndicatorEx
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.components.JBTextField
import eu.ibagroup.formainframe.config.ConfigStateV2
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.makeCrudableWithoutListeners
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.JobsFilter
import eu.ibagroup.formainframe.config.ws.MaskStateWithWS
import eu.ibagroup.formainframe.config.ws.UssPath
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.dataops.sort.SortQueryKeys
import eu.ibagroup.formainframe.explorer.FilesWorkingSet
import eu.ibagroup.formainframe.explorer.ui.NodeData
import eu.ibagroup.formainframe.explorer.ui.UssDirNode
import eu.ibagroup.formainframe.explorer.ui.UssFileNode
import eu.ibagroup.formainframe.tso.config.TSOSessionConfig
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.formainframe.vfs.MFVirtualFileSystem
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.zowe.kotlinsdk.annotations.ZVersion
import retrofit2.Call
import retrofit2.Response
import java.time.Duration
import java.time.Instant.now
import java.time.LocalDateTime
import java.util.*
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
      val mockCrud = spyk(makeCrudableWithoutListeners(false) { ConfigStateV2() })

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
      val mockCrud = spyk(makeCrudableWithoutListeners(false) { ConfigStateV2() })

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
          FilesWorkingSetConfig("ws", "a", "con", mutableListOf(), mutableListOf()),
          FilesWorkingSetConfig("ws1", "a1", "con1", mutableListOf(), mutableListOf())
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
          FilesWorkingSetConfig("ws", "a", "con", mutableListOf(), mutableListOf()),
          FilesWorkingSetConfig("ws1", "a1", "con1", mutableListOf(), mutableListOf())
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
      val mockMaskStateWithWs = mockk<MaskStateWithWS>()

      should("validate working set mask name when there are no other working set masks") {
        jTextField.text = "/a1"

        every { mockWs.ussPaths } returns listOf()
        every { mockWs.masks } returns listOf()

        every { mockMaskStateWithWs.ws } returns mockWs
        every { mockMaskStateWithWs.mask } returns ""
        every { mockMaskStateWithWs.type } returns MaskType.ZOS

        val actual = validateWorkingSetMaskName(jTextField, mockMaskStateWithWs)
        val expected = null

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate working set mask name when there are other working set masks and the mask is unique") {
        jTextField.text = "MASK.MASK"

        every { mockWs.ussPaths } returns listOf(UssPath("/path1"), UssPath("/path2"))
        every { mockWs.masks } returns listOf(DSMask("MASK1", mutableListOf<String>()))

        every { mockMaskStateWithWs.ws } returns mockWs
        every { mockMaskStateWithWs.mask } returns ""
        every { mockMaskStateWithWs.type } returns MaskType.ZOS

        val actual = validateWorkingSetMaskName(jTextField, mockMaskStateWithWs)
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

        every { mockMaskStateWithWs.ws } returns mockWs
        every { mockMaskStateWithWs.mask } returns ""
        every { mockMaskStateWithWs.type } returns MaskType.USS

        val actual = validateWorkingSetMaskName(jTextField, mockMaskStateWithWs)
        val expected = ValidationInfo(
          "You must provide unique mask in working set. Working Set " +
            "\"${mockWs.name}\" already has mask - ${jTextField.text}", jTextField
        )

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate working set mask name when the mask name is not changed") {
        jTextField.text = "/path1"

        every { mockWs.ussPaths } returns listOf(UssPath("/path1"), UssPath("/path2"))
        every { mockWs.masks } returns listOf(DSMask("MASK.MASK", mutableListOf<String>()))
        every { mockWs.name } returns "Ws name"

        every { mockMaskStateWithWs.ws } returns mockWs
        every { mockMaskStateWithWs.mask } returns "/path1"
        every { mockMaskStateWithWs.type } returns MaskType.USS

        val actual = validateWorkingSetMaskName(jTextField, mockMaskStateWithWs)
        val expected = null

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate working set mask name when the mask name is not changed (dataset mask)") {
        jTextField.text = "MASK.name"

        every { mockWs.ussPaths } returns listOf()
        every { mockWs.masks } returns listOf(DSMask("MASK.NAME", mutableListOf()))
        every { mockWs.name } returns "Ws name"

        every { mockMaskStateWithWs.ws } returns mockWs
        every { mockMaskStateWithWs.mask } returns "MASK.NAME"
        every { mockMaskStateWithWs.type } returns MaskType.ZOS

        val actual = validateWorkingSetMaskName(jTextField, mockMaskStateWithWs)
        val expected = null

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
        val expected =
          ValidationInfo("Please provide a valid URL to z/OSMF. Example: https://myhost.com:10443", component)

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
      val mockFileNode1 = mockk<UssFileNode>()
      val mockFileNode2 = mockk<UssFileNode>()
      val mockDirNode1 = mockk<UssDirNode>()
      val mockDirNode2 = mockk<UssDirNode>()
      mockkObject(MFVirtualFileSystem)
      every { MFVirtualFileSystem.instance } returns mockk()
      val mockVirtualFile = mockk<MFVirtualFile>()

      should("validate that the USS file name is not already exist") {
        jTextField.text = "notExist"

        val mockNode = spyk(
          NodeData(
            node = mockFileNode1,
            file = mockVirtualFile,
            attributes = null
          )
        )

        every { mockFileNode1.parent?.children } returns listOf(mockFileNode1)
        every { mockFileNode1.value.filenameInternal } returns "filename"

        val actual = validateUssFileNameAlreadyExists(jTextField, mockNode)
        val expected = null

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate that the USS file name is already exist") {
        jTextField.text = "filename2"

        val mockNode1 = spyk(
          NodeData(
            node = mockFileNode1,
            file = mockVirtualFile,
            attributes = null
          )
        )

        every { mockFileNode1.parent?.children } returns listOf(mockFileNode1, mockFileNode2)
        every { mockFileNode1.value.filenameInternal } returns "filename1"
        every { mockFileNode2.value.filenameInternal } returns "filename2"


        val actual = validateUssFileNameAlreadyExists(jTextField, mockNode1)
        val expected =
          ValidationInfo("Filename already exists. Please specify another filename.", jTextField).asWarning()

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate that the USS file name is the same as the previous one") {
        jTextField.text = "filename1"

        val mockNode1 = spyk(
          NodeData(
            node = mockFileNode1,
            file = mockVirtualFile,
            attributes = null
          )
        )

        every { mockFileNode1.parent?.children } returns listOf(mockFileNode1)
        every { mockFileNode1.value.filenameInternal } returns "filename1"


        val actual = validateUssFileNameAlreadyExists(jTextField, mockNode1)
        val expected = null

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate that the USS directory name is already exist") {
        jTextField.text = "dirname2"

        val mockNode1 = spyk(
          NodeData(
            node = mockDirNode1,
            file = mockVirtualFile,
            attributes = null
          )
        )

        every { mockDirNode1.parent?.children } returns listOf(mockDirNode1, mockDirNode2)
        every { mockDirNode1.value.path } returns "dirname1"
        every { mockDirNode2.value.path } returns "dirname2"

        val actual = validateUssFileNameAlreadyExists(jTextField, mockNode1)
        val expected = ValidationInfo(
          "Directory name already exists. Please specify another directory name.",
          jTextField
        ).asWarning()

        assertSoftly {
          actual shouldBe expected
        }
      }
      should("validate that the USS directory name is the same as the previous one") {
        jTextField.text = "dirname1"

        val mockNode1 = spyk(
          NodeData(
            node = mockDirNode1,
            file = mockVirtualFile,
            attributes = null
          )
        )

        every { mockDirNode1.parent?.children } returns listOf(mockDirNode1)
        every { mockDirNode1.value.path } returns "dirname1"

        val actual = validateUssFileNameAlreadyExists(jTextField, mockNode1)
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
    context("validateForGreaterOrEqualValue") {
      val component = JTextField()

      should("validate that the number is greater than or equal to the provided one") {
        component.text = "15"
        val value = 10
        val actual = validateForGreaterOrEqualValue(component, value)

        val value2 = 10L
        val actual2 = validateForGreaterOrEqualValue(component, value2)

        val expected = null

        assertSoftly {
          actual shouldBe expected
          actual2 shouldBe expected
        }
      }
      should("validate that the number is not greater than or equal to the provided one") {
        component.text = "5"
        val value = 10
        val actual = validateForGreaterOrEqualValue(component, value)

        val value2 = 10L
        val actual2 = validateForGreaterOrEqualValue(component, value2)

        val expected = ValidationInfo("Enter a number greater than or equal to $value", component)

        assertSoftly {
          actual shouldBe expected
          actual2 shouldBe expected
        }
      }
      should("validate that the number is not valide") {
        component.text = "10A"
        val value = 0
        val actual = validateForGreaterOrEqualValue(component, value)

        val value2 = 0L
        val actual2 = validateForGreaterOrEqualValue(component, value2)

        val expected = ValidationInfo("Enter a valid number", component)

        assertSoftly {
          actual shouldBe expected
          actual2 shouldBe expected
        }
      }
      should("validate that the number is not positive") {
        component.text = "-10"
        val value = 0
        val actual = validateForGreaterOrEqualValue(component, value)

        val value2 = 0L
        val actual2 = validateForGreaterOrEqualValue(component, value2)

        val expected = ValidationInfo("Enter a positive number", component)

        assertSoftly {
          actual shouldBe expected
          actual2 shouldBe expected
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
    context("cancelByIndicator") {
      lateinit var delegate: ProgressIndicatorEx

      val mockCall = mockk<Call<Any>>()
      val mockProgressIndicatorEx = mockk<ProgressIndicatorEx>()

      every { mockProgressIndicatorEx.addStateDelegate(any()) } answers {
        delegate = firstArg()
      }

      should("cancel the call due to progress indicator is cancelled") {
        var isCancelDelegateCalled = false

        every { mockCall.cancel() } answers {
          isCancelDelegateCalled = true
        }

        mockCall.cancelByIndicator(mockProgressIndicatorEx)
        delegate.cancel()

        assertFalse(delegate.wasStarted())
        assertThrows(UnsupportedOperationException::class.java) { delegate.addStateDelegate(mockk()) }
        assert(isCancelDelegateCalled)
      }
      should("not cancel the call when the processing is finished") {
        every { mockCall.execute() } returns Response.success("Success")
        val taskInfo = mockk<TaskInfo>()
        val result = mockCall.cancelByIndicator(mockProgressIndicatorEx).execute()

        val mockProgressIndicator = mockk<ProgressIndicator>()
        val result2 = mockCall.cancelByIndicator(mockProgressIndicator)

        delegate.processFinish()
        delegate.finish(taskInfo)
        assertEquals(mockCall, result2)
        assertTrue(delegate.isFinished(taskInfo))
        assert(result.isSuccessful)
      }
    }
  }
  context("utils module: miscUtils") {
    lateinit var test: String
    lateinit var duration: Duration

    should("run a block of code after the debounce action") {

      withContext(Dispatchers.IO) {
        val started = now()
        debounce(500) {
          duration = Duration.between(started, now())
          test = "debounce block"
        }.invoke()
        Thread.sleep(1000)
      }

      assertSoftly {
        test shouldBe "debounce block"
        duration.toMillis() shouldBeGreaterThanOrEqual 500
      }
    }

    should("return a human readable date format given valid LocalDateTime instance") {
      //given
      val actualLocalDate = LocalDateTime.of(2023, 12, 30, 10, 0, 0)
      val expectedString = "30 DECEMBER 10:00:00"
      //when
      val actualString = actualLocalDate.toHumanReadableFormat()
      //then
      assertSoftly {
        actualString shouldBe expectedString
      }
    }

    should("return this list cleared and one new element added, given the input list and another list") {
      //given
      val receiver = mutableListOf("AAA", "BBB", "CCC")
      val another = listOf("DDD")
      val expectedList = listOf("DDD")
      //when
      receiver.clearAndMergeWith(another)
      //then
      assertSoftly {
        receiver shouldContainExactly expectedList
      }
    }

    should("return this list cleared and new sort key added, given the input list and TYPED sort key to add") {
      //given
      val receiver = mutableListOf(SortQueryKeys.JOB_NAME, SortQueryKeys.DESCENDING)
      val toAdd = SortQueryKeys.JOB_STATUS
      val expectedList = listOf(SortQueryKeys.DESCENDING, SortQueryKeys.JOB_STATUS)
      //when
      receiver.clearOldKeysAndAddNew(toAdd)
      //then
      assertSoftly {
        receiver shouldContainExactly expectedList
      }
    }

    should("return this list cleared and new sort key added, given the input list and ORDERING sort key to add") {
      //given
      val receiver = mutableListOf(SortQueryKeys.JOB_NAME, SortQueryKeys.DESCENDING)
      val toAdd = SortQueryKeys.ASCENDING
      val expectedList = listOf(SortQueryKeys.JOB_NAME, SortQueryKeys.ASCENDING)
      //when
      receiver.clearOldKeysAndAddNew(toAdd)
      //then
      assertSoftly {
        receiver shouldContainExactly expectedList
      }
    }

    should("return this without modifications, given the input list and null key to add") {
      //given
      val receiver = mutableListOf(SortQueryKeys.JOB_NAME, SortQueryKeys.DESCENDING)
      val toAdd = null
      val expectedList = listOf(SortQueryKeys.JOB_NAME, SortQueryKeys.DESCENDING)
      //when
      receiver.clearOldKeysAndAddNew(toAdd)
      //then
      assertSoftly {
        receiver shouldContainExactly expectedList
      }
    }
  }
  context("validateTsoSessionName") {
    val jTextField = JTextField()
    val crudableMock = spyk(makeCrudableWithoutListeners(false) { ConfigStateV2() })

    should("validate TSO session name when there are no other sessions") {
      jTextField.text = "name"
      val initialName = "initialName"

      every { crudableMock.getAll(TSOSessionConfig::class.java) } returns Stream.of()

      val actual = validateTsoSessionName(jTextField, initialName, crudableMock)
      val expected = null

      assertSoftly {
        actual shouldBe expected
      }
    }
    should("validate TSO session name when there are other sessions and the name is unique") {
      jTextField.text = "name"
      val initialName = "initialName"

      val tsoSessionConfig = TSOSessionConfig()
      tsoSessionConfig.name = "tsoSessionName"
      every { crudableMock.getAll(TSOSessionConfig::class.java) } returns Stream.of(tsoSessionConfig)

      val actual = validateTsoSessionName(jTextField, initialName, crudableMock)
      val expected = null

      assertSoftly {
        actual shouldBe expected
      }
    }
    should("validate TSO session name when there are other sessions and the name is not unique") {
      jTextField.text = "name"
      val initialName = "initialName"

      val tsoSessionConfig = TSOSessionConfig()
      tsoSessionConfig.name = "name"
      every { crudableMock.getAll(TSOSessionConfig::class.java) } returns Stream.of(tsoSessionConfig)

      val actual = validateTsoSessionName(jTextField, initialName, crudableMock)
      val expected = ValidationInfo(
        "You must provide unique TSO session name. TSO session \"${jTextField.text}\" already exists.",
        jTextField
      )

      assertSoftly {
        actual shouldBe expected
      }
    }
    should("validate TSO session name when there are other sessions and the name is not unique but this name is ignored") {
      jTextField.text = "name"
      val initialName = "name"

      val tsoSessionConfig = TSOSessionConfig()
      tsoSessionConfig.name = "name"
      every { crudableMock.getAll(TSOSessionConfig::class.java) } returns Stream.of(tsoSessionConfig)

      val actual = validateTsoSessionName(jTextField, initialName, crudableMock)
      val expected = null

      assertSoftly {
        actual shouldBe expected
      }
    }
    should("validate TSO session name when there are no other sessions and there is no ignore value") {
      jTextField.text = "name"

      every { crudableMock.getAll(TSOSessionConfig::class.java) } returns Stream.of()

      val actual = validateTsoSessionName(component = jTextField, crudable = crudableMock)
      val expected = null

      assertSoftly {
        actual shouldBe expected
      }
    }
  }
  context("validateConnectionSelection") {
    val comboBox = ComboBox<ConnectionConfig>()
    val connectionConfig = ConnectionConfig()
    comboBox.model = CollectionComboBoxModel(listOf(connectionConfig))

    should("validate connection selection if selected") {
      comboBox.selectedItem = connectionConfig

      val actual = validateConnectionSelection(comboBox)
      val expected = null

      assertSoftly {
        actual shouldBe expected
      }
    }
    should("validate connection selection if not selected") {
      comboBox.selectedItem = null

      val actual = validateConnectionSelection(comboBox)
      val expected = ValidationInfo("You must provide a connection", comboBox)

      assertSoftly {
        actual shouldBe expected
      }
    }
  }
  context("validateTsoSessionSelection") {
    val comboBox = ComboBox<TSOSessionConfig>()
    val tsoSessionConfig = TSOSessionConfig()
    comboBox.model = CollectionComboBoxModel(listOf(tsoSessionConfig))

    val crudableMock = mockk<Crudable>()

    should("validate TSO session selection if selected and connection config exists") {
      comboBox.selectedItem = tsoSessionConfig

      every {
        crudableMock.getByUniqueKey(ConnectionConfig::class.java, any<String>())
      } returns Optional.of(ConnectionConfig())

      val actual = validateTsoSessionSelection(comboBox, crudableMock)
      val expected = null

      assertSoftly {
        actual shouldBe expected
      }
    }
    should("validate TSO session selection if selected and connection config does not exist") {
      comboBox.selectedItem = tsoSessionConfig

      every {
        crudableMock.getByUniqueKey(ConnectionConfig::class.java, any<String>())
      } returns Optional.empty()

      val actual = validateTsoSessionSelection(comboBox, crudableMock)
      val expected = ValidationInfo("TSO session must contain a connection", comboBox)

      assertSoftly {
        actual shouldBe expected
      }
    }
    should("validate TSO session selection if not selected") {
      comboBox.selectedItem = null

      val actual = validateTsoSessionSelection(comboBox, crudableMock)
      val expected = ValidationInfo("You must provide a TSO session", comboBox)

      assertSoftly {
        actual shouldBe expected
      }
    }
  }
})
