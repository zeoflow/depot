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

package com.zeoflow.depot.solver.shortcut.binderprovider

import com.zeoflow.depot.ext.L
import com.zeoflow.depot.ext.T
import com.zeoflow.depot.compiler.processing.XRawType
import com.zeoflow.depot.compiler.processing.XType
import com.zeoflow.depot.processor.Context
import com.zeoflow.depot.solver.RxType
import com.zeoflow.depot.solver.shortcut.binder.CallableDeleteOrUpdateMethodBinder.Companion.createDeleteOrUpdateBinder
import com.zeoflow.depot.solver.shortcut.binder.DeleteOrUpdateMethodBinder

/**
 * Provider for Rx Callable binders.
 */
open class RxCallableDeleteOrUpdateMethodBinderProvider internal constructor(
    val context: Context,
    private val rxType: RxType
) : DeleteOrUpdateMethodBinderProvider {

    /**
     * [Single] and [Maybe] are generics but [Completable] is not so each implementation of this
     * class needs to define how to extract the type argument.
     */
    open fun extractTypeArg(declared: XType): XType = declared.typeArguments.first()

    override fun matches(declared: XType): Boolean =
        declared.typeArguments.size == 1 && matchesRxType(declared)

    private fun matchesRxType(declared: XType): Boolean {
        return declared.rawType.typeName == rxType.className
    }

    override fun provide(declared: XType): DeleteOrUpdateMethodBinder {
        val typeArg = extractTypeArg(declared)
        val adapter = context.typeAdapterStore.findDeleteOrUpdateAdapter(typeArg)
        return createDeleteOrUpdateBinder(typeArg, adapter) { callableImpl, _ ->
            addStatement("return $T.fromCallable($L)", rxType.className, callableImpl)
        }
    }

    companion object {
        fun getAll(context: Context) = listOf(
            RxCallableDeleteOrUpdateMethodBinderProvider(context, RxType.RX2_SINGLE),
            RxCallableDeleteOrUpdateMethodBinderProvider(context, RxType.RX2_MAYBE),
            RxCompletableDeleteOrUpdateMethodBinderProvider(context, RxType.RX2_COMPLETABLE),
            RxCallableDeleteOrUpdateMethodBinderProvider(context, RxType.RX3_SINGLE),
            RxCallableDeleteOrUpdateMethodBinderProvider(context, RxType.RX3_MAYBE),
            RxCompletableDeleteOrUpdateMethodBinderProvider(context, RxType.RX3_COMPLETABLE)
        )
    }
}

private class RxCompletableDeleteOrUpdateMethodBinderProvider(
    context: Context,
    rxType: RxType
) : RxCallableDeleteOrUpdateMethodBinderProvider(context, rxType) {

    private val completableType: XRawType? by lazy {
        context.processingEnv.findType(rxType.className)?.rawType
    }

    /**
     * Since Completable is not a generic, the supported return type should be Void.
     * Like this, the generated Callable.call method will return Void.
     */
    override fun extractTypeArg(declared: XType): XType =
        context.COMMON_TYPES.VOID

    override fun matches(declared: XType): Boolean = isCompletable(declared)

    private fun isCompletable(declared: XType): Boolean {
        if (completableType == null) {
            return false
        }
        return declared.rawType.isAssignableFrom(completableType!!)
    }
}
