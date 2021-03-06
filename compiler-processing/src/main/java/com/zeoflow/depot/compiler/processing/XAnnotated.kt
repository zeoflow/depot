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

package com.zeoflow.depot.compiler.processing

import kotlin.reflect.KClass

/**
 * Common interface implemented by elements that might have annotations.
 */
interface XAnnotated {
    /**
     * Gets the list of annotations with the given type.
     *
     * For repeated annotations declared in Java code, please use the repeated annotation type,
     * not the container. Calling this method with a container annotation will have inconsistent
     * behaviour between Java AP and KSP.
     *
     * @see [hasAnnotation]
     * @see [hasAnnotationWithPackage]
     */
    fun <T : Annotation> getAnnotations(
        annotation: KClass<T>
    ): List<XAnnotationBox<T>>

    /**
     * Returns `true` if this element is annotated with the given [annotation].
     *
     * For repeated annotations declared in Java code, please use the repeated annotation type,
     * not the container. Calling this method with a container annotation will have inconsistent
     * behaviour between Java AP and KSP.
     * @see [hasAnyOf]
     */
    fun hasAnnotation(
        annotation: KClass<out Annotation>
    ): Boolean

    /**
     * Returns `true` if this element has an annotation that is declared in the given package.
     */
    // a very sad method but helps avoid abstraction annotation
    fun hasAnnotationWithPackage(pkg: String): Boolean

    /**
     * Returns `true` if this element has one of the [annotations].
     */
    fun hasAnyOf(vararg annotations: KClass<out Annotation>) = annotations.any(this::hasAnnotation)

    @Deprecated(
        replaceWith = ReplaceWith("getAnnotation(annotation)"),
        message = "Use getAnnotation(not repeatable) or getAnnotations (repeatable)"
    )
    fun <T : Annotation> toAnnotationBox(annotation: KClass<T>): XAnnotationBox<T>? =
        getAnnotation(annotation)

    /**
     * If the current element has an annotation with the given [annotation] class, a boxed instance
     * of it will be returned where fields can be read. Otherwise, `null` value is returned.
     *
     * @see [hasAnnotation]
     * @see [getAnnotations]
     * @see [hasAnnotationWithPackage]
     */
    fun <T : Annotation> getAnnotation(annotation: KClass<T>): XAnnotationBox<T>? {
        return getAnnotations(annotation).firstOrNull()
    }
}