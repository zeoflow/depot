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

package com.zeoflow.depot.vo

import com.zeoflow.depot.parser.ParsedQuery
import com.zeoflow.depot.compiler.processing.XMethodElement
import com.zeoflow.depot.compiler.processing.XType
import com.zeoflow.depot.solver.prepared.binder.PreparedQueryResultBinder
import com.zeoflow.depot.solver.query.result.QueryResultBinder

/**
 * A class that holds information about a QueryMethod.
 * It is self sufficient and must have all generics etc resolved once created.
 */
sealed class QueryMethod(
    val element: XMethodElement,
    val query: ParsedQuery,
    val name: String,
    val returnType: XType,
    val parameters: List<QueryParameter>
) {
    val sectionToParamMapping by lazy {
        query.bindSections.map {
            if (it.text.trim() == "?") {
                Pair(it, parameters.firstOrNull())
            } else if (it.text.startsWith(":")) {
                val subName = it.text.substring(1)
                Pair(
                    it,
                    parameters.firstOrNull {
                        it.sqlName == subName
                    }
                )
            } else {
                Pair(it, null)
            }
        }
    }
}

/**
 * A query method who's query is a SELECT statement.
 */
class ReadQueryMethod(
    element: XMethodElement,
    query: ParsedQuery,
    name: String,
    returnType: XType,
    parameters: List<QueryParameter>,
    val inTransaction: Boolean,
    val queryResultBinder: QueryResultBinder
) : QueryMethod(element, query, name, returnType, parameters)

/**
 * A query method who's query is a INSERT, UPDATE or DELETE statement.
 */
class WriteQueryMethod(
    element: XMethodElement,
    query: ParsedQuery,
    name: String,
    returnType: XType,
    parameters: List<QueryParameter>,
    val preparedQueryResultBinder: PreparedQueryResultBinder
) : QueryMethod(element, query, name, returnType, parameters)