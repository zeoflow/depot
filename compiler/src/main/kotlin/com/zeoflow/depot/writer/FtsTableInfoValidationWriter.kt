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

import com.zeoflow.depot.ext.CommonTypeNames
import com.zeoflow.depot.ext.L
import com.zeoflow.depot.ext.N
import com.zeoflow.depot.ext.DepotTypeNames
import com.zeoflow.depot.ext.S
import com.zeoflow.depot.ext.T
import com.zeoflow.depot.ext.typeName
import com.zeoflow.depot.vo.FtsEntity
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import stripNonJava
import java.util.Locale

class FtsTableInfoValidationWriter(val entity: FtsEntity) : ValidationWriter() {
    override fun write(dbParam: ParameterSpec, scope: CountingCodeGenScope) {
        val suffix = entity.tableName.stripNonJava().capitalize(Locale.US)
        val expectedInfoVar = scope.getTmpVar("_info$suffix")
        scope.builder().apply {
            val columnListVar = scope.getTmpVar("_columns$suffix")
            val columnListType = ParameterizedTypeName.get(
                HashSet::class.typeName,
                CommonTypeNames.STRING
            )

            addStatement(
                "final $T $L = new $T($L)", columnListType, columnListVar,
                columnListType, entity.fields.size
            )
            entity.nonHiddenFields.forEach {
                addStatement("$L.add($S)", columnListVar, it.columnName)
            }

            addStatement(
                "final $T $L = new $T($S, $L, $S)",
                DepotTypeNames.FTS_TABLE_INFO, expectedInfoVar, DepotTypeNames.FTS_TABLE_INFO,
                entity.tableName, columnListVar, entity.createTableQuery
            )

            val existingVar = scope.getTmpVar("_existing$suffix")
            addStatement(
                "final $T $L = $T.read($N, $S)",
                DepotTypeNames.FTS_TABLE_INFO, existingVar, DepotTypeNames.FTS_TABLE_INFO,
                dbParam, entity.tableName
            )

            beginControlFlow("if (!$L.equals($L))", expectedInfoVar, existingVar).apply {
                addStatement(
                    "return new $T(false, $S + $L + $S + $L)",
                    DepotTypeNames.OPEN_HELPER_VALIDATION_RESULT,
                    "${entity.tableName}(${entity.element.qualifiedName}).\n Expected:\n",
                    expectedInfoVar, "\n Found:\n", existingVar
                )
            }
            endControlFlow()
        }
    }
}