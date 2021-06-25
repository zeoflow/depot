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

package com.zeoflow.depot.solver.query.result

import com.zeoflow.depot.compiler.processing.XType
import com.zeoflow.depot.ext.GuavaBaseTypeNames
import com.zeoflow.depot.ext.L
import com.zeoflow.depot.ext.T
import com.zeoflow.depot.solver.CodeGenScope
import com.squareup.javapoet.ParameterizedTypeName

/**
 * Wraps a row adapter when there is only 1 item in the result, and the result's outer type is
 * {@link com.google.common.base.Optional}.
 */
class GuavaOptionalQueryResultAdapter(
    private val typeArg: XType,
    private val resultAdapter: SingleEntityQueryResultAdapter
) : QueryResultAdapter(resultAdapter.rowAdapter) {
    override fun convert(
        outVarName: String,
        cursorVarName: String,
        scope: CodeGenScope
    ) {
        scope.builder().apply {
            val valueVarName = scope.getTmpVar("_value")
            resultAdapter.convert(valueVarName, cursorVarName, scope)
            addStatement(
                "final $T $L = $T.fromNullable($L)",
                ParameterizedTypeName.get(GuavaBaseTypeNames.OPTIONAL, typeArg.typeName),
                outVarName,
                GuavaBaseTypeNames.OPTIONAL,
                valueVarName
            )
        }
    }
}
