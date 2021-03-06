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

package com.zeoflow.depot

import com.zeoflow.depot.compiler.processing.XProcessingEnv
import com.zeoflow.depot.compiler.processing.XProcessingStep.Companion.executeInKsp
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated

/**
 * Entry point for processing using KSP.
 */
class DepotKspProcessor(
    private val options: Map<String, String>,
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val processingEnv = XProcessingEnv.create(
            options,
            resolver,
            codeGenerator,
            logger
        )

        return DatabaseProcessingStep().executeInKsp(
            processingEnv
        )
    }

    class Provider : SymbolProcessorProvider {
        override fun create(
            options: Map<String, String>,
            kotlinVersion: KotlinVersion,
            codeGenerator: CodeGenerator,
            logger: KSPLogger
        ): SymbolProcessor {
            return DepotKspProcessor(
                options = options,
                codeGenerator = codeGenerator,
                logger = logger
            )
        }
    }
}