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

package com.zeoflow.depot.compiler.processing.ksp

import com.zeoflow.depot.compiler.processing.XMethodType
import com.zeoflow.depot.compiler.processing.XSuspendMethodType
import com.zeoflow.depot.compiler.processing.XType
import com.squareup.javapoet.TypeVariableName

internal sealed class KspMethodType(
    val env: KspProcessingEnv,
    val origin: KspMethodElement,
    val containing: KspType?
) : XMethodType {
    override val parameterTypes: List<XType> by lazy {
        if (containing == null) {
            origin.parameters.map {
                it.type
            }
        } else {
            origin.parameters.map {
                it.asMemberOf(containing)
            }
        }
    }

    override val typeVariableNames: List<TypeVariableName> by lazy {
        origin.declaration.typeParameters.map {
            val typeParameterBounds = it.bounds.map {
                it.typeName(env.resolver)
            }.toList().toTypedArray()
            TypeVariableName.get(
                it.name.asString(),
                *typeParameterBounds
            )
        }
    }

    /**
     * Creates a MethodType where variance is inherited for java code generation.
     *
     * see [OverrideVarianceResolver] for details.
     */
    fun inheritVarianceForOverride(): XMethodType {
        return OverrideVarianceResolver(env, this).resolve()
    }

    private class KspNormalMethodType(
        env: KspProcessingEnv,
        origin: KspMethodElement,
        containing: KspType?
    ) : KspMethodType(env, origin, containing) {
        override val returnType: XType by lazy {
            // b/160258066
            // we may need to box the return type if it is overriding a generic, hence, we should
            // use the declaration of the overridee if available when deciding nullability
            val overridee = origin.declaration.findOverridee()
            env.wrap(
                originatingReference = (overridee?.returnType ?: origin.declaration.returnType)!!,
                ksType = origin.declaration.returnTypeAsMemberOf(
                    ksType = containing?.ksType
                )
            )
        }
    }

    private class KspSuspendMethodType(
        env: KspProcessingEnv,
        origin: KspMethodElement,
        containing: KspType?
    ) : KspMethodType(env, origin, containing), XSuspendMethodType {
        override val returnType: XType
            // suspend functions always return Any?, no need to call asMemberOf
            get() = origin.returnType

        override fun getSuspendFunctionReturnType(): XType {
            // suspend functions work w/ continuation so it is always boxed
            return env.wrap(
                ksType = origin.declaration.returnTypeAsMemberOf(
                    ksType = containing?.ksType
                ),
                allowPrimitives = false
            )
        }
    }

    companion object {
        fun create(
            env: KspProcessingEnv,
            origin: KspMethodElement,
            containing: KspType?
        ) = if (origin.isSuspendFunction()) {
            KspSuspendMethodType(env, origin, containing)
        } else {
            KspNormalMethodType(env, origin, containing)
        }
    }
}
