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

package com.zeoflow.depot.solver.shortcut.result

import com.zeoflow.depot.ext.KotlinTypeNames
import com.zeoflow.depot.ext.L
import com.zeoflow.depot.ext.N
import com.zeoflow.depot.ext.T
import com.zeoflow.depot.compiler.processing.XType
import com.zeoflow.depot.compiler.processing.isInt
import com.zeoflow.depot.compiler.processing.isKotlinUnit
import com.zeoflow.depot.compiler.processing.isVoid
import com.zeoflow.depot.compiler.processing.isVoidObject
import com.zeoflow.depot.solver.CodeGenScope
import com.zeoflow.depot.vo.ShortcutQueryParameter
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec

/**
 * Class that knows how to generate a delete or update method body.
 */
class DeleteOrUpdateMethodAdapter private constructor(private val returnType: XType) {
    companion object {
        fun create(returnType: XType): DeleteOrUpdateMethodAdapter? {
            if (isDeleteOrUpdateValid(returnType)) {
                return DeleteOrUpdateMethodAdapter(returnType)
            }
            return null
        }

        private fun isDeleteOrUpdateValid(returnType: XType): Boolean {
            return returnType.isVoid() ||
                returnType.isInt() ||
                returnType.isVoidObject() ||
                returnType.isKotlinUnit()
        }
    }

    fun createDeleteOrUpdateMethodBody(
        parameters: List<ShortcutQueryParameter>,
        adapters: Map<String, Pair<FieldSpec, TypeSpec>>,
        dbField: FieldSpec,
        scope: CodeGenScope
    ) {
        val resultVar = if (hasResultValue(returnType)) {
            scope.getTmpVar("_total")
        } else {
            null
        }
        scope.builder().apply {
            if (resultVar != null) {
                addStatement("$T $L = 0", TypeName.INT, resultVar)
            }
            addStatement("$N.beginTransaction()", dbField)
            beginControlFlow("try").apply {
                parameters.forEach { param ->
                    val adapter = adapters[param.name]?.first
                    addStatement(
                        "$L$N.$L($L)",
                        if (resultVar == null) "" else "$resultVar +=",
                        adapter, param.handleMethodName(), param.name
                    )
                }
                addStatement("$N.setTransactionSuccessful()", dbField)
                if (resultVar != null) {
                    addStatement("return $L", resultVar)
                } else if (hasNullReturn(returnType)) {
                    addStatement("return null")
                } else if (hasUnitReturn(returnType)) {
                    addStatement("return $T.INSTANCE", KotlinTypeNames.UNIT)
                }
            }
            nextControlFlow("finally").apply {
                addStatement("$N.endTransaction()", dbField)
            }
            endControlFlow()
        }
    }

    private fun hasResultValue(returnType: XType): Boolean {
        return !(
            returnType.isVoid() ||
                returnType.isVoidObject() ||
                returnType.isKotlinUnit()
            )
    }

    private fun hasNullReturn(returnType: XType) = returnType.isVoidObject()

    private fun hasUnitReturn(returnType: XType) = returnType.isKotlinUnit()
}