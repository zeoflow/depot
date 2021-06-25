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

package com.zeoflow.depot.compiler.processing.javac

import com.zeoflow.depot.compiler.processing.XFiler
import com.squareup.javapoet.JavaFile
import com.squareup.kotlinpoet.FileSpec
import javax.annotation.processing.ProcessingEnvironment

internal class JavacFiler(val processingEnv: ProcessingEnvironment) : XFiler {

    // "mode" is ignored in javac, and only applicable in KSP
    override fun write(javaFile: JavaFile, mode: XFiler.Mode) {
        javaFile.writeTo(processingEnv.filer)
    }

    override fun write(fileSpec: FileSpec, mode: XFiler.Mode) {
        require(processingEnv.options.containsKey("kapt.kotlin.generated")) {
            val filePath = fileSpec.packageName.replace('.', '/')
            "Could not generate kotlin file $filePath/${fileSpec.name}.kt. The " +
                "annotation processing environment is not set to generate Kotlin files."
        }
        fileSpec.writeTo(processingEnv.filer)
    }
}
