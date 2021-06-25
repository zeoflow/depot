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

import com.zeoflow.depot.ext.LifecyclesTypeNames
import com.zeoflow.depot.compiler.processing.XRawType
import com.zeoflow.depot.compiler.processing.XType
import com.zeoflow.depot.processor.Context
import com.zeoflow.depot.solver.ObservableQueryResultBinderProvider
import com.zeoflow.depot.solver.query.result.LiveDataQueryResultBinder
import com.zeoflow.depot.solver.query.result.QueryResultAdapter
import com.zeoflow.depot.solver.query.result.QueryResultBinder

class LiveDataQueryResultBinderProvider(context: Context) :
    ObservableQueryResultBinderProvider(context) {
    private val liveDataType: XRawType? by lazy {
        context.processingEnv.findType(LifecyclesTypeNames.LIVE_DATA)?.rawType
    }

    override fun extractTypeArg(declared: XType): XType = declared.typeArguments.first()

    override fun create(
        typeArg: XType,
        resultAdapter: QueryResultAdapter?,
        tableNames: Set<String>
    ): QueryResultBinder {
        return LiveDataQueryResultBinder(
            typeArg = typeArg,
            tableNames = tableNames,
            adapter = resultAdapter
        )
    }

    override fun matches(declared: XType): Boolean =
        declared.typeArguments.size == 1 && isLiveData(declared)

    private fun isLiveData(declared: XType): Boolean {
        if (liveDataType == null) {
            return false
        }
        return declared.rawType.isAssignableFrom(liveDataType!!)
    }
}