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

package eu.ibagroup.formainframe.editor.inspection

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.InspectionsBundle
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.ide.DataManager
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.lang.properties.charset.Native2AsciiCharset
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.toArray
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.utils.createCharsetsActionGroup
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CoderResult
import java.nio.charset.CodingErrorAction

/** Class that checks the contents of the editor for compatibility with the selected encoding. */
class MFLossyEncodingInspection : LocalInspectionTool() {

  override fun getShortName(): String {
    return "MFLossyEncoding"
  }

  override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
    if (InjectedLanguageManager.getInstance(file.project).isInjectedFragment(file)) return null
    if (!file.isPhysical) return null
    val viewProvider = file.viewProvider
    if (viewProvider.baseLanguage !== file.language) return null
    val virtualFile = file.virtualFile ?: return null
    if (virtualFile !is MFVirtualFile) return null
    val text = viewProvider.contents
    val charset = LoadTextUtil.extractCharsetFromFileContent(file.project, virtualFile, text)

    // no sense in checking transparently decoded file: all characters there are already safely encoded
    if (charset is Native2AsciiCharset) return null

    val descriptors: MutableList<ProblemDescriptor> = mutableListOf()
    checkIfCharactersWillBeLostAfterSave(file, manager, isOnTheFly, text, charset, descriptors)
    return descriptors.toArray(ProblemDescriptor.EMPTY_ARRAY)
  }

  /** Checks if the characters entered by the user in the editor will be lost after saving the file. */
  private fun checkIfCharactersWillBeLostAfterSave(
    file: PsiFile,
    manager: InspectionManager,
    isOnTheFly: Boolean,
    text: CharSequence,
    charset: Charset,
    descriptors: MutableList<ProblemDescriptor>
  ) {
    val buffer = CharBuffer.wrap(text)
    val textLength = text.length
    val back = CharBuffer.allocate(textLength) // must be enough, error otherwise
    val outRef = Ref.create<ByteBuffer>()
    val attributes = DataOpsManager.getService().tryToGetAttributes(file.virtualFile)

    // do not report too many errors
    var pos = 0
    var errorCount = 0
    while (pos < textLength && errorCount < 200) {
      var errRange = nextUnmappable(buffer, pos, outRef, back, charset) ?: break
      val lastDescriptor = ContainerUtil.getLastItem(descriptors)
      if (lastDescriptor != null && lastDescriptor.textRangeInElement.endOffset == errRange.startOffset) {
        // combine two adjacent descriptors
        errRange = lastDescriptor.textRangeInElement.union(errRange)
        descriptors.removeAt(descriptors.size - 1)
      }
      val message = InspectionsBundle.message("unsupported.character.for.the.charset", charset)
      val descriptor = if (attributes is RemoteUssAttributes) manager.createProblemDescriptor(
        file, errRange, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly,
        ChangeEncodingFix(file, attributes)
      )
      else manager.createProblemDescriptor(
        file, errRange, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly
      )
      descriptors.add(descriptor)
      pos = errRange.endOffset
      errorCount++
    }
  }

  /**
   * Finds the next unmappable piece of content.
   * Range of the characters either failed to be encoded to bytes or
   * failed to be decoded back or decoded to the chars different from the original.
   * @return null if OK.
   */
  private fun nextUnmappable(
    input: CharBuffer,
    position: Int,
    outRef: Ref<ByteBuffer>,
    back: CharBuffer,
    charset: Charset
  ): TextRange? {
    val encoder = charset.newEncoder()
      .onUnmappableCharacter(CodingErrorAction.REPORT)
      .onMalformedInput(CodingErrorAction.REPORT)
    val textLength = input.limit() - position
    var output = outRef.get()
    if (output == null) {
      outRef.set(ByteBuffer.allocate((encoder.averageBytesPerChar() * textLength).toInt()).also { output = it })
    }
    output.rewind()
    output.limit(output.capacity())
    input.rewind()
    input.position(position)
    var coderResult: CoderResult
    while (true) {
      coderResult = if (input.hasRemaining()) encoder.encode(input, output, true) else CoderResult.UNDERFLOW
      if (coderResult.isUnderflow) {
        coderResult = encoder.flush(output)
      }
      if (!coderResult.isOverflow) {
        break
      }
      val n = 3 * output.capacity() / 2 + 1
      val tmp = ByteBuffer.allocate(n)
      output.flip()
      tmp.put(output)
      outRef.set(tmp.also { output = it })
    }
    if (coderResult.isError) {
      return TextRange.from(input.position(), coderResult.length())
    }
    // phew, encoded successfully. now check if we can decode it back with char-to-char precision
    val outLength = output.position()
    val decoder = charset.newDecoder()
      .onUnmappableCharacter(CodingErrorAction.REPORT)
      .onMalformedInput(CodingErrorAction.REPORT)
    output.rewind()
    output.limit(outLength)
    back.rewind()
    val decoderResult = decoder.decode(output, back, true)
    if (decoderResult.isError) {
      return TextRange.from(back.position(), decoderResult.length())
    }
    if (back.position() != textLength) {
      return TextRange.from(textLength.coerceAtMost(back.position()), 1)
    }
    // ok, we decoded it back to string. now compare if the strings are identical
    input.rewind()
    input.position(position)
    back.rewind()
    val len = StringUtil.commonPrefixLength(input, back)
    return if (len == textLength) null else TextRange.from(len, 1)
    // let's report only the first diff char
  }

  /** Provides change encoding fix to save file correctly. */
  private open class ChangeEncodingFix(
    file: PsiFile,
    val attributes: RemoteUssAttributes
  ) : LocalQuickFixAndIntentionActionOnPsiElement(file) {
    override fun startInWriteAction(): Boolean {
      return false
    }

    override fun getText(): String {
      return familyName
    }

    override fun getFamilyName(): String {
      return InspectionsBundle.message("change.encoding.fix.family.name")
    }

    override fun invoke(
      project: Project,
      file: PsiFile,
      editor: Editor?,
      startElement: PsiElement,
      endElement: PsiElement
    ) {
      val virtualFile = file.virtualFile
      val dataContext = DataManager.getInstance().getDataContext(editor?.component)
      val group = createCharsetsActionGroup(virtualFile, attributes)
      val popup = JBPopupFactory.getInstance().createActionGroupPopup(
        "", group, dataContext, JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false
      )
      popup.showInBestPositionFor(dataContext)
    }
  }
}