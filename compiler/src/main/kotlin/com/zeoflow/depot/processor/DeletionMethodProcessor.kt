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

import com.zeoflow.depot.compiler.processing.XMethodElement
import com.zeoflow.depot.compiler.processing.XType
import com.zeoflow.depot.vo.DeletionMethod

class DeletionMethodProcessor(
    baseContext: Context,
    val containing: XType,
    val executableElement: XMethodElement
) {
    val context = baseContext.fork(executableElement)

    fun process(): DeletionMethod {
        val delegate = ShortcutMethodProcessor(context, containing, executableElement)
        val annotation = delegate
            .extractAnnotation(com.zeoflow.depot.Delete::class, ProcessorErrors.MISSING_DELETE_ANNOTATION)

        val returnType = delegate.extractReturnType()

        val methodBinder = delegate.findDeleteOrUpdateMethodBinder(returnType)

        context.checker.check(
            methodBinder.adapter != null,
            executableElement,
            ProcessorErrors.CANNOT_FIND_DELETE_RESULT_ADAPTER
        )

        val (entities, params) = delegate.extractParams(
            targetEntityType = annotation?.getAsType("entity"),
            missingParamError = ProcessorErrors.DELETION_MISSING_PARAMS,
            onValidatePartialEntity = { _, _ -> }
        )

        return DeletionMethod(
            element = delegate.executableElement,
            name = delegate.executableElement.name,
            entities = entities,
            parameters = params,
            methodBinder = methodBinder
        )
    }
}
