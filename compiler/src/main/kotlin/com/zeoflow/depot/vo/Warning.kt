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

import java.util.Locale

/**
 * Internal representation of supported warnings
 */
// If these warnings are updated also update com.zeoflow.depot.DepotWarnings
enum class Warning(val publicKey: String) {
    ALL("ALL"),
    CURSOR_MISMATCH("DEPOT_CURSOR_MISMATCH"),
    MISSING_JAVA_TMP_DIR("DEPOT_MISSING_JAVA_TMP_DIR"),
    CANNOT_CREATE_VERIFICATION_DATABASE("DEPOT_CANNOT_CREATE_VERIFICATION_DATABASE"),
    PRIMARY_KEY_FROM_EMBEDDED_IS_DROPPED("DEPOT_EMBEDDED_PRIMARY_KEY_IS_DROPPED"),
    INDEX_FROM_EMBEDDED_FIELD_IS_DROPPED("DEPOT_EMBEDDED_INDEX_IS_DROPPED"),
    INDEX_FROM_EMBEDDED_ENTITY_IS_DROPPED("DEPOT_EMBEDDED_ENTITY_INDEX_IS_DROPPED"),
    INDEX_FROM_PARENT_IS_DROPPED("DEPOT_PARENT_INDEX_IS_DROPPED"),
    INDEX_FROM_PARENT_FIELD_IS_DROPPED("DEPOT_PARENT_FIELD_INDEX_IS_DROPPED"),
    RELATION_TYPE_MISMATCH("DEPOT_RELATION_TYPE_MISMATCH"),
    MISSING_SCHEMA_LOCATION("DEPOT_MISSING_SCHEMA_LOCATION"),
    MISSING_INDEX_ON_FOREIGN_KEY_CHILD("DEPOT_MISSING_FOREIGN_KEY_CHILD_INDEX"),
    RELATION_QUERY_WITHOUT_TRANSACTION("DEPOT_RELATION_QUERY_WITHOUT_TRANSACTION"),
    DEFAULT_CONSTRUCTOR("DEPOT_DEFAULT_CONSTRUCTOR"),
    // TODO(danysantiago): These warning keys should have 'DEPOT_' prefix.
    MISSING_COPY_ANNOTATIONS("MISSING_COPY_ANNOTATIONS"),
    MISSING_INDEX_ON_JUNCTION("MISSING_INDEX_ON_JUNCTION"),
    JDK_VERSION_HAS_BUG("JDK_VERSION_HAS_BUG"),
    MISMATCHED_GETTER_TYPE("DEPOT_MISMATCHED_GETTER_TYPE"),
    MISMATCHED_SETTER_TYPE("DEPOT_MISMATCHED_SETTER_TYPE"),
    // NOTE there is no constant for this in DepotWarnings since this is a temporary case until
    // expand projection is removed.
    EXPAND_PROJECTION_WITH_REMOVE_UNUSED_COLUMNS("DEPOT_EXPAND_PROJECTION_WITH_UNUSED_COLUMNS");

    companion object {
        val PUBLIC_KEY_MAP = values().associateBy { it.publicKey }
        fun fromPublicKey(publicKey: String): Warning? {
            return PUBLIC_KEY_MAP[publicKey.uppercase(Locale.US)]
        }
    }
}
