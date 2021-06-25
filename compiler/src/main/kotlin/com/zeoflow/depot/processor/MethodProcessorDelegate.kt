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

package com.zeoflow.depot.processor

import com.zeoflow.depot.compiler.processing.XMethodElement
import com.zeoflow.depot.compiler.processing.XMethodType
import com.zeoflow.depot.compiler.processing.XSuspendMethodType
import com.zeoflow.depot.compiler.processing.XType
import com.zeoflow.depot.compiler.processing.XVariableElement
import com.zeoflow.depot.compiler.processing.isSuspendFunction
import com.zeoflow.depot.ext.KotlinTypeNames
import com.zeoflow.depot.ext.L
import com.zeoflow.depot.ext.N
import com.zeoflow.depot.ext.DepotCoroutinesTypeNames
import com.zeoflow.depot.ext.T
import com.zeoflow.depot.parser.ParsedQuery
import com.zeoflow.depot.solver.prepared.binder.CallablePreparedQueryResultBinder.Companion.createPreparedBinder
import com.zeoflow.depot.solver.prepared.binder.PreparedQueryResultBinder
import com.zeoflow.depot.solver.query.result.CoroutineResultBinder
import com.zeoflow.depot.solver.query.result.QueryResultBinder
import com.zeoflow.depot.solver.shortcut.binder.CallableDeleteOrUpdateMethodBinder.Companion.createDeleteOrUpdateBinder
import com.zeoflow.depot.solver.shortcut.binder.CallableInsertMethodBinder.Companion.createInsertBinder
import com.zeoflow.depot.solver.shortcut.binder.DeleteOrUpdateMethodBinder
import com.zeoflow.depot.solver.shortcut.binder.InsertMethodBinder
import com.zeoflow.depot.solver.transaction.binder.CoroutineTransactionMethodBinder
import com.zeoflow.depot.solver.transaction.binder.InstantTransactionMethodBinder
import com.zeoflow.depot.solver.transaction.binder.TransactionMethodBinder
import com.zeoflow.depot.solver.transaction.result.TransactionMethodAdapter
import com.zeoflow.depot.vo.QueryParameter
import com.zeoflow.depot.vo.ShortcutQueryParameter
import com.zeoflow.depot.vo.TransactionMethod

/**
 *  Delegate class with common functionality for DAO method processors.
 */
abstract class MethodProcessorDelegate(
    val context: Context,
    val containing: XType,
    val executableElement: XMethodElement
) {

    abstract fun extractReturnType(): XType

    abstract fun extractParams(): List<XVariableElement>

    fun extractQueryParams(query: ParsedQuery): List<QueryParameter> {
        return extractParams().map { variableElement ->
            QueryParameterProcessor(
                baseContext = context,
                containing = containing,
                element = variableElement,
                sqlName = variableElement.name,
                bindVarSection = query.bindSections.firstOrNull {
                    it.varName == variableElement.name
                }
            ).process()
        }
    }

    abstract fun findResultBinder(returnType: XType, query: ParsedQuery): QueryResultBinder

    abstract fun findPreparedResultBinder(
        returnType: XType,
        query: ParsedQuery
    ): PreparedQueryResultBinder

    abstract fun findInsertMethodBinder(
        returnType: XType,
        params: List<ShortcutQueryParameter>
    ): InsertMethodBinder

    abstract fun findDeleteOrUpdateMethodBinder(returnType: XType): DeleteOrUpdateMethodBinder

    abstract fun findTransactionMethodBinder(
        callType: TransactionMethod.CallType
    ): TransactionMethodBinder

    companion object {
        fun createFor(
            context: Context,
            containing: XType,
            executableElement: XMethodElement
        ): MethodProcessorDelegate {
            val asMember = executableElement.asMemberOf(containing)
            return if (asMember.isSuspendFunction()) {
                val hasCoroutineArtifact = context.processingEnv
                    .findTypeElement(DepotCoroutinesTypeNames.COROUTINES_DEPOT.toString()) != null
                if (!hasCoroutineArtifact) {
                    context.logger.e(ProcessorErrors.MISSING_DEPOT_COROUTINE_ARTIFACT)
                }
                SuspendMethodProcessorDelegate(
                    context,
                    containing,
                    executableElement,
                    asMember
                )
            } else {
                DefaultMethodProcessorDelegate(
                    context,
                    containing,
                    executableElement,
                    asMember
                )
            }
        }
    }
}

/**
 * Default delegate for DAO methods.
 */
class DefaultMethodProcessorDelegate(
    context: Context,
    containing: XType,
    executableElement: XMethodElement,
    val executableType: XMethodType
) : MethodProcessorDelegate(context, containing, executableElement) {

    override fun extractReturnType(): XType {
        return executableType.returnType
    }

    override fun extractParams() = executableElement.parameters

    override fun findResultBinder(returnType: XType, query: ParsedQuery) =
        context.typeAdapterStore.findQueryResultBinder(returnType, query)

    override fun findPreparedResultBinder(
        returnType: XType,
        query: ParsedQuery
    ) = context.typeAdapterStore.findPreparedQueryResultBinder(returnType, query)

    override fun findInsertMethodBinder(
        returnType: XType,
        params: List<ShortcutQueryParameter>
    ) = context.typeAdapterStore.findInsertMethodBinder(returnType, params)

    override fun findDeleteOrUpdateMethodBinder(returnType: XType) =
        context.typeAdapterStore.findDeleteOrUpdateMethodBinder(returnType)

    override fun findTransactionMethodBinder(callType: TransactionMethod.CallType) =
        InstantTransactionMethodBinder(
            TransactionMethodAdapter(executableElement.name, callType)
        )
}

/**
 * Delegate for DAO methods that are a suspend function.
 */
class SuspendMethodProcessorDelegate(
    context: Context,
    containing: XType,
    executableElement: XMethodElement,
    val executableType: XSuspendMethodType
) : MethodProcessorDelegate(context, containing, executableElement) {

    private val continuationParam: XVariableElement by lazy {
        val continuationType = context.processingEnv
            .requireType(KotlinTypeNames.CONTINUATION.toString()).rawType
        executableElement.parameters.last {
            it.type.rawType == continuationType
        }
    }

    override fun extractReturnType(): XType {
        return executableType.getSuspendFunctionReturnType()
    }

    override fun extractParams() =
        executableElement.parameters.filterNot {
            it == continuationParam
        }

    override fun findResultBinder(returnType: XType, query: ParsedQuery) =
        CoroutineResultBinder(
            typeArg = returnType,
            adapter = context.typeAdapterStore.findQueryResultAdapter(returnType, query),
            continuationParamName = continuationParam.name
        )

    override fun findPreparedResultBinder(
        returnType: XType,
        query: ParsedQuery
    ) = createPreparedBinder(
        returnType = returnType,
        adapter = context.typeAdapterStore.findPreparedQueryResultAdapter(returnType, query)
    ) { callableImpl, dbField ->
        addStatement(
            "return $T.execute($N, $L, $L, $N)",
            DepotCoroutinesTypeNames.COROUTINES_DEPOT,
            dbField,
            "true", // inTransaction
            callableImpl,
            continuationParam.name
        )
    }

    override fun findInsertMethodBinder(
        returnType: XType,
        params: List<ShortcutQueryParameter>
    ) = createInsertBinder(
        typeArg = returnType,
        adapter = context.typeAdapterStore.findInsertAdapter(returnType, params)
    ) { callableImpl, dbField ->
        addStatement(
            "return $T.execute($N, $L, $L, $N)",
            DepotCoroutinesTypeNames.COROUTINES_DEPOT,
            dbField,
            "true", // inTransaction
            callableImpl,
            continuationParam.name
        )
    }

    override fun findDeleteOrUpdateMethodBinder(returnType: XType) =
        createDeleteOrUpdateBinder(
            typeArg = returnType,
            adapter = context.typeAdapterStore.findDeleteOrUpdateAdapter(returnType)
        ) { callableImpl, dbField ->
            addStatement(
                "return $T.execute($N, $L, $L, $N)",
                DepotCoroutinesTypeNames.COROUTINES_DEPOT,
                dbField,
                "true", // inTransaction
                callableImpl,
                continuationParam.name
            )
        }

    override fun findTransactionMethodBinder(callType: TransactionMethod.CallType) =
        CoroutineTransactionMethodBinder(
            adapter = TransactionMethodAdapter(executableElement.name, callType),
            continuationParamName = continuationParam.name
        )
}