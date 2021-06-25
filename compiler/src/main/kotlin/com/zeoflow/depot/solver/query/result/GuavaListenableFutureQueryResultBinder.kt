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
import com.zeoflow.depot.ext.DepotGuavaTypeNames
import com.zeoflow.depot.ext.DepotTypeNames
import com.zeoflow.depot.ext.T
import com.zeoflow.depot.compiler.processing.XType
import com.zeoflow.depot.solver.CodeGenScope
import com.squareup.javapoet.FieldSpec

/**
 * A ResultBinder that emits a ListenableFuture<T> where T is the input {@code typeArg}.
 *
 * <p>The Future runs on the background thread Executor.
 */
class GuavaListenableFutureQueryResultBinder(
    val typeArg: XType,
    adapter: QueryResultAdapter?
) : BaseObservableQueryResultBinder(adapter) {

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

        // Callable<T> // Note that this callable does not release the query object.
        val callableImpl = CallableTypeSpecBuilder(typeArg.typeName) {
            createRunQueryAndReturnStatements(
                builder = this,
                depotSQLiteQueryVar = depotSQLiteQueryVar,
                dbField = dbField,
                inTransaction = inTransaction,
                scope = scope,
                cancellationSignalVar = cancellationSignalVar
            )
        }.build()

        scope.builder().apply {
            addStatement(
                "return $T.createListenableFuture($N, $L, $L, $L, $L, $L)",
                DepotGuavaTypeNames.GUAVA_DEPOT,
                dbField,
                if (inTransaction) "true" else "false",
                callableImpl,
                depotSQLiteQueryVar,
                canReleaseQuery,
                cancellationSignalVar
            )
        }
    }
}
