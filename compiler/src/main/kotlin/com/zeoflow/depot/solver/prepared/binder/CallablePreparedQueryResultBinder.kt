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

package com.zeoflow.depot.solver.prepared.binder

import com.zeoflow.depot.ext.CallableTypeSpecBuilder
import com.zeoflow.depot.compiler.processing.XType
import com.zeoflow.depot.solver.CodeGenScope
import com.zeoflow.depot.solver.prepared.result.PreparedQueryResultAdapter
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeSpec

/**
 * Binder for deferred queries.
 *
 * This binder will create a Callable implementation that delegates to the
 * [PreparedQueryResultAdapter]. Usage of the Callable impl is then delegate to the [addStmntBlock]
 * function.
 */
class CallablePreparedQueryResultBinder private constructor(
    val returnType: XType,
    val addStmntBlock: CodeBlock.Builder.(callableImpl: TypeSpec, dbField: FieldSpec) -> Unit,
    adapter: PreparedQueryResultAdapter?
) : PreparedQueryResultBinder(adapter) {

    companion object {
        fun createPreparedBinder(
            returnType: XType,
            adapter: PreparedQueryResultAdapter?,
            addCodeBlock: CodeBlock.Builder.(callableImpl: TypeSpec, dbField: FieldSpec) -> Unit
        ) = CallablePreparedQueryResultBinder(returnType, addCodeBlock, adapter)
    }

    override fun executeAndReturn(
        prepareQueryStmtBlock: CodeGenScope.() -> String,
        preparedStmtField: String?,
        dbField: FieldSpec,
        scope: CodeGenScope
    ) {
        val binderScope = scope.fork()
        val callableImpl = CallableTypeSpecBuilder(returnType.typeName) {
            adapter?.executeAndReturn(
                binderScope.prepareQueryStmtBlock(),
                preparedStmtField,
                dbField,
                binderScope
            )
            addCode(binderScope.generate())
        }.build()

        scope.builder().apply {
            addStmntBlock(callableImpl, dbField)
        }
    }
}