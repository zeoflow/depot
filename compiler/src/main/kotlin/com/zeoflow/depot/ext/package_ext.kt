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

package com.zeoflow.depot.ext

import com.zeoflow.depot.DepotProcessor

/**
 * Map of dejetified packages names. Useful for letting Depot know which packages names to use
 * when generating code in a dejetified environment. To use this map add a resource file named
 * 'dejetifier.config' containing one key-value pair per line separated by '=' where the key is
 * the androidx package name to dejetify and the value is the dejetified package name.
 *
 * Example of a typical config:
 * ```
 * # Depot dejetifier packages for JavaPoet class names.
 * com.zeoflow.sqlite = android.arch.persistence
 * com.zeoflow.depot = android.arch.persistence.depot
 * androidx.paging = android.arch.paging
 * androidx.lifecycle = android.arch.lifecycle
 * androidx.collection = com.android.support
 * ```
 */
private val PACKAGE_NAME_OVERRIDES: Map<String, String> by lazy {
    DepotProcessor::class.java.classLoader.getResourceAsStream("dejetifier.config")?.reader()?.use {
        try {
            it.readLines()
                .filterNot { it.startsWith('#') }
                .associate { it.split('=').let { split -> split[0].trim() to split[1].trim() } }
        } catch (ex: Exception) {
            throw RuntimeException("Malformed dejetifier.config file.", ex)
        }
    } ?: emptyMap()
}

val SQLITE_PACKAGE = getOrDefault("com.zeoflow.sqlite")
val DEPOT_PACKAGE = getOrDefault("com.zeoflow.depot")
// TODO implement paging
val PAGING_PACKAGE = getOrDefault("androidx.paging")
val LIFECYCLE_PACKAGE = getOrDefault("androidx.lifecycle")
val COLLECTION_PACKAGE = getOrDefault("androidx.collection")

private fun getOrDefault(key: String) = PACKAGE_NAME_OVERRIDES.getOrDefault(key, key)
