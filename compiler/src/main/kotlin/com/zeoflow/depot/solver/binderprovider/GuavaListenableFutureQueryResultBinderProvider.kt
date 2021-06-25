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
import com.zeoflow.depot.ext.GuavaUtilConcurrentTypeNames
import com.zeoflow.depot.ext.DepotGuavaTypeNames
import com.zeoflow.depot.parser.ParsedQuery
import com.zeoflow.depot.processor.Context
import com.zeoflow.depot.processor.ProcessorErrors
import com.zeoflow.depot.solver.QueryResultBinderProvider
import com.zeoflow.depot.solver.query.result.GuavaListenableFutureQueryResultBinder
import com.zeoflow.depot.solver.query.result.QueryResultBinder

@Suppress("FunctionName")
fun GuavaListenableFutureQueryResultBinderProvider(context: Context): QueryResultBinderProvider =
    GuavaListenableFutureQueryResultBinderProviderImpl(
        context = context
    ).requireArtifact(
        context = context,
        requiredType = DepotGuavaTypeNames.GUAVA_DEPOT,
        missingArtifactErrorMsg = ProcessorErrors.MISSING_DEPOT_GUAVA_ARTIFACT
    )

class GuavaListenableFutureQueryResultBinderProviderImpl(
    val context: Context
) : QueryResultBinderProvider {
    /**
     * Returns the {@link GuavaListenableFutureQueryResultBinder} instance for the input type, if
     * possible.
     *
     * <p>Emits a compiler error if the Guava Depot extension library is not linked.
     */
    override fun provide(declared: XType, query: ParsedQuery): QueryResultBinder {
        // Use the type T inside ListenableFuture<T> as the type to adapt and to pass into
        // the binder.
        val adapter = context.typeAdapterStore.findQueryResultAdapter(
            declared.typeArguments.first(), query
        )
        return GuavaListenableFutureQueryResultBinder(
            declared.typeArguments.first(), adapter
        )
    }

    /**
     * Returns true iff the input {@code declared} type is ListenableFuture<T>.
     */
    override fun matches(declared: XType): Boolean =
        declared.typeArguments.size == 1 &&
            declared.rawType.typeName == GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE
}
