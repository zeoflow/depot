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

package com.zeoflow.depot.solver.types

import com.zeoflow.depot.compiler.processing.XProcessingEnv
import com.zeoflow.depot.compiler.processing.XType
import com.zeoflow.depot.ext.CommonTypeNames
import com.zeoflow.depot.ext.L
import com.zeoflow.depot.parser.SQLTypeAffinity.TEXT
import com.zeoflow.depot.solver.CodeGenScope

class StringColumnTypeAdapter private constructor(
    out: XType
) : ColumnTypeAdapter(out = out, typeAffinity = TEXT) {
    override fun readFromCursor(
        outVarName: String,
        cursorVarName: String,
        indexVarName: String,
        scope: CodeGenScope
    ) {
        scope.builder().apply {
            // according to docs, getString might throw if the value is null
            // https://developer.android.com/reference/android/database/Cursor#getString(int)
            beginControlFlow("if ($L.isNull($L))", cursorVarName, indexVarName).apply {
                addStatement("$L = null", outVarName)
            }
            nextControlFlow("else").apply {
                addStatement("$L = $L.getString($L)", outVarName, cursorVarName, indexVarName)
            }
            endControlFlow()
        }
    }

    override fun bindToStmt(
        stmtName: String,
        indexVarName: String,
        valueVarName: String,
        scope: CodeGenScope
    ) {
        scope.builder().apply {
            beginControlFlow("if ($L == null)", valueVarName)
                .addStatement("$L.bindNull($L)", stmtName, indexVarName)
            nextControlFlow("else")
                .addStatement("$L.bindString($L, $L)", stmtName, indexVarName, valueVarName)
            endControlFlow()
        }
    }

    companion object {
        fun create(env: XProcessingEnv): List<StringColumnTypeAdapter> {
            val stringType = env.requireType(CommonTypeNames.STRING)
            return if (env.backend == XProcessingEnv.Backend.KSP) {
                listOf(
                    StringColumnTypeAdapter(stringType.makeNonNullable()),
                    StringColumnTypeAdapter(stringType.makeNullable()),
                )
            } else {
                listOf(
                    StringColumnTypeAdapter(stringType)
                )
            }
        }
    }
}
