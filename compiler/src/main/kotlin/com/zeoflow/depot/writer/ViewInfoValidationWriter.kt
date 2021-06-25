/*
 * Copyright (C) 2021 ZeoFlow SRL
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zeoflow.depot.writer

import com.zeoflow.depot.ext.L
import com.zeoflow.depot.ext.N
import com.zeoflow.depot.ext.DepotTypeNames
import com.zeoflow.depot.ext.S
import com.zeoflow.depot.ext.T
import com.zeoflow.depot.vo.DatabaseView
import com.squareup.javapoet.ParameterSpec
import stripNonJava
import java.util.Locale

class ViewInfoValidationWriter(val view: DatabaseView) : ValidationWriter() {

    override fun write(dbParam: ParameterSpec, scope: CountingCodeGenScope) {
        val suffix = view.viewName.stripNonJava().capitalize(Locale.US)
        scope.builder().apply {
            val expectedInfoVar = scope.getTmpVar("_info$suffix")
            addStatement(
                "final $T $L = new $T($S, $S)",
                DepotTypeNames.VIEW_INFO, expectedInfoVar, DepotTypeNames.VIEW_INFO,
                view.viewName, view.createViewQuery
            )

            val existingVar = scope.getTmpVar("_existing$suffix")
            addStatement(
                "final $T $L = $T.read($N, $S)",
                DepotTypeNames.VIEW_INFO, existingVar, DepotTypeNames.VIEW_INFO,
                dbParam, view.viewName
            )

            beginControlFlow("if (! $L.equals($L))", expectedInfoVar, existingVar).apply {
                addStatement(
                    "return new $T(false, $S + $L + $S + $L)",
                    DepotTypeNames.OPEN_HELPER_VALIDATION_RESULT,
                    "${view.viewName}(${view.element.qualifiedName}).\n Expected:\n",
                    expectedInfoVar, "\n Found:\n", existingVar
                )
            }
            endControlFlow()
        }
    }
}
