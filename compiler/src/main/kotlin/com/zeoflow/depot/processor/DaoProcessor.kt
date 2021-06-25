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

package com.zeoflow.depot.processor

import com.zeoflow.depot.compiler.processing.XConstructorElement
import com.zeoflow.depot.compiler.processing.XMethodElement
import com.zeoflow.depot.compiler.processing.XType
import com.zeoflow.depot.compiler.processing.XTypeElement
import com.zeoflow.depot.verifier.DatabaseVerifier
import com.zeoflow.depot.vo.Dao
import com.zeoflow.depot.vo.KotlinBoxedPrimitiveMethodDelegate
import com.zeoflow.depot.vo.KotlinDefaultMethodDelegate

class DaoProcessor(
    baseContext: Context,
    val element: XTypeElement,
    val dbType: XType,
    val dbVerifier: DatabaseVerifier?
) {
    val context = baseContext.fork(element)

    companion object {
        val PROCESSED_ANNOTATIONS = listOf(
            com.zeoflow.depot.Insert::class, com.zeoflow.depot.Delete::class, com.zeoflow.depot.Query::class,
            com.zeoflow.depot.Update::class, com.zeoflow.depot.RawQuery::class
        )
    }

    fun process(): Dao {
        context.checker.hasAnnotation(
            element, com.zeoflow.depot.Dao::class,
            ProcessorErrors.DAO_MUST_BE_ANNOTATED_WITH_DAO
        )
        context.checker.check(
            element.isAbstract() || element.isInterface(),
            element, ProcessorErrors.DAO_MUST_BE_AN_ABSTRACT_CLASS_OR_AN_INTERFACE
        )

        val declaredType = element.type
        val allMethods = element.getAllMethods()
        val methods = allMethods
            .filter {
                it.isAbstract() && !it.hasKotlinDefaultImpl()
            }.groupBy { method ->
                context.checker.check(
                    PROCESSED_ANNOTATIONS.count { method.hasAnnotation(it) } <= 1, method,
                    ProcessorErrors.INVALID_ANNOTATION_COUNT_IN_DAO_METHOD
                )
                if (method.hasAnnotation(com.zeoflow.depot.Query::class)) {
                    com.zeoflow.depot.Query::class
                } else if (method.hasAnnotation(com.zeoflow.depot.Insert::class)) {
                    com.zeoflow.depot.Insert::class
                } else if (method.hasAnnotation(com.zeoflow.depot.Delete::class)) {
                    com.zeoflow.depot.Delete::class
                } else if (method.hasAnnotation(com.zeoflow.depot.Update::class)) {
                    com.zeoflow.depot.Update::class
                } else if (method.hasAnnotation(com.zeoflow.depot.RawQuery::class)) {
                    com.zeoflow.depot.RawQuery::class
                } else {
                    Any::class
                }
            }

        val processorVerifier = if (element.hasAnnotation(com.zeoflow.depot.SkipQueryVerification::class) ||
            element.hasAnnotation(com.zeoflow.depot.RawQuery::class)
        ) {
            null
        } else {
            dbVerifier
        }

        val queryMethods = methods[com.zeoflow.depot.Query::class]?.map {
            QueryMethodProcessor(
                baseContext = context,
                containing = declaredType,
                executableElement = it,
                dbVerifier = processorVerifier
            ).process()
        } ?: emptyList()

        val rawQueryMethods = methods[com.zeoflow.depot.RawQuery::class]?.map {
            RawQueryMethodProcessor(
                baseContext = context,
                containing = declaredType,
                executableElement = it
            ).process()
        } ?: emptyList()

        val insertionMethods = methods[com.zeoflow.depot.Insert::class]?.map {
            InsertionMethodProcessor(
                baseContext = context,
                containing = declaredType,
                executableElement = it
            ).process()
        } ?: emptyList()

        val deletionMethods = methods[com.zeoflow.depot.Delete::class]?.map {
            DeletionMethodProcessor(
                baseContext = context,
                containing = declaredType,
                executableElement = it
            ).process()
        } ?: emptyList()

        val updateMethods = methods[com.zeoflow.depot.Update::class]?.map {
            UpdateMethodProcessor(
                baseContext = context,
                containing = declaredType,
                executableElement = it
            ).process()
        } ?: emptyList()

        val transactionMethods = allMethods.filter { member ->
            member.hasAnnotation(com.zeoflow.depot.Transaction::class) &&
                PROCESSED_ANNOTATIONS.none { member.hasAnnotation(it) }
        }.map {
            TransactionMethodProcessor(
                baseContext = context,
                containing = declaredType,
                executableElement = it
            ).process()
        }

        // Only try to find kotlin boxed delegating methods when the dao extends a class or
        // implements an interface since otherwise there are no duplicated method generated by
        // Kotlin.
        val unannotatedMethods = methods[Any::class] ?: emptyList<XMethodElement>()
        val delegatingMethods =
            if (element.superType != null ||
                element.getSuperInterfaceElements().isNotEmpty()
            ) {
                matchKotlinBoxedPrimitiveMethods(
                    unannotatedMethods,
                    methods.values.flatten() - unannotatedMethods
                )
            } else {
                emptyList()
            }

        val kotlinDefaultMethodDelegates = if (element.isInterface()) {
            val allProcessedMethods =
                methods.values.flatten() + transactionMethods.map { it.element }
            allMethods.filterNot {
                allProcessedMethods.contains(it)
            }.mapNotNull { method ->
                if (method.hasKotlinDefaultImpl()) {
                    KotlinDefaultMethodDelegate(
                        element = method
                    )
                } else {
                    null
                }
            }
        } else {
            emptyList()
        }

        val constructors = element.getConstructors()
        val goodConstructor = constructors.firstOrNull {
            it.parameters.size == 1 &&
                it.parameters[0].type.isAssignableFrom(dbType)
        }
        val constructorParamType = if (goodConstructor != null) {
            goodConstructor.parameters[0].type.typeName
        } else {
            validateEmptyConstructor(constructors)
            null
        }

        val type = declaredType.typeName
        context.checker.notUnbound(
            type, element,
            ProcessorErrors.CANNOT_USE_UNBOUND_GENERICS_IN_DAO_CLASSES
        )

        (unannotatedMethods - delegatingMethods.map { it.element }).forEach { method ->
            context.logger.e(method, ProcessorErrors.INVALID_ANNOTATION_COUNT_IN_DAO_METHOD)
        }

        return Dao(
            element = element,
            type = declaredType,
            queryMethods = queryMethods,
            rawQueryMethods = rawQueryMethods,
            insertionMethods = insertionMethods,
            deletionMethods = deletionMethods,
            updateMethods = updateMethods,
            transactionMethods = transactionMethods,
            delegatingMethods = delegatingMethods,
            kotlinDefaultMethodDelegates = kotlinDefaultMethodDelegates,
            constructorParamType = constructorParamType
        )
    }

    private fun validateEmptyConstructor(constructors: List<XConstructorElement>) {
        if (constructors.isNotEmpty() && constructors.all { it.parameters.isNotEmpty() }) {
            context.logger.e(
                element,
                ProcessorErrors.daoMustHaveMatchingConstructor(
                    element.qualifiedName, dbType.typeName.toString()
                )
            )
        }
    }

    private fun matchKotlinBoxedPrimitiveMethods(
        unannotatedMethods: List<XMethodElement>,
        annotatedMethods: List<XMethodElement>
    ) = unannotatedMethods.mapNotNull { unannotated ->
        annotatedMethods.firstOrNull {
            if (it.name != unannotated.name) {
                return@firstOrNull false
            }
            if (!it.returnType.boxed().isSameType(unannotated.returnType.boxed())) {
                return@firstOrNull false
            }
            if (it.parameters.size != unannotated.parameters.size) {
                return@firstOrNull false
            }
            for (i in it.parameters.indices) {
                if (it.parameters[i].type.boxed() != unannotated.parameters[i].type.boxed()) {
                    return@firstOrNull false
                }
            }
            return@firstOrNull true
        }?.let { matchingMethod -> KotlinBoxedPrimitiveMethodDelegate(unannotated, matchingMethod) }
    }
}