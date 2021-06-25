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

package com.zeoflow.depot.solver.transaction.binder

import com.zeoflow.depot.ext.Function1TypeSpecBuilder
import com.zeoflow.depot.ext.KotlinTypeNames.CONTINUATION
import com.zeoflow.depot.ext.L
import com.zeoflow.depot.ext.N
import com.zeoflow.depot.ext.DepotTypeNames.DEPOT_DB_KT
import com.zeoflow.depot.ext.T
import com.zeoflow.depot.compiler.processing.XType
import com.zeoflow.depot.solver.CodeGenScope
import com.zeoflow.depot.solver.transaction.result.TransactionMethodAdapter
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.WildcardTypeName

/**
 * Binder that knows how to write suspending transaction wrapper methods.
 */
class CoroutineTransactionMethodBinder(
    adapter: TransactionMethodAdapter,
    private val continuationParamName: String
) : TransactionMethodBinder(adapter) {
    override fun executeAndReturn(
        returnType: XType,
        parameterNames: List<String>,
        daoName: ClassName,
        daoImplName: ClassName,
        dbField: FieldSpec,
        scope: CodeGenScope
    ) {
        val innerContinuationParamName = "__cont"
        val functionImpl = Function1TypeSpecBuilder(
            parameterTypeName = ParameterizedTypeName.get(
                CONTINUATION, WildcardTypeName.supertypeOf(returnType.typeName)
            ),
            parameterName = innerContinuationParamName,
            returnTypeName = ClassName.OBJECT
        ) {
            val adapterScope = scope.fork()
            adapter.createDelegateToSuperStatement(
                returnType = returnType,
                parameterNames = parameterNames + innerContinuationParamName,
                daoName = daoName,
                daoImplName = daoImplName,
                returnStmt = true,
                scope = adapterScope
            )
            addCode(adapterScope.generate())
        }.build()

        scope.builder().apply {
            addStatement(
                "return $T.withTransaction($N, $L, $N)",
                DEPOT_DB_KT, dbField, functionImpl, continuationParamName
            )
        }
    }
}