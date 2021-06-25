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

/**
 * Depot is a Database Object Mapping library that makes it easy to access database on Android
 * applications.
 * <p>
 * Rather than hiding the details of SQLite, Depot tries to embrace them by providing convenient APIs
 * to query the database and also verify such queries at compile time. This allows you to access
 * the full power of SQLite while having the type safety provided by Java SQL query builders.
 * <p>
 * There are 3 major components in Depot.
 * <ul>
 *     <li>{@link com.zeoflow.depot.Database Database}: This annotation marks a class as a database.
 *     It should be an abstract class that extends {@link com.zeoflow.depot.DepotDatabase DepotDatabase}.
 *     At runtime, you can acquire an instance of it via {@link com.zeoflow.depot.Depot#databaseBuilder(
 *     android.content.Context,java.lang.Class, java.lang.String) Depot.databaseBuilder} or
 *     {@link com.zeoflow.depot.Depot#inMemoryDatabaseBuilder(android.content.Context, java.lang.Class)
 *     Depot.inMemoryDatabaseBuilder}.
 *     <p>
 *     The database class defines the list of entities and data access objects in the database.
 *     It is also the main access point for the underlying connection.
 *     </li>
 *     <li>{@link com.zeoflow.depot.Entity Entity}: This annotation marks a class as a database row.
 *     For each {@link com.zeoflow.depot.Entity Entity}, a database table is created to hold the items.
 *     The Entity class must be referenced in the
 *     {@link com.zeoflow.depot.Database#entities() Database#entities} array. Each field of the Entity
 *     (and its super class) is persisted in the database unless it is denoted otherwise
 *     (see {@link com.zeoflow.depot.Entity Entity} docs for details).
 *     </li>
 *     <li>{@link com.zeoflow.depot.Dao Dao}: This annotation marks a class or interface as a
 *     Data Access Object. Data access objects are the main components of Depot that are
 *     responsible for defining the methods that access the database. The class that is annotated
 *     with {@link com.zeoflow.depot.Database Database} must have an abstract method that has 0
 *     arguments and returns the class that is annotated with Dao. While generating the code at
 *     compile time, Depot will generate an implementation of this class.
 *     <p>
 *     Using Dao classes for database access rather than query builders or direct queries allows you
 *     to keep a separation between different components and easily mock the database access while
 *     testing your application.
 *     </li>
 * </ul>
 * Below is a sample of a simple database.
 * <pre>
 * // File: Song.java
 * {@literal @}Entity
 * public class Song {
 *   {@literal @}PrimaryKey
 *   private int id;
 *   private String name;
 *   {@literal @}ColumnInfo(name = "release_year")
 *   private int releaseYear;
 *   // getters and setters are ignored for brevity but they are required for Depot to work.
 * }
 * // File: SongDao.java
 * {@literal @}Dao
 * public interface SongDao {
 *   {@literal @}Query("SELECT * FROM song")
 *   List&lt;Song&gt; loadAll();
 *   {@literal @}Query("SELECT * FROM song WHERE id IN (:songIds)")
 *   List&lt;Song&gt; loadAllBySongId(int... songIds);
 *   {@literal @}Query("SELECT * FROM song WHERE name LIKE :name AND release_year = :year LIMIT 1")
 *   Song loadOneByNameAndReleaseYear(String first, int year);
 *   {@literal @}Insert
 *   void insertAll(Song... songs);
 *   {@literal @}Delete
 *   void delete(Song song);
 * }
 * // File: MusicDatabase.java
 * {@literal @}Database(entities = {Song.class})
 * public abstract class MusicDatabase extends DepotDatabase {
 *   public abstract SongDao songDao();
 * }
 * </pre>
 * You can create an instance of {@code MusicDatabase} as follows:
 * <pre>
 * MusicDatabase db = Depot
 *     .databaseBuilder(getApplicationContext(), MusicDatabase.class, "database-name")
 *     .build();
 * </pre>
 * Since Depot verifies your queries at compile time, it also detects information about which tables
 * are accessed by the query or what columns are present in the response.
 * <p>
 * You can observe a particular table for changes using the
 * {@link com.zeoflow.depot.InvalidationTracker InvalidationTracker} class which you can acquire via
 * {@link com.zeoflow.depot.DepotDatabase#getInvalidationTracker()
 * DepotDatabase.getInvalidationTracker}.
 * <p>
 * For convenience, Depot allows you to return {@link androidx.lifecycle.LiveData LiveData} from
 * {@link com.zeoflow.depot.Query Query} methods. It will automatically observe the related tables as
 * long as the {@code LiveData} has active observers.
 * <pre>
 * // This live data will automatically dispatch changes as the database changes.
 * {@literal @}Query("SELECT * FROM song ORDER BY name LIMIT 5")
 * LiveData&lt;Song&gt; loadFirstFiveSongs();
 * </pre>
 * <p>
 * You can also return arbitrary data objects from your query results as long as the fields in the
 * object match the list of columns in the query response. This makes it very easy to write
 * applications that drive the UI from persistent storage.
 * <pre>
 * class IdAndSongHeader {
 *   int id;
 *   {@literal @}ColumnInfo(name = "header")
 *   String header;
 * }
 * // DAO
 * {@literal @}Query("SELECT id, name || '-' || release_year AS header FROM song")
 * public IdAndSongHeader[] loadSongHeaders();
 * </pre>
 * If there is a mismatch between the query result and the POJO, Depot will print a warning during
 * compilation.
 * <p>
 * Please see the documentation of individual classes for details.
 */
package com.zeoflow.depot;
