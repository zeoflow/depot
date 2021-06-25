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

import com.zeoflow.depot.compiler.processing.XRawType
import com.zeoflow.depot.compiler.processing.XType
import com.zeoflow.depot.ext.PagingTypeNames
import com.zeoflow.depot.parser.ParsedQuery
import com.zeoflow.depot.processor.Context
import com.zeoflow.depot.processor.ProcessorErrors
import com.zeoflow.depot.solver.QueryResultBinderProvider
import com.zeoflow.depot.solver.query.result.DataSourceFactoryQueryResultBinder
import com.zeoflow.depot.solver.query.result.ListQueryResultAdapter
import com.zeoflow.depot.solver.query.result.PositionalDataSourceQueryResultBinder
import com.zeoflow.depot.solver.query.result.QueryResultBinder

class DataSourceFactoryQueryResultBinderProvider(val context: Context) : QueryResultBinderProvider {
    private val dataSourceFactoryType: XRawType? by lazy {
        context.processingEnv.findType(PagingTypeNames.DATA_SOURCE_FACTORY)?.rawType
    }

    override fun provide(declared: XType, query: ParsedQuery): QueryResultBinder {
        if (query.tables.isEmpty()) {
            context.logger.e(ProcessorErrors.OBSERVABLE_QUERY_NOTHING_TO_OBSERVE)
        }
        val typeArg = declared.typeArguments[1]
        val adapter = context.typeAdapterStore.findRowAdapter(typeArg, query)?.let {
            ListQueryResultAdapter(typeArg, it)
        }

        val tableNames = (
            (adapter?.accessedTableNames() ?: emptyList()) +
                query.tables.map { it.name }
            ).toSet()
        val countedBinder = PositionalDataSourceQueryResultBinder(
            listAdapter = adapter,
            tableNames = tableNames,
            forPaging3 = false
        )
        return DataSourceFactoryQueryResultBinder(countedBinder)
    }

    override fun matches(declared: XType): Boolean =
        declared.typeArguments.size == 2 && isLivePagedList(declared)

    private fun isLivePagedList(declared: XType): Boolean {
        if (dataSourceFactoryType == null) {
            return false
        }
        // we don't want to return paged list unless explicitly requested
        return declared.rawType.isAssignableFrom(dataSourceFactoryType!!)
    }
}
