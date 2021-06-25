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
import com.zeoflow.depot.ext.L
import com.zeoflow.depot.ext.N
import com.zeoflow.depot.ext.DepotTypeNames
import com.zeoflow.depot.ext.T
import com.zeoflow.depot.solver.CodeGenScope
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import javax.lang.model.element.Modifier

/**
 * Base class for query result binders that observe the database. It includes common functionality
 * like creating a finalizer to release the query or creating the actual adapter call code.
 */
abstract class BaseObservableQueryResultBinder(adapter: QueryResultAdapter?) :
    QueryResultBinder(adapter) {

    protected fun createFinalizeMethod(depotSQLiteQueryVar: String): MethodSpec {
        return MethodSpec.methodBuilder("finalize").apply {
            addModifiers(Modifier.PROTECTED)
            addAnnotation(Override::class.java)
            addStatement("$L.release()", depotSQLiteQueryVar)
        }.build()
    }

    protected fun createRunQueryAndReturnStatements(
        builder: MethodSpec.Builder,
        depotSQLiteQueryVar: String,
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
            }
            endControlFlow()
        }
        transactionWrapper?.endTransactionWithControlFlow()
    }
}
