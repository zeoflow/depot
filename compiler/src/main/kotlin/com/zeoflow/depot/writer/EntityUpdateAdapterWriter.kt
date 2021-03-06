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
import com.zeoflow.depot.ext.DepotTypeNames
import com.zeoflow.depot.ext.S
import com.zeoflow.depot.ext.SupportDbTypeNames
import com.zeoflow.depot.solver.CodeGenScope
import com.zeoflow.depot.vo.FieldWithIndex
import com.zeoflow.depot.vo.Fields
import com.zeoflow.depot.vo.Pojo
import com.zeoflow.depot.vo.ShortcutEntity
import com.zeoflow.depot.vo.columnNames
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier.PUBLIC

class EntityUpdateAdapterWriter private constructor(
    val tableName: String,
    val pojo: Pojo,
    val primaryKeyFields: Fields,
    val onConflict: String
) {
    companion object {
        fun create(entity: ShortcutEntity, onConflict: String) =
            EntityUpdateAdapterWriter(
                tableName = entity.tableName,
                pojo = entity.pojo,
                primaryKeyFields = entity.primaryKey.fields,
                onConflict = onConflict
            )
    }

    fun createAnonymous(classWriter: ClassWriter, dbParam: String): TypeSpec {
        @Suppress("RemoveSingleExpressionStringTemplate")
        return TypeSpec.anonymousClassBuilder("$L", dbParam).apply {
            superclass(
                ParameterizedTypeName.get(
                    DepotTypeNames.DELETE_OR_UPDATE_ADAPTER,
                    pojo.typeName
                )
            )
            addMethod(
                MethodSpec.methodBuilder("createQuery").apply {
                    addAnnotation(Override::class.java)
                    addModifiers(PUBLIC)
                    returns(ClassName.get("java.lang", "String"))
                    val query = "UPDATE OR $onConflict `$tableName` SET " +
                        pojo.columnNames.joinToString(",") { "`$it` = ?" } + " WHERE " +
                        primaryKeyFields.columnNames.joinToString(" AND ") { "`$it` = ?" }
                    addStatement("return $S", query)
                }.build()
            )
            addMethod(
                MethodSpec.methodBuilder("bind").apply {
                    val bindScope = CodeGenScope(classWriter)
                    addAnnotation(Override::class.java)
                    addModifiers(PUBLIC)
                    returns(TypeName.VOID)
                    val stmtParam = "stmt"
                    addParameter(
                        ParameterSpec.builder(
                            SupportDbTypeNames.SQLITE_STMT,
                            stmtParam
                        ).build()
                    )
                    val valueParam = "value"
                    addParameter(ParameterSpec.builder(pojo.typeName, valueParam).build())
                    val mappedField = FieldWithIndex.byOrder(pojo.fields)
                    FieldReadWriteWriter.bindToStatement(
                        ownerVar = valueParam,
                        stmtParamVar = stmtParam,
                        fieldsWithIndices = mappedField,
                        scope = bindScope
                    )
                    val pkeyStart = pojo.fields.size
                    val mappedPrimaryKeys = primaryKeyFields.mapIndexed { index, field ->
                        FieldWithIndex(
                            field = field,
                            indexVar = "${pkeyStart + index + 1}",
                            alwaysExists = true
                        )
                    }
                    FieldReadWriteWriter.bindToStatement(
                        ownerVar = valueParam,
                        stmtParamVar = stmtParam,
                        fieldsWithIndices = mappedPrimaryKeys,
                        scope = bindScope
                    )
                    addCode(bindScope.builder().build())
                }.build()
            )
        }.build()
    }
}
