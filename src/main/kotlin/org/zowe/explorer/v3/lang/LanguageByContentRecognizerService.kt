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
 *   IBA Group
 *   Zowe Community
 *   Artemiy Vishnyakov
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.v3.lang

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.ZipUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.tensorflow.SavedModelBundle
import org.tensorflow.ndarray.NdArrays
import org.tensorflow.ndarray.Shape
import org.tensorflow.types.TFloat32
import org.tensorflow.types.TString
import org.zowe.explorer.telemetry.NotificationsService
import kotlin.io.path.exists

/**
 * Language by content recognizer service to define the virtual file's content language if it is possible.
 * Uses <a href="https://github.com/yoeo/guesslang">Guesslang machine learning model</a> to recognize the language.
 * Unused is suppressed as the service is used outside the plugin
 */
@Suppress("unused")
@Service
class LanguageByContentRecognizerService {
  companion object {
    @JvmStatic
    fun getService(): LanguageByContentRecognizerService = service()

    private const val DEFAULT_MAX_CONTENT_SIZE = 100000
    private const val DEFAULT_MIN_CONTENT_SIZE = 20
    private const val MODEL_ZIP_NAME = "guesslang-model"
    private const val MODEL_VERSION = "2.2.1"
  }

  private val minContentSize: Int = DEFAULT_MIN_CONTENT_SIZE
  private val maxContentSize: Int = DEFAULT_MAX_CONTENT_SIZE
  private val normalizeNewline: Boolean = true

  private val model: SavedModelBundle? by lazy { loadModel() }

  /**
   * Load model to recognize content's language with
   * @return the [SavedModelBundle] instance or null if the model loading is failed
   */
  private fun loadModel(): SavedModelBundle? {
    return try {
      val modelPlacingRootPath = PathManager.getConfigDir().resolve(MODEL_ZIP_NAME)
      val activeClassLoader = this::class.java.classLoader
      runBlocking {
        withContext(Dispatchers.IO) {
          if (!modelPlacingRootPath.exists()) {
            val modelNameWithVersion = "${MODEL_ZIP_NAME}-${MODEL_VERSION}"
            val modelWithExt = "$modelNameWithVersion.zip"
            val modelTempFile = FileUtil.createTempFile(MODEL_ZIP_NAME, ".zip")
            val modelResource = activeClassLoader
              .getResourceAsStream(modelWithExt)
              ?: throw Exception("No $modelWithExt found")
            modelTempFile.writeBytes(modelResource.readAllBytes())
            ZipUtil.extract(modelTempFile.toPath(), modelPlacingRootPath, null)
          }
        }
      }
      SavedModelBundle.load(modelPlacingRootPath.toAbsolutePath().toString())
    } catch (e: Exception) {
      NotificationsService.errorNotification(e)
      null
    }
  }

  /**
   * Run model on the provided content to define the content's language.
   * If the model is not loaded, it will return a map of an empty string to a 0 probability
   * @param content the content to define the language for
   * @return map of language names to probabilities based on the provided content
   */
  private fun runModel(content: String): Map<String, Float> {
    if (content.length < minContentSize) return emptyMap()

    var processedContent = content
    if (processedContent.length >= maxContentSize) {
      processedContent = processedContent.substring(0, maxContentSize)
    }
    if (normalizeNewline) {
      processedContent = processedContent.replace("\r\n", "\n")
    }

    var results: Map<String, Float> = mapOf("" to 0.0f)
    // Load model
    model.use {
      // Call the TensorFlow model
      it?.session()
        ?.use { session ->
          val ndArray = NdArrays.ofObjects(String::class.javaObjectType, Shape.of(1))
          ndArray.setObject(processedContent, 0L)
          val tensor = TString.tensorOf(ndArray)
          val output = session.runner()
            .feed("Placeholder:0", tensor)
            .fetch("head/predictions/probabilities:0")
            .fetch("head/Tile:0")
            .run()
          val probabilitiesTensor = output[0] as TFloat32
          val probabilities = probabilitiesTensor.scalars().map { prob -> prob.getFloat() }
          val labelsTensor = output[1] as TString
          val labels = labelsTensor.scalars().map { lang -> lang.getObject() }
          results = labels.zip(probabilities).toMap()
        }
    }
    return results.toSortedMap { currLang, nextLang ->
      if ((results[currLang] ?: 0.0f) > (results[nextLang] ?: 0.0f)) -1 else 1
    }
  }

  /**
   * Recognize the [virtualFile]'s content language if it is cached in the plugin's storage
   * @param virtualFile the virtual file to find the cached content by
   * @return a recognized language if succeeded or an empty string otherwise
   */
  fun getFileContentLanguage(virtualFile: VirtualFile): String {
    val fileText = FileDocumentManager.getInstance()
      .getCachedDocument(virtualFile)
      ?.text
      ?: ""
    return if (fileText != "") runModel(fileText).keys.first() else ""
  }
}
