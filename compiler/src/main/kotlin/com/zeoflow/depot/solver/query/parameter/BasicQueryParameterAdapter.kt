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

package com.zeoflow.depot.solver.query.parameter

import com.zeoflow.depot.solver.CodeGenScope
import com.zeoflow.depot.solver.types.StatementValueBinder

/**
 * Knows how to convert a query parameter into arguments
 */
class BasicQueryParameterAdapter(val bindAdapter: StatementValueBinder) :
    QueryParameterAdapter(false) {
    override fun bindToStmt(
        inputVarName: String,
        stmtVarName: String,
        startIndexVarName: String,
        scope: CodeGenScope
    ) {
        scope.builder().apply {
            bindAdapter.bindToStmt(stmtVarName, startIndexVarName, inputVarName, scope)
        }
    }

    override fun getArgCount(inputVarName: String, outputVarName: String, scope: CodeGenScope) {
        throw UnsupportedOperationException(
            "should not call getArgCount on basic adapters." +
                "It is always one."
        )
    }
}
