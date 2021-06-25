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

import com.zeoflow.depot.ext.AndroidTypeNames
import com.zeoflow.depot.ext.CallableTypeSpecBuilder
import com.zeoflow.depot.ext.L
import com.zeoflow.depot.ext.N
import com.zeoflow.depot.ext.DepotTypeNames
import com.zeoflow.depot.ext.S
import com.zeoflow.depot.ext.T
import com.zeoflow.depot.compiler.processing.XType
import com.zeoflow.depot.solver.CodeGenScope
import com.zeoflow.depot.solver.RxType
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import javax.lang.model.element.Modifier

/**
 * Generic Result binder for Rx classes that accept a callable.
 */
internal class RxCallableQueryResultBinder(
    private val rxType: RxType,
    val typeArg: XType,
    adapter: QueryResultAdapter?
) : QueryResultBinder(adapter) {
    override fun convertAndReturn(
        depotSQLiteQueryVar: String,
        canReleaseQuery: Boolean,
        dbField: FieldSpec,
        inTransaction: Boolean,
        scope: CodeGenScope
    ) {
        val callable = CallableTypeSpecBuilder(typeArg.typeName) {
            fillInCallMethod(
                depotSQLiteQueryVar = depotSQLiteQueryVar,
                dbField = dbField,
                inTransaction = inTransaction,
                scope = scope
            )
        }.apply {
            if (canReleaseQuery) {
                addMethod(createFinalizeMethod(depotSQLiteQueryVar))
            }
        }.build()
        scope.builder().apply {
            if (rxType.isSingle()) {
                addStatement("return $T.createSingle($L)", rxType.version.rxDepotClassName, callable)
            } else {
                addStatement("return $T.fromCallable($L)", rxType.className, callable)
            }
        }
    }

    private fun MethodSpec.Builder.fillInCallMethod(
        depotSQLiteQueryVar: String,
        dbField: FieldSpec,
        inTransaction: Boolean,
        scope: CodeGenScope
    ) {
        val adapterScope = scope.fork()
        val transactionWrapper = if (inTransaction) {
            transactionWrapper(dbField)
        } else {
            null
        }
        transactionWrapper?.beginTransactionWithControlFlow()
        val shouldCopyCursor = adapter?.shouldCopyCursor() == true
        val outVar = scope.getTmpVar("_result")
        val cursorVar = scope.getTmpVar("_cursor")
        addStatement(
            "final $T $L = $T.query($N, $L, $L, $L)",
            AndroidTypeNames.CURSOR,
            cursorVar,
            DepotTypeNames.DB_UTIL,
            dbField,
            depotSQLiteQueryVar,
            if (shouldCopyCursor) "true" else "false",
            "null"
        )
        beginControlFlow("try").apply {
            adapter?.convert(outVar, cursorVar, adapterScope)
            addCode(adapterScope.generate())
            if (!rxType.canBeNull) {
                beginControlFlow("if($L == null)", outVar).apply {
                    addStatement(
                        "throw new $T($S + $L.getSql())",
                        rxType.version.emptyResultExceptionClassName,
                        "Query returned empty result set: ",
                        depotSQLiteQueryVar
                    )
                }
                endControlFlow()
            }
            transactionWrapper?.commitTransaction()
            addStatement("return $L", outVar)
        }
        nextControlFlow("finally").apply {
            addStatement("$L.close()", cursorVar)
        }
        endControlFlow()
        transactionWrapper?.endTransactionWithControlFlow()
    }

    private fun createFinalizeMethod(depotSQLiteQueryVar: String): MethodSpec {
        return MethodSpec.methodBuilder("finalize").apply {
            addModifiers(Modifier.PROTECTED)
            addAnnotation(Override::class.java)
            addStatement("$L.release()", depotSQLiteQueryVar)
        }.build()
    }
}