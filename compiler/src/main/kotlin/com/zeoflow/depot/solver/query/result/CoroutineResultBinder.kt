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
import com.zeoflow.depot.ext.DepotCoroutinesTypeNames
import com.zeoflow.depot.ext.DepotTypeNames
import com.zeoflow.depot.ext.T
import com.zeoflow.depot.compiler.processing.XType
import com.zeoflow.depot.solver.CodeGenScope
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec

/**
 * Binds the result of a of a Kotlin coroutine suspend function.
 */
class CoroutineResultBinder(
    val typeArg: XType,
    private val continuationParamName: String,
    adapter: QueryResultAdapter?
) : QueryResultBinder(adapter) {

    override fun convertAndReturn(
        depotSQLiteQueryVar: String,
        canReleaseQuery: Boolean,
        dbField: FieldSpec,
        inTransaction: Boolean,
        scope: CodeGenScope
    ) {
        val cancellationSignalVar = scope.getTmpVar("_cancellationSignal")
        scope.builder().addStatement(
            "final $T $L = $T.createCancellationSignal()",
            AndroidTypeNames.CANCELLATION_SIGNAL,
            cancellationSignalVar,
            DepotTypeNames.DB_UTIL
        )

        val callableImpl = CallableTypeSpecBuilder(typeArg.typeName) {
            createRunQueryAndReturnStatements(
                builder = this,
                depotSQLiteQueryVar = depotSQLiteQueryVar,
                canReleaseQuery = canReleaseQuery,
                dbField = dbField,
                inTransaction = inTransaction,
                scope = scope,
                cancellationSignalVar = "null"
            )
        }.build()

        scope.builder().apply {
            addStatement(
                "return $T.execute($N, $L, $L, $L, $N)",
                DepotCoroutinesTypeNames.COROUTINES_DEPOT,
                dbField,
                if (inTransaction) "true" else "false",
                cancellationSignalVar,
                callableImpl,
                continuationParamName
            )
        }
    }

    private fun createRunQueryAndReturnStatements(
        builder: MethodSpec.Builder,
        depotSQLiteQueryVar: String,
        canReleaseQuery: Boolean,
        dbField: FieldSpec,
        inTransaction: Boolean,
        scope: CodeGenScope,
        cancellationSignalVar: String
    ) {
        val transactionWrapper = if (inTransaction) {
            builder.transactionWrapper(dbField)
        } else {
            null
        }
        val shouldCopyCursor = adapter?.shouldCopyCursor() == true
        val outVar = scope.getTmpVar("_result")
        val cursorVar = scope.getTmpVar("_cursor")
        transactionWrapper?.beginTransactionWithControlFlow()
        builder.apply {
            addStatement(
                "final $T $L = $T.query($N, $L, $L, $L)",
                AndroidTypeNames.CURSOR,
                cursorVar,
                DepotTypeNames.DB_UTIL,
                dbField,
                depotSQLiteQueryVar,
                if (shouldCopyCursor) "true" else "false",
                cancellationSignalVar
            )
            beginControlFlow("try").apply {
                val adapterScope = scope.fork()
                adapter?.convert(outVar, cursorVar, adapterScope)
                addCode(adapterScope.builder().build())
                transactionWrapper?.commitTransaction()
                addStatement("return $L", outVar)
            }
            nextControlFlow("finally").apply {
                addStatement("$L.close()", cursorVar)
                if (canReleaseQuery) {
                    addStatement("$L.release()", depotSQLiteQueryVar)
                }
            }
            endControlFlow()
        }
        transactionWrapper?.endTransactionWithControlFlow()
    }
}