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

package com.zeoflow.depot.solver.query.result

import com.zeoflow.depot.compiler.processing.XType
import com.zeoflow.depot.solver.CodeGenScope

/**
 * Converts a row of a cursor result into an Entity or a primitive.
 * <p>
 * An instance of this is created for each usage so that it can keep local variables.
 */
abstract class RowAdapter(val out: XType) {
    /**
     * Called when cursor variable is ready, good place to put initialization code.
     */
    open fun onCursorReady(cursorVarName: String, scope: CodeGenScope) {}

    /**
     * Called to convert a single row.
     */
    abstract fun convert(outVarName: String, cursorVarName: String, scope: CodeGenScope)

    /**
     * Called when the cursor is finished. It is important to return null if no operation is
     * necessary so that caller can understand that we can do lazy loading.
     */
    open fun onCursorFinished(): ((scope: CodeGenScope) -> Unit)? = null
}
