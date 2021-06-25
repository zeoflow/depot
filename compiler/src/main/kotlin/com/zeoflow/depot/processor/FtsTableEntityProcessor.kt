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

import com.zeoflow.depot.FtsOptions.MatchInfo
import com.zeoflow.depot.FtsOptions.Order
import com.zeoflow.depot.compiler.processing.XAnnotationBox
import com.zeoflow.depot.compiler.processing.XType
import com.zeoflow.depot.compiler.processing.XTypeElement
import com.zeoflow.depot.parser.FtsVersion
import com.zeoflow.depot.parser.SQLTypeAffinity
import com.zeoflow.depot.processor.EntityProcessor.Companion.extractForeignKeys
import com.zeoflow.depot.processor.EntityProcessor.Companion.extractIndices
import com.zeoflow.depot.processor.EntityProcessor.Companion.extractTableName
import com.zeoflow.depot.processor.cache.Cache
import com.zeoflow.depot.vo.Entity
import com.zeoflow.depot.vo.Field
import com.zeoflow.depot.vo.Fields
import com.zeoflow.depot.vo.FtsEntity
import com.zeoflow.depot.vo.FtsOptions
import com.zeoflow.depot.vo.LanguageId
import com.zeoflow.depot.vo.PrimaryKey
import com.zeoflow.depot.vo.columnNames

class FtsTableEntityProcessor internal constructor(
    baseContext: Context,
    val element: XTypeElement,
    private val referenceStack: LinkedHashSet<String> = LinkedHashSet()
) : EntityProcessor {

    val context = baseContext.fork(element)

    override fun process(): FtsEntity {
        return context.cache.entities.get(Cache.EntityKey(element)) {
            doProcess()
        } as FtsEntity
    }

    private fun doProcess(): FtsEntity {
        context.checker.hasAnnotation(
            element, com.zeoflow.depot.Entity::class,
            ProcessorErrors.ENTITY_MUST_BE_ANNOTATED_WITH_ENTITY
        )
        val entityAnnotation = element.getAnnotation(com.zeoflow.depot.Entity::class)
        val tableName: String
        if (entityAnnotation != null) {
            tableName = extractTableName(element, entityAnnotation.value)
            context.checker.check(
                extractIndices(entityAnnotation, tableName).isEmpty(),
                element, ProcessorErrors.INDICES_IN_FTS_ENTITY
            )
            context.checker.check(
                extractForeignKeys(entityAnnotation).isEmpty(),
                element, ProcessorErrors.FOREIGN_KEYS_IN_FTS_ENTITY
            )
        } else {
            tableName = element.name
        }

        val pojo = PojoProcessor.createFor(
            context = context,
            element = element,
            bindingScope = FieldProcessor.BindingScope.TWO_WAY,
            parent = null,
            referenceStack = referenceStack
        ).process()

        context.checker.check(pojo.relations.isEmpty(), element, ProcessorErrors.RELATION_IN_ENTITY)

        val (ftsVersion, ftsOptions) = if (element.hasAnnotation(com.zeoflow.depot.Fts3::class)) {
            FtsVersion.FTS3 to getFts3Options(element.getAnnotation(com.zeoflow.depot.Fts3::class)!!)
        } else {
            FtsVersion.FTS4 to getFts4Options(element.getAnnotation(com.zeoflow.depot.Fts4::class)!!)
        }

        val shadowTableName = if (ftsOptions.contentEntity != null) {
            // In 'external content' mode the FTS table content is in another table.
            // See: https://www.sqlite.org/fts3.html#_external_content_fts4_tables_
            ftsOptions.contentEntity.tableName
        } else {
            // The %_content table contains the unadulterated data inserted by the user into the FTS
            // virtual table. See: https://www.sqlite.org/fts3.html#shadow_tables
            "${tableName}_content"
        }

        val primaryKey = findAndValidatePrimaryKey(entityAnnotation, pojo.fields)
        findAndValidateLanguageId(pojo.fields, ftsOptions.languageIdColumnName)

        val missingNotIndexed = ftsOptions.notIndexedColumns - pojo.columnNames
        context.checker.check(
            missingNotIndexed.isEmpty(), element,
            ProcessorErrors.missingNotIndexedField(missingNotIndexed)
        )

        context.checker.check(
            ftsOptions.prefixSizes.all { it > 0 },
            element, ProcessorErrors.INVALID_FTS_ENTITY_PREFIX_SIZES
        )

        val entity = FtsEntity(
            element = element,
            tableName = tableName,
            type = pojo.type,
            fields = pojo.fields,
            embeddedFields = pojo.embeddedFields,
            primaryKey = primaryKey,
            constructor = pojo.constructor,
            ftsVersion = ftsVersion,
            ftsOptions = ftsOptions,
            shadowTableName = shadowTableName
        )

        validateExternalContentEntity(entity)

        return entity
    }

    private fun getFts3Options(annotation: XAnnotationBox<com.zeoflow.depot.Fts3>) =
        FtsOptions(
            tokenizer = annotation.value.tokenizer,
            tokenizerArgs = annotation.value.tokenizerArgs.asList(),
            contentEntity = null,
            languageIdColumnName = "",
            matchInfo = MatchInfo.FTS4,
            notIndexedColumns = emptyList(),
            prefixSizes = emptyList(),
            preferredOrder = Order.ASC
        )

    private fun getFts4Options(annotation: XAnnotationBox<com.zeoflow.depot.Fts4>): FtsOptions {
        val contentEntity: Entity? = getContentEntity(annotation.getAsType("contentEntity"))
        return FtsOptions(
            tokenizer = annotation.value.tokenizer,
            tokenizerArgs = annotation.value.tokenizerArgs.asList(),
            contentEntity = contentEntity,
            languageIdColumnName = annotation.value.languageId,
            matchInfo = annotation.value.matchInfo,
            notIndexedColumns = annotation.value.notIndexed.asList(),
            prefixSizes = annotation.value.prefix.asList(),
            preferredOrder = annotation.value.order
        )
    }

    private fun getContentEntity(entityType: XType?): Entity? {
        if (entityType == null) {
            context.logger.e(element, ProcessorErrors.FTS_EXTERNAL_CONTENT_CANNOT_FIND_ENTITY)
            return null
        }

        val defaultType = context.processingEnv.requireType(Object::class)
        if (entityType.isSameType(defaultType)) {
            return null
        }
        val contentEntityElement = entityType.typeElement
        if (contentEntityElement == null) {
            context.logger.e(element, ProcessorErrors.FTS_EXTERNAL_CONTENT_CANNOT_FIND_ENTITY)
            return null
        }
        if (!contentEntityElement.hasAnnotation(com.zeoflow.depot.Entity::class)) {
            context.logger.e(
                contentEntityElement,
                ProcessorErrors.externalContentNotAnEntity(
                    contentEntityElement.className.canonicalName()
                )
            )
            return null
        }
        return EntityProcessor(context, contentEntityElement, referenceStack).process()
    }

    private fun findAndValidatePrimaryKey(
        entityAnnotation: XAnnotationBox<com.zeoflow.depot.Entity>?,
        fields: List<Field>
    ): PrimaryKey {
        val keysFromEntityAnnotation =
            entityAnnotation?.value?.primaryKeys?.mapNotNull { pkColumnName ->
                val field = fields.firstOrNull { it.columnName == pkColumnName }
                context.checker.check(
                    field != null, element,
                    ProcessorErrors.primaryKeyColumnDoesNotExist(
                        pkColumnName,
                        fields.map { it.columnName }
                    )
                )
                field?.let { pkField ->
                    PrimaryKey(
                        declaredIn = pkField.element.enclosingElement,
                        fields = Fields(pkField),
                        autoGenerateId = true
                    )
                }
            } ?: emptyList()

        val keysFromPrimaryKeyAnnotations = fields.mapNotNull { field ->
            if (field.element.hasAnnotation(com.zeoflow.depot.PrimaryKey::class)) {
                PrimaryKey(
                    declaredIn = field.element.enclosingElement,
                    fields = Fields(field),
                    autoGenerateId = true
                )
            } else {
                null
            }
        }
        val primaryKeys = keysFromEntityAnnotation + keysFromPrimaryKeyAnnotations
        if (primaryKeys.isEmpty()) {
            fields.firstOrNull { it.columnName == "rowid" }?.let {
                context.checker.check(
                    it.element.hasAnnotation(com.zeoflow.depot.PrimaryKey::class),
                    it.element, ProcessorErrors.MISSING_PRIMARY_KEYS_ANNOTATION_IN_ROW_ID
                )
            }
            return PrimaryKey.MISSING
        }
        context.checker.check(
            primaryKeys.size == 1, element,
            ProcessorErrors.TOO_MANY_PRIMARY_KEYS_IN_FTS_ENTITY
        )
        val primaryKey = primaryKeys.first()
        context.checker.check(
            primaryKey.columnNames.first() == "rowid",
            primaryKey.declaredIn ?: element,
            ProcessorErrors.INVALID_FTS_ENTITY_PRIMARY_KEY_NAME
        )
        context.checker.check(
            primaryKey.fields.first().affinity == SQLTypeAffinity.INTEGER,
            primaryKey.declaredIn ?: element,
            ProcessorErrors.INVALID_FTS_ENTITY_PRIMARY_KEY_AFFINITY
        )
        return primaryKey
    }

    private fun validateExternalContentEntity(ftsEntity: FtsEntity) {
        val contentEntity = ftsEntity.ftsOptions.contentEntity
        if (contentEntity == null) {
            return
        }

        // Verify external content columns are a superset of those defined in the FtsEntity
        ftsEntity.nonHiddenFields.filterNot {
            contentEntity.fields.any { contentField -> contentField.columnName == it.columnName }
        }.forEach {
            context.logger.e(
                it.element,
                ProcessorErrors.missingFtsContentField(
                    element.qualifiedName, it.columnName,
                    contentEntity.element.qualifiedName
                )
            )
        }
    }

    private fun findAndValidateLanguageId(
        fields: List<Field>,
        languageIdColumnName: String
    ): LanguageId {
        if (languageIdColumnName.isEmpty()) {
            return LanguageId.MISSING
        }

        val languageIdField = fields.firstOrNull { it.columnName == languageIdColumnName }
        if (languageIdField == null) {
            context.logger.e(element, ProcessorErrors.missingLanguageIdField(languageIdColumnName))
            return LanguageId.MISSING
        }

        context.checker.check(
            languageIdField.affinity == SQLTypeAffinity.INTEGER,
            languageIdField.element, ProcessorErrors.INVALID_FTS_ENTITY_LANGUAGE_ID_AFFINITY
        )
        return LanguageId(languageIdField.element, languageIdField)
    }
}