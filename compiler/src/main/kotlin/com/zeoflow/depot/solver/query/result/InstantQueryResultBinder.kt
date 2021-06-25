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
import com.zeoflow.depot.writer.DaoWriter
import com.squareup.javapoet.FieldSpec

/**
 * Instantly runs and returns the query.
 */
class InstantQueryResultBinder(adapter: QueryResultAdapter?) : QueryResultBinder(adapter) {
    override fun convertAndReturn(
        depotSQLiteQueryVar: String,
        canReleaseQuery: Boolean,
        dbField: FieldSpec,
        inTransaction: Boolean,
        scope: CodeGenScope
    ) {
        scope.builder().apply {
            addStatement("$N.assertNotSuspendingTransaction()", DaoWriter.dbField)
        }
        val transactionWrapper = if (inTransaction) {
            scope.builder().transactionWrapper(dbField)
        } else {
            null
        }
        transactionWrapper?.beginTransactionWithControlFlow()
        scope.builder().apply {
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
                adapter?.convert(outVar, cursorVar, scope)
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
