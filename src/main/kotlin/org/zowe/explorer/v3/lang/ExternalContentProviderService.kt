/*
 * Copyright (c) 2025 IBA Group.
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

package org.zowe.explorer.v3.lang;

import com.intellij.openapi.components.Service
import org.zowe.explorer.v3.lang.ModelOperations

@Service
class ExternalContentProviderService{ // extends Service?
    fun getPrediction(content: String): String {
        // Do the stuff in Zowe Explorer to define the prediction by the provided content
        val predictions = ModelOperations().runModel(content)
        var responseLanguage = predictions.keys.first()
        val throwMessage ="Response body detected as $responseLanguage"

        println(throwMessage)
        if(responseLanguage=="cbl") {
            // call COBOL highlighting API
        }
        return responseLanguage
    }
}