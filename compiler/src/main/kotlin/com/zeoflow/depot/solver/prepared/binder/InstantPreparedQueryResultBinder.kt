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

package com.zeoflow.depot.solver.prepared.binder

import com.zeoflow.depot.ext.N
import com.zeoflow.depot.solver.CodeGenScope
import com.zeoflow.depot.solver.prepared.result.PreparedQueryResultAdapter
import com.zeoflow.depot.writer.DaoWriter
import com.squareup.javapoet.FieldSpec

/**
 * Default binder for prepared queries.
 */
class InstantPreparedQueryResultBinder(adapter: PreparedQueryResultAdapter?) :
    PreparedQueryResultBinder(adapter) {

    override fun executeAndReturn(
        prepareQueryStmtBlock: CodeGenScope.() -> String,
        preparedStmtField: String?,
        dbField: FieldSpec,
        scope: CodeGenScope
    ) {
        scope.builder().apply {
            addStatement("$N.assertNotSuspendingTransaction()", DaoWriter.dbField)
        }
        adapter?.executeAndReturn(
            stmtQueryVal = scope.prepareQueryStmtBlock(),
            preparedStmtField = preparedStmtField,
            dbField = dbField,
            scope = scope
        )
    }
}