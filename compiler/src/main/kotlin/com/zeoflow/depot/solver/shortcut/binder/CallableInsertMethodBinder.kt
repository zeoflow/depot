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

package com.zeoflow.depot.solver.shortcut.binder

import com.zeoflow.depot.ext.CallableTypeSpecBuilder
import com.zeoflow.depot.compiler.processing.XType
import com.zeoflow.depot.solver.CodeGenScope
import com.zeoflow.depot.solver.shortcut.result.InsertMethodAdapter
import com.zeoflow.depot.vo.ShortcutQueryParameter
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeSpec

/**
 * Binder for deferred insert methods.
 *
 * This binder will create a Callable implementation that delegates to the
 * [InsertMethodAdapter]. Usage of the Callable impl is then delegate to the [addStmntBlock]
 * function.
 */
class CallableInsertMethodBinder(
    val typeArg: XType,
    val addStmntBlock: CodeBlock.Builder.(callableImpl: TypeSpec, dbField: FieldSpec) -> Unit,
    adapter: InsertMethodAdapter?
) : InsertMethodBinder(adapter) {

    companion object {
        fun createInsertBinder(
            typeArg: XType,
            adapter: InsertMethodAdapter?,
            addCodeBlock: CodeBlock.Builder.(callableImpl: TypeSpec, dbField: FieldSpec) -> Unit
        ) = CallableInsertMethodBinder(typeArg, addCodeBlock, adapter)
    }

    override fun convertAndReturn(
        parameters: List<ShortcutQueryParameter>,
        insertionAdapters: Map<String, Pair<FieldSpec, TypeSpec>>,
        dbField: FieldSpec,
        scope: CodeGenScope
    ) {
        val adapterScope = scope.fork()
        val callableImpl = CallableTypeSpecBuilder(typeArg.typeName) {
            adapter?.createInsertionMethodBody(
                parameters = parameters,
                insertionAdapters = insertionAdapters,
                dbField = dbField,
                scope = adapterScope
            )
            addCode(adapterScope.generate())
        }.build()

        scope.builder().apply {
            addStmntBlock(callableImpl, dbField)
        }
    }
}