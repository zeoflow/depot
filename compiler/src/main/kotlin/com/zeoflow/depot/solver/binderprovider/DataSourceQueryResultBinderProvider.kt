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

import com.zeoflow.depot.ext.PagingTypeNames
import com.zeoflow.depot.parser.ParsedQuery
import com.zeoflow.depot.compiler.processing.XRawType
import com.zeoflow.depot.compiler.processing.XType
import com.zeoflow.depot.processor.Context
import com.zeoflow.depot.processor.ProcessorErrors
import com.zeoflow.depot.solver.QueryResultBinderProvider
import com.zeoflow.depot.solver.query.result.ListQueryResultAdapter
import com.zeoflow.depot.solver.query.result.PositionalDataSourceQueryResultBinder
import com.zeoflow.depot.solver.query.result.QueryResultBinder

class DataSourceQueryResultBinderProvider(val context: Context) : QueryResultBinderProvider {
    private val dataSourceType: XRawType? by lazy {
        context.processingEnv.findType(PagingTypeNames.DATA_SOURCE)?.rawType
    }

    private val positionalDataSourceType: XRawType? by lazy {
        context.processingEnv.findType(PagingTypeNames.POSITIONAL_DATA_SOURCE)?.rawType
    }

    override fun provide(declared: XType, query: ParsedQuery): QueryResultBinder {
        if (query.tables.isEmpty()) {
            context.logger.e(ProcessorErrors.OBSERVABLE_QUERY_NOTHING_TO_OBSERVE)
        }
        val typeArg = declared.typeArguments.last()
        val listAdapter = context.typeAdapterStore.findRowAdapter(typeArg, query)?.let {
            ListQueryResultAdapter(typeArg, it)
        }
        val tableNames = (
            (listAdapter?.accessedTableNames() ?: emptyList()) +
                query.tables.map { it.name }
            ).toSet()
        return PositionalDataSourceQueryResultBinder(
            listAdapter = listAdapter,
            tableNames = tableNames,
            forPaging3 = false
        )
    }

    override fun matches(declared: XType): Boolean {
        if (dataSourceType == null || positionalDataSourceType == null) {
            return false
        }
        if (declared.typeArguments.isEmpty()) {
            return false
        }
        val isDataSource = dataSourceType!!.isAssignableFrom(declared)
        if (!isDataSource) {
            return false
        }
        val isPositional = positionalDataSourceType!!.isAssignableFrom(declared)
        if (!isPositional) {
            context.logger.e(ProcessorErrors.PAGING_SPECIFY_DATA_SOURCE_TYPE)
        }
        return true
    }
}
