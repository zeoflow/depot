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

import com.zeoflow.depot.ext.CallableTypeSpecBuilder
import com.zeoflow.depot.ext.L
import com.zeoflow.depot.ext.N
import com.zeoflow.depot.ext.T
import com.zeoflow.depot.ext.arrayTypeName
import com.zeoflow.depot.compiler.processing.XType
import com.zeoflow.depot.solver.CodeGenScope
import com.squareup.javapoet.FieldSpec

/**
 * Converts the query into a LiveData and returns it. No query is run until necessary.
 */
class LiveDataQueryResultBinder(
    val typeArg: XType,
    val tableNames: Set<String>,
    adapter: QueryResultAdapter?
) : BaseObservableQueryResultBinder(adapter) {
    @Suppress("JoinDeclarationAndAssignment")
    override fun convertAndReturn(
        depotSQLiteQueryVar: String,
        canReleaseQuery: Boolean,
        dbField: FieldSpec,
        inTransaction: Boolean,
        scope: CodeGenScope
    ) {
        val callableImpl = CallableTypeSpecBuilder(typeArg.typeName) {
            createRunQueryAndReturnStatements(
                builder = this,
                depotSQLiteQueryVar = depotSQLiteQueryVar,
                inTransaction = inTransaction,
                dbField = dbField,
                scope = scope,
                cancellationSignalVar = "null" // LiveData can't be cancelled
            )
        }.apply {
            if (canReleaseQuery) {
                addMethod(createFinalizeMethod(depotSQLiteQueryVar))
            }
        }.build()

        scope.builder().apply {
            val tableNamesList = tableNames.joinToString(",") { "\"$it\"" }
            addStatement(
                "return $N.getInvalidationTracker().createLiveData(new $T{$L}, $L, $L)",
                dbField,
                String::class.arrayTypeName,
                tableNamesList,
                if (inTransaction) "true" else "false",
                callableImpl
            )
        }
    }
}
