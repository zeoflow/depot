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

package com.zeoflow.depot.solver.shortcut.binder

import com.zeoflow.depot.ext.N
import com.zeoflow.depot.solver.CodeGenScope
import com.zeoflow.depot.solver.shortcut.result.DeleteOrUpdateMethodAdapter
import com.zeoflow.depot.vo.ShortcutQueryParameter
import com.zeoflow.depot.writer.DaoWriter
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeSpec

/**
 * Binder that knows how to write instant (blocking) delete and update methods.
 */
class InstantDeleteOrUpdateMethodBinder(
    adapter: DeleteOrUpdateMethodAdapter?
) : DeleteOrUpdateMethodBinder(adapter) {

    override fun convertAndReturn(
        parameters: List<ShortcutQueryParameter>,
        adapters: Map<String, Pair<FieldSpec, TypeSpec>>,
        dbField: FieldSpec,
        scope: CodeGenScope
    ) {
        scope.builder().apply {
            addStatement("$N.assertNotSuspendingTransaction()", DaoWriter.dbField)
        }
        adapter?.createDeleteOrUpdateMethodBody(
            parameters = parameters,
            adapters = adapters,
            dbField = dbField,
            scope = scope
        )
    }
}