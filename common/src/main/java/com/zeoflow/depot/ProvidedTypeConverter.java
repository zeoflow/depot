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

package com.zeoflow.depot;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a type converter that will be provided to Depot at runtime.
 * If Depot uses the annotated type converter class, it will verify that it is provided in the
 * builder and if not, will throw an exception.
 * An instance of a class annotated with this annotation has to be provided to Depot using
 * {@code Depot.databaseBuilder.addTypeConverter(Object)}
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface ProvidedTypeConverter {
}
