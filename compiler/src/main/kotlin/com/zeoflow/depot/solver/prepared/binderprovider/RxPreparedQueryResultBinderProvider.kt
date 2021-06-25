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

import com.zeoflow.depot.ext.L
import com.zeoflow.depot.ext.T
import com.zeoflow.depot.parser.ParsedQuery
import com.zeoflow.depot.compiler.processing.XRawType
import com.zeoflow.depot.compiler.processing.XType
import com.zeoflow.depot.processor.Context
import com.zeoflow.depot.solver.RxType
import com.zeoflow.depot.solver.prepared.binder.CallablePreparedQueryResultBinder.Companion.createPreparedBinder
import com.zeoflow.depot.solver.prepared.binder.PreparedQueryResultBinder

open class RxPreparedQueryResultBinderProvider internal constructor(
    val context: Context,
    private val rxType: RxType
) : PreparedQueryResultBinderProvider {

    private val hasRxJavaArtifact by lazy {
        context.processingEnv.findTypeElement(rxType.version.rxDepotClassName) != null
    }

    override fun matches(declared: XType): Boolean =
        declared.typeArguments.size == 1 && matchesRxType(declared)

    private fun matchesRxType(declared: XType): Boolean {
        return declared.rawType.typeName == rxType.className
    }

    override fun provide(declared: XType, query: ParsedQuery): PreparedQueryResultBinder {
        if (!hasRxJavaArtifact) {
            context.logger.e(rxType.version.missingArtifactMessage)
        }
        val typeArg = extractTypeArg(declared)
        return createPreparedBinder(
            returnType = typeArg,
            adapter = context.typeAdapterStore.findPreparedQueryResultAdapter(typeArg, query)
        ) { callableImpl, _ ->
            addStatement("return $T.fromCallable($L)", rxType.className, callableImpl)
        }
    }

    open fun extractTypeArg(declared: XType): XType = declared.typeArguments.first()

    companion object {
        fun getAll(context: Context) = listOf(
            RxPreparedQueryResultBinderProvider(context, RxType.RX2_SINGLE),
            RxPreparedQueryResultBinderProvider(context, RxType.RX2_MAYBE),
            RxCompletablePreparedQueryResultBinderProvider(context, RxType.RX2_COMPLETABLE),
            RxPreparedQueryResultBinderProvider(context, RxType.RX3_SINGLE),
            RxPreparedQueryResultBinderProvider(context, RxType.RX3_MAYBE),
            RxCompletablePreparedQueryResultBinderProvider(context, RxType.RX3_COMPLETABLE)
        )
    }
}

private class RxCompletablePreparedQueryResultBinderProvider(
    context: Context,
    rxType: RxType
) : RxPreparedQueryResultBinderProvider(context, rxType) {

    private val completableType: XRawType? by lazy {
        context.processingEnv.findType(rxType.className)?.rawType
    }

    override fun matches(declared: XType): Boolean {
        if (completableType == null) {
            return false
        }
        return declared.rawType.isAssignableFrom(completableType!!)
    }

    override fun extractTypeArg(declared: XType) = context.COMMON_TYPES.VOID
}