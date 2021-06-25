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

package com.zeoflow.depot.solver.prepared.binderprovider

import com.zeoflow.depot.compiler.processing.XType
import com.zeoflow.depot.parser.ParsedQuery
import com.zeoflow.depot.solver.prepared.binder.PreparedQueryResultBinder

/**
 * Interface for for providing the appropriate [PreparedQueryResultBinder] given a query and a
 * method's return type.
 */
interface PreparedQueryResultBinderProvider {
    /**
     * Check whether the [XType] can be handled by the [PreparedQueryResultBinder] provided
     * by this provider.
     */
    fun matches(declared: XType): Boolean

    /**
     * Provides a [PreparedQueryResultBinder]
     */
    fun provide(declared: XType, query: ParsedQuery): PreparedQueryResultBinder
}