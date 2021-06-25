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

import com.zeoflow.depot.compiler.processing.XMethodElement
import com.zeoflow.depot.compiler.processing.XType
import com.zeoflow.depot.solver.shortcut.binder.InsertMethodBinder

data class InsertionMethod(
    val element: XMethodElement,
    val name: String,
    @com.zeoflow.depot.OnConflictStrategy val onConflict: Int,
    val entities: Map<String, ShortcutEntity>,
    val returnType: XType,
    val parameters: List<ShortcutQueryParameter>,
    val methodBinder: InsertMethodBinder
)