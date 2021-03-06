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

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import com.zeoflow.depot.migration.AutoMigrationSpec;
import com.zeoflow.sqlite.db.SupportSQLiteOpenHelper;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

/**
 * Configuration class for a {@link DepotDatabase}.
 */
@SuppressWarnings("WeakerAccess")
public class DatabaseConfiguration {

    /**
     * The factory to use to access the database.
     */
    @NonNull
    public final SupportSQLiteOpenHelper.Factory sqliteOpenHelperFactory;
    /**
     * The context to use while connecting to the database.
     */
    @NonNull
    public final Context context;
    /**
     * The name of the database file or null if it is an in-memory database.
     */
    @Nullable
    public final String name;

    /**
     * Collection of available migrations.
     */
    @NonNull
    public final DepotDatabase.MigrationContainer migrationContainer;

    @Nullable
    public final List<DepotDatabase.Callback> callbacks;

    @Nullable
    public final DepotDatabase.PrepackagedDatabaseCallback prepackagedDatabaseCallback;

    @NonNull
    public final List<Object> typeConverters;

    @NonNull
    public final List<AutoMigrationSpec> autoMigrationSpecs;

    /**
     * Whether Depot should throw an exception for queries run on the main thread.
     */
    public final boolean allowMainThreadQueries;

    /**
     * The journal mode for this database.
     */
    public final DepotDatabase.JournalMode journalMode;

    /**
     * The Executor used to execute asynchronous queries.
     */
    @NonNull
    public final Executor queryExecutor;

    /**
     * The Executor used to execute asynchronous transactions.
     */
    @NonNull
    public final Executor transactionExecutor;

    /**
     * If true, table invalidation in an instance of {@link DepotDatabase} is broadcast and
     * synchronized with other instances of the same {@link DepotDatabase} file, including those
     * in a separate process.
     */
    public final boolean multiInstanceInvalidation;

    /**
     * If true, Depot should crash if a migration is missing.
     */
    public final boolean requireMigration;

    /**
     * If true, Depot should perform a destructive migration when downgrading without an available
     * migration.
     */
    public final boolean allowDestructiveMigrationOnDowngrade;

    /**
     * The collection of schema versions from which migrations aren't required.
     */
    private final Set<Integer> mMigrationNotRequiredFrom;

    /**
     * The assets path to a pre-packaged database to copy from.
     */
    @Nullable
    public final String copyFromAssetPath;

    /**
     * The pre-packaged database file to copy from.
     */
    @Nullable
    public final File copyFromFile;

    /**
     * The callable to get the input stream from which a pre-package database file will be copied
     * from.
     */
    @Nullable
    public final Callable<InputStream> copyFromInputStream;

    /**
     * Creates a database configuration with the given values.
     *
     * @deprecated Use {@link #DatabaseConfiguration(Context, String,
     * SupportSQLiteOpenHelper.Factory, DepotDatabase.MigrationContainer, List, boolean,
     * DepotDatabase.JournalMode, Executor, Executor, boolean, boolean, boolean, Set, String, File,
     * Callable, DepotDatabase.PrepackagedDatabaseCallback, List<Object>, List<AutoMigrationSpec>)}
     *
     * @param context The application context.
     * @param name Name of the database, can be null if it is in memory.
     * @param sqliteOpenHelperFactory The open helper factory to use.
     * @param migrationContainer The migration container for migrations.
     * @param callbacks The list of callbacks for database events.
     * @param allowMainThreadQueries Whether to allow main thread reads/writes or not.
     * @param journalMode The journal mode. This has to be either TRUNCATE or WRITE_AHEAD_LOGGING.
     * @param queryExecutor The Executor used to execute asynchronous queries.
     * @param requireMigration True if Depot should require a valid migration if version changes,
     *                        instead of recreating the tables.
     * @param migrationNotRequiredFrom The collection of schema versions from which migrations
     *                                 aren't required.
     *
     * @hide
     */
    @Deprecated
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public DatabaseConfiguration(@NonNull Context context, @Nullable String name,
            @NonNull SupportSQLiteOpenHelper.Factory sqliteOpenHelperFactory,
            @NonNull DepotDatabase.MigrationContainer migrationContainer,
            @Nullable List<DepotDatabase.Callback> callbacks,
            boolean allowMainThreadQueries,
            DepotDatabase.JournalMode journalMode,
            @NonNull Executor queryExecutor,
            boolean requireMigration,
            @Nullable Set<Integer> migrationNotRequiredFrom) {
        this(context, name, sqliteOpenHelperFactory, migrationContainer, callbacks,
                allowMainThreadQueries, journalMode, queryExecutor, queryExecutor, false,
                requireMigration, false, migrationNotRequiredFrom, null, null, null, null, null,
                null);
    }

    /**
     * Creates a database configuration with the given values.
     *
     * @deprecated Use {@link #DatabaseConfiguration(Context, String,
     * SupportSQLiteOpenHelper.Factory, DepotDatabase.MigrationContainer, List, boolean,
     * DepotDatabase.JournalMode, Executor, Executor, boolean, boolean, boolean, Set, String, File,
     * Callable, DepotDatabase.PrepackagedDatabaseCallback, List<Object>, List<AutoMigrationSpec>)}
     *
     * @param context The application context.
     * @param name Name of the database, can be null if it is in memory.
     * @param sqliteOpenHelperFactory The open helper factory to use.
     * @param migrationContainer The migration container for migrations.
     * @param callbacks The list of callbacks for database events.
     * @param allowMainThreadQueries Whether to allow main thread reads/writes or not.
     * @param journalMode The journal mode. This has to be either TRUNCATE or WRITE_AHEAD_LOGGING.
     * @param queryExecutor The Executor used to execute asynchronous queries.
     * @param transactionExecutor The Executor used to execute asynchronous transactions.
     * @param multiInstanceInvalidation True if Depot should perform multi-instance invalidation.
     * @param requireMigration True if Depot should require a valid migration if version changes,
     * @param allowDestructiveMigrationOnDowngrade True if Depot should recreate tables if no
     *                                             migration is supplied during a downgrade.
     * @param migrationNotRequiredFrom The collection of schema versions from which migrations
     *                                 aren't required.
     *
     * @hide
     */
    @Deprecated
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public DatabaseConfiguration(@NonNull Context context, @Nullable String name,
            @NonNull SupportSQLiteOpenHelper.Factory sqliteOpenHelperFactory,
            @NonNull DepotDatabase.MigrationContainer migrationContainer,
            @Nullable List<DepotDatabase.Callback> callbacks,
            boolean allowMainThreadQueries,
            DepotDatabase.JournalMode journalMode,
            @NonNull Executor queryExecutor,
            @NonNull Executor transactionExecutor,
            boolean multiInstanceInvalidation,
            boolean requireMigration,
            boolean allowDestructiveMigrationOnDowngrade,
            @Nullable Set<Integer> migrationNotRequiredFrom) {
        this(context, name, sqliteOpenHelperFactory, migrationContainer, callbacks,
                allowMainThreadQueries, journalMode, queryExecutor, transactionExecutor,
                multiInstanceInvalidation, requireMigration, allowDestructiveMigrationOnDowngrade,
                migrationNotRequiredFrom, null, null, null, null, null, null);
    }

    /**
     * Creates a database configuration with the given values.
     *
     * @deprecated Use {@link #DatabaseConfiguration(Context, String,
     * SupportSQLiteOpenHelper.Factory, DepotDatabase.MigrationContainer, List, boolean,
     * DepotDatabase.JournalMode, Executor, Executor, boolean, boolean, boolean, Set, String, File,
     * Callable, DepotDatabase.PrepackagedDatabaseCallback, List<Object>, List<AutoMigrationSpec>)}
     *
     * @param context The application context.
     * @param name Name of the database, can be null if it is in memory.
     * @param sqliteOpenHelperFactory The open helper factory to use.
     * @param migrationContainer The migration container for migrations.
     * @param callbacks The list of callbacks for database events.
     * @param allowMainThreadQueries Whether to allow main thread reads/writes or not.
     * @param journalMode The journal mode. This has to be either TRUNCATE or WRITE_AHEAD_LOGGING.
     * @param queryExecutor The Executor used to execute asynchronous queries.
     * @param transactionExecutor The Executor used to execute asynchronous transactions.
     * @param multiInstanceInvalidation True if Depot should perform multi-instance invalidation.
     * @param requireMigration True if Depot should require a valid migration if version changes,
     * @param allowDestructiveMigrationOnDowngrade True if Depot should recreate tables if no
     *                                             migration is supplied during a downgrade.
     * @param migrationNotRequiredFrom The collection of schema versions from which migrations
     *                                 aren't required.
     * @param copyFromAssetPath The assets path to the pre-packaged database.
     * @param copyFromFile The pre-packaged database file.
     *
     * @hide
     */
    @Deprecated
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public DatabaseConfiguration(@NonNull Context context, @Nullable String name,
            @NonNull SupportSQLiteOpenHelper.Factory sqliteOpenHelperFactory,
            @NonNull DepotDatabase.MigrationContainer migrationContainer,
            @Nullable List<DepotDatabase.Callback> callbacks,
            boolean allowMainThreadQueries,
            DepotDatabase.JournalMode journalMode,
            @NonNull Executor queryExecutor,
            @NonNull Executor transactionExecutor,
            boolean multiInstanceInvalidation,
            boolean requireMigration,
            boolean allowDestructiveMigrationOnDowngrade,
            @Nullable Set<Integer> migrationNotRequiredFrom,
            @Nullable String copyFromAssetPath,
            @Nullable File copyFromFile) {
        this(context, name, sqliteOpenHelperFactory, migrationContainer, callbacks,
                allowMainThreadQueries, journalMode, queryExecutor, transactionExecutor,
                multiInstanceInvalidation, requireMigration, allowDestructiveMigrationOnDowngrade,
                migrationNotRequiredFrom, copyFromAssetPath, copyFromFile, null, null, null, null);
    }

    /**
     * Creates a database configuration with the given values.
     *
     * @deprecated Use {@link #DatabaseConfiguration(Context, String,
     * SupportSQLiteOpenHelper.Factory, DepotDatabase.MigrationContainer, List, boolean,
     * DepotDatabase.JournalMode, Executor, Executor, boolean, boolean, boolean, Set, String, File,
     * Callable, DepotDatabase.PrepackagedDatabaseCallback, List<Object>, List<AutoMigrationSpec>)}
     *
     * @param context The application context.
     * @param name Name of the database, can be null if it is in memory.
     * @param sqliteOpenHelperFactory The open helper factory to use.
     * @param migrationContainer The migration container for migrations.
     * @param callbacks The list of callbacks for database events.
     * @param allowMainThreadQueries Whether to allow main thread reads/writes or not.
     * @param journalMode The journal mode. This has to be either TRUNCATE or WRITE_AHEAD_LOGGING.
     * @param queryExecutor The Executor used to execute asynchronous queries.
     * @param transactionExecutor The Executor used to execute asynchronous transactions.
     * @param multiInstanceInvalidation True if Depot should perform multi-instance invalidation.
     * @param requireMigration True if Depot should require a valid migration if version changes,
     * @param allowDestructiveMigrationOnDowngrade True if Depot should recreate tables if no
     *                                             migration is supplied during a downgrade.
     * @param migrationNotRequiredFrom The collection of schema versions from which migrations
     *                                 aren't required.
     * @param copyFromAssetPath The assets path to the pre-packaged database.
     * @param copyFromFile The pre-packaged database file.
     * @param copyFromInputStream The callable to get the input stream from which a
     *                            pre-package database file will be copied from.
     *
     * @hide
     */
    @Deprecated
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public DatabaseConfiguration(@NonNull Context context, @Nullable String name,
            @NonNull SupportSQLiteOpenHelper.Factory sqliteOpenHelperFactory,
            @NonNull DepotDatabase.MigrationContainer migrationContainer,
            @Nullable List<DepotDatabase.Callback> callbacks,
            boolean allowMainThreadQueries,
            @NonNull DepotDatabase.JournalMode journalMode,
            @NonNull Executor queryExecutor,
            @NonNull Executor transactionExecutor,
            boolean multiInstanceInvalidation,
            boolean requireMigration,
            boolean allowDestructiveMigrationOnDowngrade,
            @Nullable Set<Integer> migrationNotRequiredFrom,
            @Nullable String copyFromAssetPath,
            @Nullable File copyFromFile,
            @Nullable Callable<InputStream> copyFromInputStream) {
        this(context, name, sqliteOpenHelperFactory, migrationContainer, callbacks,
                allowMainThreadQueries, journalMode, queryExecutor, transactionExecutor,
                multiInstanceInvalidation, requireMigration, allowDestructiveMigrationOnDowngrade,
                migrationNotRequiredFrom, copyFromAssetPath, copyFromFile, copyFromInputStream,
                null, null, null);
    }

    /**
     * Creates a database configuration with the given values.
     *
     * @deprecated Use {@link #DatabaseConfiguration(Context, String,
     * SupportSQLiteOpenHelper.Factory, DepotDatabase.MigrationContainer, List, boolean,
     * DepotDatabase.JournalMode, Executor, Executor, boolean, boolean, boolean, Set, String, File,
     * Callable, DepotDatabase.PrepackagedDatabaseCallback, List<Object>, List<AutoMigrationSpec>)}
     *
     * @param context The application context.
     * @param name Name of the database, can be null if it is in memory.
     * @param sqliteOpenHelperFactory The open helper factory to use.
     * @param migrationContainer The migration container for migrations.
     * @param callbacks The list of callbacks for database events.
     * @param allowMainThreadQueries Whether to allow main thread reads/writes or not.
     * @param journalMode The journal mode. This has to be either TRUNCATE or WRITE_AHEAD_LOGGING.
     * @param queryExecutor The Executor used to execute asynchronous queries.
     * @param transactionExecutor The Executor used to execute asynchronous transactions.
     * @param multiInstanceInvalidation True if Depot should perform multi-instance invalidation.
     * @param requireMigration True if Depot should require a valid migration if version changes,
     * @param allowDestructiveMigrationOnDowngrade True if Depot should recreate tables if no
     *                                             migration is supplied during a downgrade.
     * @param migrationNotRequiredFrom The collection of schema versions from which migrations
     *                                 aren't required.
     * @param copyFromAssetPath The assets path to the pre-packaged database.
     * @param copyFromFile The pre-packaged database file.
     * @param copyFromInputStream The callable to get the input stream from which a
     *                            pre-package database file will be copied from.
     * @param prepackagedDatabaseCallback The pre-packaged callback.
     *
     * @hide
     */
    @Deprecated
    @SuppressLint("LambdaLast")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public DatabaseConfiguration(@NonNull Context context, @Nullable String name,
            @NonNull SupportSQLiteOpenHelper.Factory sqliteOpenHelperFactory,
            @NonNull DepotDatabase.MigrationContainer migrationContainer,
            @Nullable List<DepotDatabase.Callback> callbacks,
            boolean allowMainThreadQueries,
            @NonNull DepotDatabase.JournalMode journalMode,
            @NonNull Executor queryExecutor,
            @NonNull Executor transactionExecutor,
            boolean multiInstanceInvalidation,
            boolean requireMigration,
            boolean allowDestructiveMigrationOnDowngrade,
            @Nullable Set<Integer> migrationNotRequiredFrom,
            @Nullable String copyFromAssetPath,
            @Nullable File copyFromFile,
            @Nullable Callable<InputStream> copyFromInputStream,
            @Nullable DepotDatabase.PrepackagedDatabaseCallback prepackagedDatabaseCallback) {
        this(context, name, sqliteOpenHelperFactory, migrationContainer, callbacks,
                allowMainThreadQueries, journalMode, queryExecutor, transactionExecutor,
                multiInstanceInvalidation, requireMigration, allowDestructiveMigrationOnDowngrade,
                migrationNotRequiredFrom, copyFromAssetPath, copyFromFile, copyFromInputStream,
                prepackagedDatabaseCallback, null, null);
    }

    /**
     * Creates a database configuration with the given values.
     *
     * @deprecated Use {@link #DatabaseConfiguration(Context, String,
     * SupportSQLiteOpenHelper.Factory, DepotDatabase.MigrationContainer, List, boolean,
     * DepotDatabase.JournalMode, Executor, Executor, boolean, boolean, boolean, Set, String, File,
     * Callable, DepotDatabase.PrepackagedDatabaseCallback, List<Object>, List<AutoMigrationSpec>)}
     *
     * @param context The application context.
     * @param name Name of the database, can be null if it is in memory.
     * @param sqliteOpenHelperFactory The open helper factory to use.
     * @param migrationContainer The migration container for migrations.
     * @param callbacks The list of callbacks for database events.
     * @param allowMainThreadQueries Whether to allow main thread reads/writes or not.
     * @param journalMode The journal mode. This has to be either TRUNCATE or WRITE_AHEAD_LOGGING.
     * @param queryExecutor The Executor used to execute asynchronous queries.
     * @param transactionExecutor The Executor used to execute asynchronous transactions.
     * @param multiInstanceInvalidation True if Depot should perform multi-instance invalidation.
     * @param requireMigration True if Depot should require a valid migration if version changes,
     * @param allowDestructiveMigrationOnDowngrade True if Depot should recreate tables if no
     *                                             migration is supplied during a downgrade.
     * @param migrationNotRequiredFrom The collection of schema versions from which migrations
     *                                 aren't required.
     * @param copyFromAssetPath The assets path to the pre-packaged database.
     * @param copyFromFile The pre-packaged database file.
     * @param copyFromInputStream The callable to get the input stream from which a
     *                            pre-package database file will be copied from.
     * @param prepackagedDatabaseCallback The pre-packaged callback.
     * @param typeConverters The type converters.
     *
     * @hide
     */
    @Deprecated
    @SuppressLint("LambdaLast")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public DatabaseConfiguration(@NonNull Context context, @Nullable String name,
            @NonNull SupportSQLiteOpenHelper.Factory sqliteOpenHelperFactory,
            @NonNull DepotDatabase.MigrationContainer migrationContainer,
            @Nullable List<DepotDatabase.Callback> callbacks,
            boolean allowMainThreadQueries,
            @NonNull DepotDatabase.JournalMode journalMode,
            @NonNull Executor queryExecutor,
            @NonNull Executor transactionExecutor,
            boolean multiInstanceInvalidation,
            boolean requireMigration,
            boolean allowDestructiveMigrationOnDowngrade,
            @Nullable Set<Integer> migrationNotRequiredFrom,
            @Nullable String copyFromAssetPath,
            @Nullable File copyFromFile,
            @Nullable Callable<InputStream> copyFromInputStream,
            @Nullable DepotDatabase.PrepackagedDatabaseCallback prepackagedDatabaseCallback,
            @Nullable List<Object> typeConverters) {
        this(context, name, sqliteOpenHelperFactory, migrationContainer, callbacks,
                allowMainThreadQueries, journalMode, queryExecutor, transactionExecutor,
                multiInstanceInvalidation, requireMigration, allowDestructiveMigrationOnDowngrade,
                migrationNotRequiredFrom, copyFromAssetPath, copyFromFile, copyFromInputStream,
                prepackagedDatabaseCallback, typeConverters, null);
    }

    /**
     * Creates a database configuration with the given values.
     *
     * @param context The application context.
     * @param name Name of the database, can be null if it is in memory.
     * @param sqliteOpenHelperFactory The open helper factory to use.
     * @param migrationContainer The migration container for migrations.
     * @param callbacks The list of callbacks for database events.
     * @param allowMainThreadQueries Whether to allow main thread reads/writes or not.
     * @param journalMode The journal mode. This has to be either TRUNCATE or WRITE_AHEAD_LOGGING.
     * @param queryExecutor The Executor used to execute asynchronous queries.
     * @param transactionExecutor The Executor used to execute asynchronous transactions.
     * @param multiInstanceInvalidation True if Depot should perform multi-instance invalidation.
     * @param requireMigration True if Depot should require a valid migration if version changes,
     * @param allowDestructiveMigrationOnDowngrade True if Depot should recreate tables if no
     *                                             migration is supplied during a downgrade.
     * @param migrationNotRequiredFrom The collection of schema versions from which migrations
     *                                 aren't required.
     * @param copyFromAssetPath The assets path to the pre-packaged database.
     * @param copyFromFile The pre-packaged database file.
     * @param copyFromInputStream The callable to get the input stream from which a
     *                            pre-package database file will be copied from.
     * @param prepackagedDatabaseCallback The pre-packaged callback.
     * @param typeConverters The type converters.
     * @param autoMigrationSpecs The auto migration specs.
     *
     * @hide
     */
    @SuppressLint("LambdaLast")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public DatabaseConfiguration(@NonNull Context context, @Nullable String name,
            @NonNull SupportSQLiteOpenHelper.Factory sqliteOpenHelperFactory,
            @NonNull DepotDatabase.MigrationContainer migrationContainer,
            @Nullable List<DepotDatabase.Callback> callbacks,
            boolean allowMainThreadQueries,
            @NonNull DepotDatabase.JournalMode journalMode,
            @NonNull Executor queryExecutor,
            @NonNull Executor transactionExecutor,
            boolean multiInstanceInvalidation,
            boolean requireMigration,
            boolean allowDestructiveMigrationOnDowngrade,
            @Nullable Set<Integer> migrationNotRequiredFrom,
            @Nullable String copyFromAssetPath,
            @Nullable File copyFromFile,
            @Nullable Callable<InputStream> copyFromInputStream,
            @Nullable DepotDatabase.PrepackagedDatabaseCallback prepackagedDatabaseCallback,
            @Nullable List<Object> typeConverters,
            @Nullable List<AutoMigrationSpec> autoMigrationSpecs) {
        this.sqliteOpenHelperFactory = sqliteOpenHelperFactory;
        this.context = context;
        this.name = name;
        this.migrationContainer = migrationContainer;
        this.callbacks = callbacks;
        this.allowMainThreadQueries = allowMainThreadQueries;
        this.journalMode = journalMode;
        this.queryExecutor = queryExecutor;
        this.transactionExecutor = transactionExecutor;
        this.multiInstanceInvalidation = multiInstanceInvalidation;
        this.requireMigration = requireMigration;
        this.allowDestructiveMigrationOnDowngrade = allowDestructiveMigrationOnDowngrade;
        this.mMigrationNotRequiredFrom = migrationNotRequiredFrom;
        this.copyFromAssetPath = copyFromAssetPath;
        this.copyFromFile = copyFromFile;
        this.copyFromInputStream = copyFromInputStream;
        this.prepackagedDatabaseCallback = prepackagedDatabaseCallback;
        this.typeConverters = typeConverters == null ? Collections.emptyList() : typeConverters;
        this.autoMigrationSpecs = autoMigrationSpecs == null
                ? Collections.emptyList() : autoMigrationSpecs;
    }

    /**
     * Returns whether a migration is required from the specified version.
     *
     * @param version  The schema version.
     * @return True if a valid migration is required, false otherwise.
     *
     * @deprecated Use {@link #isMigrationRequired(int, int)} which takes
     * {@link #allowDestructiveMigrationOnDowngrade} into account.
     */
    @Deprecated
    public boolean isMigrationRequiredFrom(int version) {
        return isMigrationRequired(version, version + 1);
    }

    /**
     * Returns whether a migration is required between two versions.
     *
     * @param fromVersion The old schema version.
     * @param toVersion   The new schema version.
     * @return True if a valid migration is required, false otherwise.
     */
    public boolean isMigrationRequired(int fromVersion, int toVersion) {
        // Migrations are not required if its a downgrade AND destructive migration during downgrade
        // has been allowed.
        final boolean isDowngrade = fromVersion > toVersion;
        if (isDowngrade && allowDestructiveMigrationOnDowngrade) {
            return false;
        }

        // Migrations are required between the two versions if we generally require migrations
        // AND EITHER there are no exceptions OR the supplied fromVersion is not one of the
        // exceptions.
        return requireMigration
                && (mMigrationNotRequiredFrom == null
                || !mMigrationNotRequiredFrom.contains(fromVersion));
    }
}
