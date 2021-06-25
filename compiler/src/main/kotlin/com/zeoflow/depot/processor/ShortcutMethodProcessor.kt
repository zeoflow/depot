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

import com.zeoflow.depot.ext.isEntityElement
import com.zeoflow.depot.compiler.processing.XAnnotationBox
import com.zeoflow.depot.compiler.processing.XMethodElement
import com.zeoflow.depot.compiler.processing.XType
import com.zeoflow.depot.compiler.processing.XTypeElement
import com.zeoflow.depot.vo.Entity
import com.zeoflow.depot.vo.Pojo
import com.zeoflow.depot.vo.ShortcutEntity
import com.zeoflow.depot.vo.ShortcutQueryParameter
import com.zeoflow.depot.vo.findFieldByColumnName
import kotlin.reflect.KClass

/**
 * Common functionality for shortcut method processors
 */
class ShortcutMethodProcessor(
    baseContext: Context,
    val containing: XType,
    val executableElement: XMethodElement
) {
    val context = baseContext.fork(executableElement)
    private val delegate = MethodProcessorDelegate.createFor(context, containing, executableElement)

    fun <T : Annotation> extractAnnotation(klass: KClass<T>, errorMsg: String): XAnnotationBox<T>? {
        val annotation = executableElement.getAnnotation(klass)
        context.checker.check(annotation != null, executableElement, errorMsg)
        return annotation
    }

    fun extractReturnType() = delegate.extractReturnType()

    fun extractParams(
        targetEntityType: XType?,
        missingParamError: String,
        onValidatePartialEntity: (Entity, Pojo) -> Unit
    ): Pair<Map<String, ShortcutEntity>, List<ShortcutQueryParameter>> {
        val params = delegate.extractParams().map {
            ShortcutParameterProcessor(
                baseContext = context,
                containing = containing,
                element = it
            ).process()
        }
        context.checker.check(params.isNotEmpty(), executableElement, missingParamError)

        val targetEntity = if (targetEntityType != null &&
            !targetEntityType.isTypeOf(Any::class)
        ) {
            val targetTypeElement = targetEntityType.typeElement
            if (targetTypeElement == null) {
                context.logger.e(
                    executableElement,
                    ProcessorErrors.INVALID_TARGET_ENTITY_IN_SHORTCUT_METHOD
                )
                null
            } else {
                processEntity(
                    element = targetTypeElement,
                    onInvalid = {
                        context.logger.e(
                            executableElement,
                            ProcessorErrors.INVALID_TARGET_ENTITY_IN_SHORTCUT_METHOD
                        )
                        return emptyMap<String, ShortcutEntity>() to emptyList()
                    }
                )
            }
        } else {
            null
        }

        val entities = params.filter { it.pojoType != null }.let {
            if (targetEntity != null) {
                extractPartialEntities(targetEntity, it, onValidatePartialEntity)
            } else {
                extractEntities(it)
            }
        }

        return Pair(entities, params)
    }

    private fun extractPartialEntities(
        targetEntity: Entity,
        params: List<ShortcutQueryParameter>,
        onValidatePartialEntity: (Entity, Pojo) -> Unit
    ) = params.associateBy(
        { it.name },
        { param ->
            if (targetEntity.type.isSameType(param.pojoType!!)) {
                ShortcutEntity(entity = targetEntity, partialEntity = null)
            } else {
                // Target entity and pojo param are not the same, process and validate partial entity.
                val pojoTypeElement = param.pojoType.typeElement
                val pojo = if (pojoTypeElement == null) {
                    context.logger.e(
                        targetEntity.element,
                        ProcessorErrors.shortcutMethodArgumentMustBeAClass(
                            typeName = param.pojoType.typeName
                        )
                    )
                    null
                } else {
                    PojoProcessor.createFor(
                        context = context,
                        element = pojoTypeElement,
                        bindingScope = FieldProcessor.BindingScope.BIND_TO_STMT,
                        parent = null
                    ).process().also { pojo ->
                        pojo.fields
                            .filter { targetEntity.findFieldByColumnName(it.columnName) == null }
                            .forEach {
                                context.logger.e(
                                    it.element,
                                    ProcessorErrors.cannotFindAsEntityField(
                                        targetEntity.typeName.toString()
                                    )

                                )
                            }

                        if (pojo.relations.isNotEmpty()) {
                            // TODO: Support Pojos with relations.
                            context.logger.e(
                                pojo.element,
                                ProcessorErrors.INVALID_RELATION_IN_PARTIAL_ENTITY
                            )
                        }

                        if (pojo.fields.isEmpty()) {
                            context.logger.e(
                                executableElement,
                                ProcessorErrors.noColumnsInPartialEntity(
                                    partialEntityName = pojo.typeName.toString()
                                )
                            )
                        }
                        onValidatePartialEntity(targetEntity, pojo)
                    }
                }
                ShortcutEntity(entity = targetEntity, partialEntity = pojo)
            }
        }
    )

    private fun extractEntities(params: List<ShortcutQueryParameter>) =
        params.mapNotNull {
            val entitiyTypeElement = it.pojoType?.typeElement
            if (entitiyTypeElement == null) {
                context.logger.e(
                    it.element,
                    ProcessorErrors.CANNOT_FIND_ENTITY_FOR_SHORTCUT_QUERY_PARAMETER
                )
                null
            } else {
                val entity = processEntity(
                    element = entitiyTypeElement,
                    onInvalid = {
                        context.logger.e(
                            it.element,
                            ProcessorErrors.CANNOT_FIND_ENTITY_FOR_SHORTCUT_QUERY_PARAMETER
                        )
                        return@mapNotNull null
                    }
                )
                it.name to ShortcutEntity(entity = entity!!, partialEntity = null)
            }
        }.toMap()

    private inline fun processEntity(element: XTypeElement, onInvalid: () -> Unit) =
        if (element.isEntityElement()) {
            EntityProcessor(
                context = context,
                element = element
            ).process()
        } else {
            onInvalid()
            null
        }

    fun findInsertMethodBinder(
        returnType: XType,
        params: List<ShortcutQueryParameter>
    ) = delegate.findInsertMethodBinder(returnType, params)

    fun findDeleteOrUpdateMethodBinder(returnType: XType) =
        delegate.findDeleteOrUpdateMethodBinder(returnType)
}
