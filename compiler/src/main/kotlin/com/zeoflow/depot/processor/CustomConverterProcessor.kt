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

import com.zeoflow.depot.compiler.processing.XElement
import com.zeoflow.depot.compiler.processing.XMethodElement
import com.zeoflow.depot.compiler.processing.XType
import com.zeoflow.depot.compiler.processing.XTypeElement
import com.zeoflow.depot.compiler.processing.isVoid
import com.zeoflow.depot.processor.ProcessorErrors.INNER_CLASS_TYPE_CONVERTER_MUST_BE_STATIC
import com.zeoflow.depot.processor.ProcessorErrors.TYPE_CONVERTER_BAD_RETURN_TYPE
import com.zeoflow.depot.processor.ProcessorErrors.TYPE_CONVERTER_EMPTY_CLASS
import com.zeoflow.depot.processor.ProcessorErrors.TYPE_CONVERTER_MISSING_NOARG_CONSTRUCTOR
import com.zeoflow.depot.processor.ProcessorErrors.TYPE_CONVERTER_MUST_BE_PUBLIC
import com.zeoflow.depot.processor.ProcessorErrors.TYPE_CONVERTER_MUST_RECEIVE_1_PARAM
import com.zeoflow.depot.processor.ProcessorErrors.TYPE_CONVERTER_UNBOUND_GENERIC
import com.zeoflow.depot.solver.types.CustomTypeConverterWrapper
import com.zeoflow.depot.vo.CustomTypeConverter
import java.util.LinkedHashSet

/**
 * Processes classes that are referenced in TypeConverters annotations.
 */
class CustomConverterProcessor(val context: Context, val element: XTypeElement) {
    companion object {
        private fun XType.isInvalidReturnType() =
            isError() || isVoid() || isNone()

        fun findConverters(context: Context, element: XElement): ProcessResult {
            val annotation = element.getAnnotation(com.zeoflow.depot.TypeConverters::class)
            return annotation?.let {
                val classes = it.getAsTypeList("value")
                    .mapTo(LinkedHashSet()) { it }
                val converters = classes.flatMap {
                    val typeElement = it.typeElement
                    if (typeElement == null) {
                        context.logger.e(
                            element,
                            ProcessorErrors.typeConverterMustBeDeclared(it.typeName)
                        )
                        emptyList()
                    } else {
                        CustomConverterProcessor(context, typeElement).process()
                    }
                }
                reportDuplicates(context, converters)
                ProcessResult(classes, converters.map(::CustomTypeConverterWrapper))
            } ?: ProcessResult.EMPTY
        }

        private fun reportDuplicates(context: Context, converters: List<CustomTypeConverter>) {
            converters
                .groupBy { it.from.typeName to it.to.typeName }
                .filterValues { it.size > 1 }
                .values.forEach {
                    it.forEach { converter ->
                        context.logger.e(
                            converter.method,
                            ProcessorErrors
                                .duplicateTypeConverters(it.minus(converter))
                        )
                    }
                }
        }
    }

    fun process(): List<CustomTypeConverter> {
        val methods = element.getAllMethods()
        val converterMethods = methods.filter {
            it.hasAnnotation(com.zeoflow.depot.TypeConverter::class)
        }
        val isProvidedConverter = element.hasAnnotation(com.zeoflow.depot.ProvidedTypeConverter::class)
        context.checker.check(converterMethods.isNotEmpty(), element, TYPE_CONVERTER_EMPTY_CLASS)
        val allStatic = converterMethods.all { it.isStatic() }
        val constructors = element.getConstructors()
        val isKotlinObjectDeclaration = element.isKotlinObject()
        if (!isProvidedConverter) {
            context.checker.check(
                element.enclosingTypeElement == null || element.isStatic(),
                element,
                INNER_CLASS_TYPE_CONVERTER_MUST_BE_STATIC
            )
            context.checker.check(
                isKotlinObjectDeclaration || allStatic || constructors.isEmpty() ||
                    constructors.any {
                        it.parameters.isEmpty()
                    },
                element, TYPE_CONVERTER_MISSING_NOARG_CONSTRUCTOR
            )
        }
        return converterMethods.mapNotNull {
            processMethod(
                container = element.type,
                isContainerKotlinObject = isKotlinObjectDeclaration,
                methodElement = it,
                isProvidedConverter = isProvidedConverter
            )
        }
    }

    private fun processMethod(
        container: XType,
        methodElement: XMethodElement,
        isContainerKotlinObject: Boolean,
        isProvidedConverter: Boolean
    ): CustomTypeConverter? {
        val asMember = methodElement.asMemberOf(container)
        val returnType = asMember.returnType
        val invalidReturnType = returnType.isInvalidReturnType()
        context.checker.check(
            methodElement.isPublic(), methodElement, TYPE_CONVERTER_MUST_BE_PUBLIC
        )
        if (invalidReturnType) {
            context.logger.e(methodElement, TYPE_CONVERTER_BAD_RETURN_TYPE)
            return null
        }
        val returnTypeName = returnType.typeName
        context.checker.notUnbound(
            returnTypeName, methodElement,
            TYPE_CONVERTER_UNBOUND_GENERIC
        )
        val params = methodElement.parameters
        if (params.size != 1) {
            context.logger.e(methodElement, TYPE_CONVERTER_MUST_RECEIVE_1_PARAM)
            return null
        }
        val param = params.map {
            it.asMemberOf(container)
        }.first()
        context.checker.notUnbound(param.typeName, params[0], TYPE_CONVERTER_UNBOUND_GENERIC)
        return CustomTypeConverter(
            enclosingClass = container,
            isEnclosingClassKotlinObject = isContainerKotlinObject,
            method = methodElement,
            from = param,
            to = returnType,
            isProvidedConverter = isProvidedConverter
        )
    }

    /**
     * Order of classes is important hence they are a LinkedHashSet not a set.
     */
    open class ProcessResult(
        val classes: LinkedHashSet<XType>,
        val converters: List<CustomTypeConverterWrapper>
    ) {
        object EMPTY : ProcessResult(LinkedHashSet(), emptyList())

        operator fun plus(other: ProcessResult): ProcessResult {
            val newClasses = LinkedHashSet<XType>()
            newClasses.addAll(classes)
            newClasses.addAll(other.classes)
            return ProcessResult(newClasses, converters + other.converters)
        }
    }
}
