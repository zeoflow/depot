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
import com.zeoflow.depot.ext.L
import com.zeoflow.depot.ext.T
import com.zeoflow.depot.parser.SQLTypeAffinity
import com.zeoflow.depot.solver.CodeGenScope
import com.squareup.javapoet.TypeName
import java.nio.ByteBuffer

class ByteBufferColumnTypeAdapter private constructor(out: XType) : ColumnTypeAdapter(
    out = out,
    typeAffinity = SQLTypeAffinity.BLOB
) {
    override fun readFromCursor(
        outVarName: String,
        cursorVarName: String,
        indexVarName: String,
        scope: CodeGenScope
    ) {
        scope.builder().apply {
            beginControlFlow("if ($L.isNull($L))", cursorVarName, indexVarName).apply {
                addStatement("$L = null", outVarName)
            }
            nextControlFlow("else").apply {
                addStatement(
                    "$L = $T.wrap($L.getBlob($L))",
                    outVarName, TypeName.get(ByteBuffer::class.java), cursorVarName, indexVarName
                )
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
                .addStatement("$L.bindBlob($L, $L.array())", stmtName, indexVarName, valueVarName)
            endControlFlow()
        }
    }

    companion object {
        fun create(env: XProcessingEnv): List<ByteBufferColumnTypeAdapter> {
            val byteBufferType = env.requireType("java.nio.ByteBuffer")
            return if (env.backend == XProcessingEnv.Backend.KSP) {
                listOf(
                    ByteBufferColumnTypeAdapter(byteBufferType.makeNonNullable()),
                    ByteBufferColumnTypeAdapter(byteBufferType.makeNullable()),
                )
            } else {
                listOf(
                    ByteBufferColumnTypeAdapter(byteBufferType)
                )
            }
        }
    }
}
