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

package com.zeoflow.depot.writer

import com.zeoflow.depot.ext.L
import com.zeoflow.depot.ext.DepotTypeNames.DEPOT_SQL_QUERY
import com.zeoflow.depot.ext.DepotTypeNames.STRING_UTIL
import com.zeoflow.depot.ext.S
import com.zeoflow.depot.ext.T
import com.zeoflow.depot.ext.typeName
import com.zeoflow.depot.parser.ParsedQuery
import com.zeoflow.depot.parser.Section
import com.zeoflow.depot.solver.CodeGenScope
import com.zeoflow.depot.vo.QueryMethod
import com.zeoflow.depot.vo.QueryParameter
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName

/**
 * Writes the SQL query and arguments for a QueryMethod.
 */
class QueryWriter constructor(
    val parameters: List<QueryParameter>,
    val sectionToParamMapping: List<Pair<Section, QueryParameter?>>,
    val query: ParsedQuery
) {

    constructor(queryMethod: QueryMethod) : this(
        queryMethod.parameters,
        queryMethod.sectionToParamMapping, queryMethod.query
    )

    fun prepareReadAndBind(
        outSqlQueryName: String,
        outDepotSQLiteQueryVar: String,
        scope: CodeGenScope
    ) {
        val listSizeVars = createSqlQueryAndArgs(outSqlQueryName, outDepotSQLiteQueryVar, scope)
        bindArgs(outDepotSQLiteQueryVar, listSizeVars, scope)
    }

    fun prepareQuery(
        outSqlQueryName: String,
        scope: CodeGenScope
    ): List<Pair<QueryParameter, String>> {
        return createSqlQueryAndArgs(outSqlQueryName, null, scope)
    }

    private fun createSqlQueryAndArgs(
        outSqlQueryName: String,
        outArgsName: String?,
        scope: CodeGenScope
    ): List<Pair<QueryParameter, String>> {
        val listSizeVars = arrayListOf<Pair<QueryParameter, String>>()
        val varargParams = parameters
            .filter { it.queryParamAdapter?.isMultiple ?: false }
        val sectionToParamMapping = sectionToParamMapping
        val knownQueryArgsCount = sectionToParamMapping.filterNot {
            it.second?.queryParamAdapter?.isMultiple ?: false
        }.size
        scope.builder().apply {
            if (varargParams.isNotEmpty()) {
                val stringBuilderVar = scope.getTmpVar("_stringBuilder")
                addStatement(
                    "$T $L = $T.newStringBuilder()",
                    ClassName.get(StringBuilder::class.java), stringBuilderVar, STRING_UTIL
                )
                query.sections.forEach {
                    @Suppress("UNUSED_VARIABLE")
                    val exhaustive = when (it) {
                        is Section.Text -> addStatement("$L.append($S)", stringBuilderVar, it.text)
                        is Section.NewLine -> addStatement("$L.append($S)", stringBuilderVar, "\n")
                        is Section.BindVar -> {
                            // If it is null, will be reported as error before. We just try out
                            // best to generate as much code as possible.
                            sectionToParamMapping.firstOrNull { mapping ->
                                mapping.first == it
                            }?.let { pair ->
                                if (pair.second?.queryParamAdapter?.isMultiple ?: false) {
                                    val tmpCount = scope.getTmpVar("_inputSize")
                                    listSizeVars.add(Pair(pair.second!!, tmpCount))
                                    pair.second
                                        ?.queryParamAdapter
                                        ?.getArgCount(pair.second!!.name, tmpCount, scope)
                                    addStatement(
                                        "$T.appendPlaceholders($L, $L)",
                                        STRING_UTIL, stringBuilderVar, tmpCount
                                    )
                                } else {
                                    addStatement("$L.append($S)", stringBuilderVar, "?")
                                }
                            }
                        }
                    }
                }

                addStatement(
                    "final $T $L = $L.toString()", String::class.typeName,
                    outSqlQueryName, stringBuilderVar
                )
                if (outArgsName != null) {
                    val argCount = scope.getTmpVar("_argCount")
                    addStatement(
                        "final $T $L = $L$L", TypeName.INT, argCount, knownQueryArgsCount,
                        listSizeVars.joinToString("") { " + ${it.second}" }
                    )
                    addStatement(
                        "final $T $L = $T.acquire($L, $L)",
                        DEPOT_SQL_QUERY, outArgsName, DEPOT_SQL_QUERY, outSqlQueryName,
                        argCount
                    )
                }
            } else {
                addStatement(
                    "final $T $L = $S", String::class.typeName,
                    outSqlQueryName, query.queryWithReplacedBindParams
                )
                if (outArgsName != null) {
                    addStatement(
                        "final $T $L = $T.acquire($L, $L)",
                        DEPOT_SQL_QUERY, outArgsName, DEPOT_SQL_QUERY, outSqlQueryName,
                        knownQueryArgsCount
                    )
                }
            }
        }
        return listSizeVars
    }

    fun bindArgs(
        outArgsName: String,
        listSizeVars: List<Pair<QueryParameter, String>>,
        scope: CodeGenScope
    ) {
        if (parameters.isEmpty()) {
            return
        }
        scope.builder().apply {
            val argIndex = scope.getTmpVar("_argIndex")
            addStatement("$T $L = $L", TypeName.INT, argIndex, 1)
            // # of bindings with 1 placeholder
            var constInputs = 0
            // variable names for size of the bindings that have multiple  args
            val varInputs = arrayListOf<String>()
            sectionToParamMapping.forEach { pair ->
                // reset the argIndex to the correct start index
                if (constInputs > 0 || varInputs.isNotEmpty()) {
                    addStatement(
                        "$L = $L$L", argIndex,
                        if (constInputs > 0) (1 + constInputs) else "1",
                        varInputs.joinToString("") { " + $it" }
                    )
                }
                val param = pair.second
                param?.let {
                    param.queryParamAdapter?.bindToStmt(param.name, outArgsName, argIndex, scope)
                }
                // add these to the list so that we can use them to calculate the next count.
                val sizeVar = listSizeVars.firstOrNull { it.first == param }
                if (sizeVar == null) {
                    constInputs ++
                } else {
                    varInputs.add(sizeVar.second)
                }
            }
        }
    }
}
