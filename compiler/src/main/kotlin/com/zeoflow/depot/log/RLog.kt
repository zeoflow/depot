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

@file:Suppress("unused")

package com.zeoflow.depot.log

import com.zeoflow.depot.compiler.processing.XElement
import com.zeoflow.depot.compiler.processing.XMessager
import com.zeoflow.depot.processor.Context
import com.zeoflow.depot.vo.Warning
import javax.tools.Diagnostic
import javax.tools.Diagnostic.Kind.ERROR
import javax.tools.Diagnostic.Kind.NOTE
import javax.tools.Diagnostic.Kind.WARNING

class RLog(
    val messager: XMessager,
    val suppressedWarnings: Set<Warning>,
    val defaultElement: XElement?
) {
    private fun String.safeFormat(vararg args: Any): String {
        try {
            return format(args)
        } catch (ex: Throwable) {
            // the input string might be from random source in which case we rather print the
            // msg as is instead of crashing while reporting an error.
            return this
        }
    }

    fun d(element: XElement, msg: String, vararg args: Any) {
        messager.printMessage(NOTE, msg.safeFormat(args), element)
    }

    fun d(msg: String, vararg args: Any) {
        messager.printMessage(NOTE, msg.safeFormat(args))
    }

    fun e(element: XElement, msg: String, vararg args: Any) {
        messager.printMessage(ERROR, msg.safeFormat(args), element)
    }

    fun e(msg: String, vararg args: Any) {
        messager.printMessage(ERROR, msg.safeFormat(args), defaultElement)
    }

    fun w(warning: Warning, element: XElement? = null, msg: String, vararg args: Any) {
        if (suppressedWarnings.contains(warning)) {
            return
        }
        messager.printMessage(
            WARNING, msg.safeFormat(args),
            element ?: defaultElement
        )
    }

    fun w(warning: Warning, msg: String, vararg args: Any) {
        if (suppressedWarnings.contains(warning)) {
            return
        }
        messager.printMessage(WARNING, msg.safeFormat(args), defaultElement)
    }

    class CollectingMessager : XMessager() {
        private val messages = mutableMapOf<Diagnostic.Kind, MutableList<Pair<String, XElement?>>>()
        override fun onPrintMessage(kind: Diagnostic.Kind, msg: String, element: XElement?) {
            messages.getOrPut(
                kind,
                {
                    arrayListOf()
                }
            ).add(Pair(msg, element))
        }

        fun hasErrors() = messages.containsKey(ERROR)

        fun writeTo(context: Context) {
            val printMessage = context.logger.messager::printMessage
            messages.forEach { pair ->
                val kind = pair.key
                pair.value.forEach { (msg, element) ->
                    printMessage(kind, msg, element)
                }
            }
        }
    }
}
