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

package com.zeoflow.depot.solver.binderprovider

import com.zeoflow.depot.compiler.processing.XType
import com.zeoflow.depot.ext.KotlinTypeNames
import com.zeoflow.depot.ext.DepotCoroutinesTypeNames
import com.zeoflow.depot.parser.ParsedQuery
import com.zeoflow.depot.processor.Context
import com.zeoflow.depot.processor.ProcessorErrors
import com.zeoflow.depot.solver.QueryResultBinderProvider
import com.zeoflow.depot.solver.query.result.CoroutineFlowResultBinder
import com.zeoflow.depot.solver.query.result.QueryResultBinder

@Suppress("FunctionName")
fun CoroutineFlowResultBinderProvider(context: Context): QueryResultBinderProvider =
    CoroutineFlowResultBinderProviderImpl(
        context
    ).requireArtifact(
        context = context,
        requiredType = DepotCoroutinesTypeNames.COROUTINES_DEPOT,
        missingArtifactErrorMsg = ProcessorErrors.MISSING_DEPOT_COROUTINE_ARTIFACT
    )

private class CoroutineFlowResultBinderProviderImpl(
    val context: Context
) : QueryResultBinderProvider {
    companion object {
        val CHANNEL_TYPE_NAMES = listOf(
            KotlinTypeNames.CHANNEL,
            KotlinTypeNames.SEND_CHANNEL,
            KotlinTypeNames.RECEIVE_CHANNEL
        )
    }

    override fun provide(declared: XType, query: ParsedQuery): QueryResultBinder {
        val typeArg = declared.typeArguments.first()
        val adapter = context.typeAdapterStore.findQueryResultAdapter(typeArg, query)
        val tableNames = (
            (adapter?.accessedTableNames() ?: emptyList()) +
                query.tables.map { it.name }
            ).toSet()
        if (tableNames.isEmpty()) {
            context.logger.e(ProcessorErrors.OBSERVABLE_QUERY_NOTHING_TO_OBSERVE)
        }
        return CoroutineFlowResultBinder(typeArg, tableNames, adapter)
    }

    override fun matches(declared: XType): Boolean {
        if (declared.typeArguments.size != 1) {
            return false
        }
        val typeName = declared.rawType.typeName
        if (typeName in CHANNEL_TYPE_NAMES) {
            context.logger.e(ProcessorErrors.invalidChannelType(typeName.toString()))
            return false
        }
        return typeName == KotlinTypeNames.FLOW
    }
}