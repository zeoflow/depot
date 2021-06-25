/*
 * Copyright (C) 2017 depot
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zeoflow.depot.dispatcher;

import androidx.annotation.RestrictTo;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a class as an SharedPreference data. This class will have a mapping SharedPreference with
 * Upper camel case.
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface DepotDispatcher
{
    /**
     * The list of entities included in the database. Each entity turns into a table in the
     * database.
     *
     * @return The list of entities in the database.
     */
    Class<?>[] entities() default {};

    /**
     * The list of dbBeans included in the database. Each entity turns into a table in the
     * database.
     *
     * @return The list of dbBeans in the database.
     */
    Class<?>[] dbBeans() default {};

    /**
     * The list of database views included in the database. Each class turns into a view in the
     * database.
     *
     * @return The list of database views.
     */
    Class<?>[] views() default {};

    /**
     * The database version.
     *
     * @return The database version.
     */
    int version();

    /**
     * The number of threads.
     *
     * @return The number of threads.
     */
    int numberThreads() default 4;

    /**
     * You can set the creator processor argument ({@code com.zeoflow.depot.schemaLocation}) to tell Depot to
     * export the database schema into a folder. Even though it is not mandatory, it is a good
     * practice to have version history of your schema in your codebase and you should commit the
     * schema files into your version control system (but don't ship them with your app!).
     * <p>
     * When {@code com.zeoflow.depot.schemaLocation} is set, Depot will check this variable and if it is set to
     * {@code true}, the database schema will be exported into the given folder.
     * <p>
     * {@code exportSchema} is {@code true} by default but you can disable it for databases when
     * you don't want to keep history of versions (like an in-memory only database).
     *
     * @return Whether the schema should be exported to the given folder when the
     * {@code com.zeoflow.depot.schemaLocation} argument is set. Defaults to {@code true}.
     */
    boolean exportSchema() default true;


    /**
     * List of AutoMigrations that can be performed on this Database.
     *
     * @return List of AutoMigrations.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    Class<?>[] autoMigrations() default {};
}
