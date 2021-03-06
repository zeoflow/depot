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

import com.zeoflow.depot.ext.PagingTypeNames
import com.zeoflow.depot.ext.typeName
import com.zeoflow.depot.solver.CodeGenScope
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier

abstract class PagingQueryResultBinder(
    val positionalDataSourceQueryResultBinder: PositionalDataSourceQueryResultBinder
) : QueryResultBinder(positionalDataSourceQueryResultBinder.listAdapter) {

    /**
     * @return [CodeBlock.Builder.addStatement] string template for return statement in
     * [convertAndReturn], which will be passed a [TypeSpec].
     */
    abstract fun returnStatementTemplate(): String

    @Suppress("HasPlatformType")
    val typeName = positionalDataSourceQueryResultBinder.itemTypeName
    final override fun convertAndReturn(
        depotSQLiteQueryVar: String,
        canReleaseQuery: Boolean,
        dbField: FieldSpec,
        inTransaction: Boolean,
        scope: CodeGenScope
    ) {
        scope.builder().apply {
            val pagedListProvider = TypeSpec.anonymousClassBuilder("")
                .apply {
                    superclass(
                        ParameterizedTypeName.get(
                            PagingTypeNames.DATA_SOURCE_FACTORY,
                            Integer::class.typeName,
                            typeName
                        )
                    )

                    addMethod(
                        createCreateMethod(
                            depotSQLiteQueryVar = depotSQLiteQueryVar,
                            dbField = dbField,
                            inTransaction = inTransaction,
                            scope = scope
                        )
                    )
                }
                .build()
            addStatement(returnStatementTemplate(), pagedListProvider)
        }
    }

    private fun createCreateMethod(
        depotSQLiteQueryVar: String,
        dbField: FieldSpec,
        inTransaction: Boolean,
        scope: CodeGenScope
    ): MethodSpec = MethodSpec.methodBuilder("create").apply {
        addAnnotation(Override::class.java)
        addModifiers(Modifier.PUBLIC)
        returns(positionalDataSourceQueryResultBinder.typeName)
        val countedBinderScope = scope.fork()
        positionalDataSourceQueryResultBinder.convertAndReturn(
            depotSQLiteQueryVar = depotSQLiteQueryVar,
            canReleaseQuery = true,
            dbField = dbField,
            inTransaction = inTransaction,
            scope = countedBinderScope
        )
        addCode(countedBinderScope.builder().build())
    }.build()
}
