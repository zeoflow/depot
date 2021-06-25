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

package com.zeoflow.depot.processor.autovalue

import com.zeoflow.depot.compiler.processing.XExecutableElement
import com.zeoflow.depot.compiler.processing.XType
import com.zeoflow.depot.compiler.processing.XTypeElement
import com.zeoflow.depot.processor.Context
import com.zeoflow.depot.processor.PojoProcessor
import com.zeoflow.depot.processor.PojoProcessor.Companion.TARGET_METHOD_ANNOTATIONS
import com.zeoflow.depot.processor.ProcessorErrors
import com.zeoflow.depot.vo.Constructor
import com.zeoflow.depot.vo.EmbeddedField
import com.zeoflow.depot.vo.Field
import com.zeoflow.depot.vo.Pojo
import com.zeoflow.depot.vo.Warning
import com.google.auto.value.AutoValue.CopyAnnotations

/**
 * Delegate to process generated AutoValue class as a Pojo.
 */
class AutoValuePojoProcessorDelegate(
    private val context: Context,
    private val autoValueElement: XTypeElement
) : PojoProcessor.Delegate {

    private val autoValueDeclaredType: XType by lazy {
        autoValueElement.type
    }

    override fun onPreProcess(element: XTypeElement) {
        val allMethods = autoValueElement.getAllMethods()
        val autoValueAbstractGetters = allMethods
            .filter { it.isAbstract() && it.parameters.size == 0 }

        // Warn about missing @AutoValue.CopyAnnotations in the property getters.
        autoValueAbstractGetters.forEach {
            val hasDepotAnnotation = it.hasAnnotationWithPackage("com.zeoflow.depot")
            if (hasDepotAnnotation && !it.hasAnnotation(CopyAnnotations::class)) {
                context.logger.w(
                    Warning.MISSING_COPY_ANNOTATIONS, it,
                    ProcessorErrors.MISSING_COPY_ANNOTATIONS
                )
            }
        }

        // Check that certain Depot annotations with @Target(METHOD) are not used in methods other
        // than the auto value abstract getters.
        (allMethods - autoValueAbstractGetters)
            .filter { it.hasAnyOf(*TARGET_METHOD_ANNOTATIONS) }
            .forEach { method ->
                val annotationName = TARGET_METHOD_ANNOTATIONS.first { method.hasAnnotation(it) }
                    .java.simpleName
                context.logger.e(
                    method,
                    ProcessorErrors.invalidAnnotationTarget(annotationName, method.kindName())
                )
            }
    }

    override fun findConstructors(element: XTypeElement): List<XExecutableElement> {
        return autoValueElement.getDeclaredMethods().filter {
            it.isStatic() &&
                !it.hasAnnotation(com.zeoflow.depot.Ignore::class) &&
                !it.isPrivate() &&
                it.returnType.isSameType(autoValueElement.type)
        }
    }

    override fun createPojo(
        element: XTypeElement,
        declaredType: XType,
        fields: List<Field>,
        embeddedFields: List<EmbeddedField>,
        relations: List<com.zeoflow.depot.vo.Relation>,
        constructor: Constructor?
    ): Pojo {
        return Pojo(
            element = element,
            type = autoValueDeclaredType,
            fields = fields,
            embeddedFields = embeddedFields,
            relations = relations,
            constructor = constructor
        )
    }

    companion object {
        /**
         * Gets the generated class name of an AutoValue annotated class.
         *
         * This is the same naming strategy used by AutoValue's processor.
         */
        fun getGeneratedClassName(element: XTypeElement): String {
            var type = element
            var name = type.name
            while (type.enclosingTypeElement != null) {
                type = type.enclosingTypeElement!!
                name = "${type.name}_$name"
            }
            val pkg = type.packageName
            return "$pkg${if (pkg.isEmpty()) "" else "."}AutoValue_$name"
        }
    }
}