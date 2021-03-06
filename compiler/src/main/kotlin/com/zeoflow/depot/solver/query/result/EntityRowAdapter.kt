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

package com.zeoflow.depot.solver.query.result

import com.zeoflow.depot.ext.L
import com.zeoflow.depot.ext.N
import com.zeoflow.depot.solver.CodeGenScope
import com.zeoflow.depot.vo.Entity
import com.zeoflow.depot.writer.EntityCursorConverterWriter
import com.squareup.javapoet.MethodSpec

class EntityRowAdapter(val entity: Entity) : RowAdapter(entity.type) {
    lateinit var methodSpec: MethodSpec
    override fun onCursorReady(cursorVarName: String, scope: CodeGenScope) {
        methodSpec = scope.writer.getOrCreateMethod(EntityCursorConverterWriter(entity))
    }

    override fun convert(outVarName: String, cursorVarName: String, scope: CodeGenScope) {
        scope.builder()
            .addStatement("$L = $N($L)", outVarName, methodSpec, cursorVarName)
    }
}
