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

package com.zeoflow.depot.solver.types

import com.zeoflow.depot.ext.L
import com.zeoflow.depot.compiler.processing.XProcessingEnv
import com.zeoflow.depot.solver.CodeGenScope
import com.squareup.javapoet.TypeName

/**
 * int to boolean adapter.
 */
object PrimitiveBooleanToIntConverter {
    fun create(processingEnvironment: XProcessingEnv): List<TypeConverter> {
        val tBoolean = processingEnvironment.requireType(TypeName.BOOLEAN)
        val tInt = processingEnvironment.requireType(TypeName.INT)
        return listOf(
            object : TypeConverter(tBoolean, tInt) {
                override fun convert(
                    inputVarName: String,
                    outputVarName: String,
                    scope: CodeGenScope
                ) {
                    scope.builder().addStatement("$L = $L ? 1 : 0", outputVarName, inputVarName)
                }
            },
            object : TypeConverter(tInt, tBoolean) {
                override fun convert(
                    inputVarName: String,
                    outputVarName: String,
                    scope: CodeGenScope
                ) {
                    scope.builder().addStatement("$L = $L != 0", outputVarName, inputVarName)
                }
            }
        )
    }
}
