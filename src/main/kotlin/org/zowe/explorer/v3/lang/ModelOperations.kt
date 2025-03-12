/*
 * Copyright (c) 2024 IBA Group.
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

package org.zowe.explorer.v3.lang

//import com.intellij.lang.documentation.ide.impl.browseAbsolute
//import com.intellij.util.indexing.diagnostic.dump.paths.PortableFilePath
//import kotlinx.html.InputType
import org.tensorflow.SavedModelBundle
import org.tensorflow.ndarray.NdArrays
import org.tensorflow.ndarray.Shape
import org.tensorflow.types.TFloat32
import org.tensorflow.types.TString
//import kotlin.io.path.absolutePathString
//import java.nio.file.Paths
//import java.io.File
import org.zowe.explorer.v3.lang.CobolDependencyManager

// TODO: doc
class ModelOperations(
  private val minContentSize: Int = DEFAULT_MIN_CONTENT_SIZE,
  private val maxContentSize: Int = DEFAULT_MAX_CONTENT_SIZE,
  private val normalizeNewline: Boolean = true
) {

  companion object {
    private const val DEFAULT_MAX_CONTENT_SIZE = 100000
    private const val DEFAULT_MIN_CONTENT_SIZE = 20
  }

  private val model: SavedModelBundle by lazy { loadModel() }

  private fun loadModel(): SavedModelBundle {
    CobolDependencyManager().init()
    try {
//      var model_file = File("resources/models")
//      println(model_file.absolutePath.toString())
//      return SavedModelBundle.load(model_file.absolutePath.toString())
      // TODO Make path local not global
      val modelFolderPath = "C:/Users/varte/Documents/University/Year 4 Sem. 2/4475/zowe-explorer-intellij/src/main/resources/models"
      return SavedModelBundle.load(modelFolderPath)
    } catch (e: Exception) {
      println("Failed to find model file")
      throw e // TODO Error handling?
    }
  }

  fun runModel(content: String): Map<String, Float> {
    if (content.length < minContentSize) return emptyMap()

    var processedContent = content
    if (processedContent.length >= maxContentSize) {
      processedContent = processedContent.substring(0, maxContentSize)
    }
    if (normalizeNewline) {
      processedContent = processedContent.replace("\r\n", "\n")
    }

    val results: Map<String, Float>
    // Load model
    model.use {
      // Call the TensorFlow model
      it.session().use {
          session ->
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
}
