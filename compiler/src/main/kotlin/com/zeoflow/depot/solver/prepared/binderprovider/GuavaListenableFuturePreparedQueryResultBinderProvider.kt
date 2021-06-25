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

package com.zeoflow.depot.solver.prepared.binderprovider

import com.zeoflow.depot.compiler.processing.XType
import com.zeoflow.depot.ext.GuavaUtilConcurrentTypeNames
import com.zeoflow.depot.ext.L
import com.zeoflow.depot.ext.N
import com.zeoflow.depot.ext.DepotGuavaTypeNames
import com.zeoflow.depot.ext.T
import com.zeoflow.depot.parser.ParsedQuery
import com.zeoflow.depot.processor.Context
import com.zeoflow.depot.processor.ProcessorErrors
import com.zeoflow.depot.solver.prepared.binder.CallablePreparedQueryResultBinder.Companion.createPreparedBinder
import com.zeoflow.depot.solver.prepared.binder.PreparedQueryResultBinder

class GuavaListenableFuturePreparedQueryResultBinderProvider(val context: Context) :
    PreparedQueryResultBinderProvider {

    private val hasGuavaDepot by lazy {
        context.processingEnv.findTypeElement(DepotGuavaTypeNames.GUAVA_DEPOT) != null
    }

    override fun matches(declared: XType): Boolean =
        declared.typeArguments.size == 1 &&
            declared.rawType.typeName == GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE

    override fun provide(declared: XType, query: ParsedQuery): PreparedQueryResultBinder {
        if (!hasGuavaDepot) {
            context.logger.e(ProcessorErrors.MISSING_DEPOT_GUAVA_ARTIFACT)
        }
        val typeArg = declared.typeArguments.first()
        return createPreparedBinder(
            returnType = typeArg,
            adapter = context.typeAdapterStore.findPreparedQueryResultAdapter(typeArg, query)
        ) { callableImpl, dbField ->
            addStatement(
                "return $T.createListenableFuture($N, $L, $L)",
                DepotGuavaTypeNames.GUAVA_DEPOT,
                dbField,
                "true", // inTransaction
                callableImpl
            )
        }
    }
}