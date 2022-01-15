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

package com.zeoflow.depot.vo

import com.zeoflow.depot.compiler.processing.XFieldElement
import com.zeoflow.depot.compiler.processing.XNullability
import com.zeoflow.depot.compiler.processing.XType
import com.zeoflow.depot.parser.Collate
import com.zeoflow.depot.parser.SQLTypeAffinity
import com.zeoflow.depot.solver.types.CursorValueReader
import com.zeoflow.depot.solver.types.StatementValueBinder
import capitalize
import com.squareup.javapoet.TypeName
import decapitalize
import java.util.Locale

// used in cache matching, must stay as a data class or implement equals
data class Field(
    val element: XFieldElement,
    val name: String,
    val type: XType,
    var affinity: SQLTypeAffinity?,
    val collate: Collate? = null,
    val columnName: String = name,
    val defaultValue: String? = null,
    // null here means that this field does not belong to parent, instead, it belongs to an
    // embedded child of the main Pojo
    val parent: EmbeddedField? = null,
    // index might be removed when being merged into an Entity
    var indexed: Boolean = false,
    /** Whether the table column for this field should be NOT NULL */
    val nonNull: Boolean = calcNonNull(type, parent)
) : HasSchemaIdentity {
    lateinit var getter: FieldGetter
    lateinit var setter: FieldSetter
    // binds the field into a statement
    var statementBinder: StatementValueBinder? = null
    // reads this field from a cursor column
    var cursorValueReader: CursorValueReader? = null
    val typeName: TypeName by lazy { type.typeName }

    override fun getIdKey(): String {
        return buildString {
            // we don't get the collate information from sqlite so ignoring it here.
            append("$columnName-${affinity?.name ?: SQLTypeAffinity.TEXT.name}-$nonNull")
            // defaultValue was newly added; it should affect the ID only when it is used.
            if (defaultValue != null) {
                append("-defaultValue=$defaultValue")
            }
        }
    }

    /**
     * Used when reporting errors on duplicate names
     */
    fun getPath(): String {
        return if (parent == null) {
            name
        } else {
            "${parent.field.getPath()} > $name"
        }
    }

    private val pathWithDotNotation: String by lazy {
        if (parent == null) {
            name
        } else {
            "${parent.field.pathWithDotNotation}.$name"
        }
    }

    /**
     * List of names that include variations.
     * e.g. if it is mUser, user is added to the list
     * or if it is isAdmin, admin is added to the list
     */
    val nameWithVariations by lazy {
        val result = arrayListOf(name)
        if (name.length > 1) {
            if (name.startsWith('_')) {
                result.add(name.substring(1))
            }
            if (name.startsWith("m") && name[1].isUpperCase()) {
                result.add(name.substring(1).decapitalize(Locale.US))
            }

            if (name.contains('_')) {
                val nameSplits = name.split('_')
                var nameFinal = ""
                for (nameSplit in nameSplits) {
                    if (nameFinal == "") {
                        nameFinal = nameSplit
                    } else {
                        nameFinal += nameSplit.capitalize(Locale.US)
                    }
                }
                result.add(nameFinal)
            }

            if (typeName == TypeName.BOOLEAN || typeName == TypeName.BOOLEAN.box()) {
                if (name.length > 2 && name.startsWith("is") && name[2].isUpperCase()) {
                    result.add(name.substring(2).decapitalize(Locale.US))
                }
                if (name.length > 3 && name.startsWith("has") && name[3].isUpperCase()) {
                    result.add(name.substring(3).decapitalize(Locale.US))
                }
            }
        }
        result
    }

    val getterNameWithVariations by lazy {
        nameWithVariations.map { "get${it.capitalize(Locale.US)}" } +
            if (typeName == TypeName.BOOLEAN || typeName == TypeName.BOOLEAN.box()) {
                nameWithVariations.flatMap {
                    listOf("is${it.capitalize(Locale.US)}", "has${it.capitalize(Locale.US)}")
                }
            } else {
                emptyList()
            }
    }

    val setterNameWithVariations by lazy {
        nameWithVariations.map { "set${it.capitalize(Locale.US)}" }
    }

    /**
     * definition to be used in create query
     */
    fun databaseDefinition(autoIncrementPKey: Boolean): String {
        val columnSpec = StringBuilder("")
        if (autoIncrementPKey) {
            columnSpec.append(" PRIMARY KEY AUTOINCREMENT")
        }
        if (nonNull) {
            columnSpec.append(" NOT NULL")
        }
        if (collate != null) {
            columnSpec.append(" COLLATE ${collate.name}")
        }
        if (defaultValue != null) {
            columnSpec.append(" DEFAULT $defaultValue")
        }
        return "`$columnName` ${(affinity ?: SQLTypeAffinity.TEXT).name}$columnSpec"
    }

    fun toBundle(): com.zeoflow.depot.migration.bundle.FieldBundle = com.zeoflow.depot.migration.bundle.FieldBundle(
        pathWithDotNotation, columnName,
        affinity?.name ?: SQLTypeAffinity.TEXT.name, nonNull, defaultValue
    )

    companion object {
        fun calcNonNull(type: XType, parent: EmbeddedField?): Boolean {
            return XNullability.NONNULL == type.nullability &&
                (parent == null || parent.isNonNullRecursively())
        }
    }
}
