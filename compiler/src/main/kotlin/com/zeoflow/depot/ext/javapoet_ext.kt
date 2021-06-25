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

package com.zeoflow.depot.ext

import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import java.util.concurrent.Callable
import javax.lang.model.element.Modifier
import kotlin.reflect.KClass

val L = "\$L"
val T = "\$T"
val N = "\$N"
val S = "\$S"
val W = "\$W"

val KClass<*>.typeName: ClassName
    get() = ClassName.get(this.java)
val KClass<*>.arrayTypeName: ArrayTypeName
    get() = ArrayTypeName.of(typeName)

object SupportDbTypeNames {
    val DB: ClassName = ClassName.get("$SQLITE_PACKAGE.db", "SupportSQLiteDatabase")
    val SQLITE_STMT: ClassName =
        ClassName.get("$SQLITE_PACKAGE.db", "SupportSQLiteStatement")
    val SQLITE_OPEN_HELPER: ClassName =
        ClassName.get("$SQLITE_PACKAGE.db", "SupportSQLiteOpenHelper")
    val SQLITE_OPEN_HELPER_CALLBACK: ClassName =
        ClassName.get("$SQLITE_PACKAGE.db", "SupportSQLiteOpenHelper.Callback")
    val SQLITE_OPEN_HELPER_CONFIG: ClassName =
        ClassName.get("$SQLITE_PACKAGE.db", "SupportSQLiteOpenHelper.Configuration")
    val QUERY: ClassName =
        ClassName.get("$SQLITE_PACKAGE.db", "SupportSQLiteQuery")
}

object DepotTypeNames {
    val STRING_UTIL: ClassName = ClassName.get("$DEPOT_PACKAGE.util", "StringUtil")
    val DEPOT_DB: ClassName = ClassName.get(DEPOT_PACKAGE, "DepotDatabase")
    val DEPOT_DB_KT: ClassName = ClassName.get(DEPOT_PACKAGE, "DepotDatabaseKt")
    val DEPOT_DB_CONFIG: ClassName = ClassName.get(DEPOT_PACKAGE, "DatabaseConfiguration")
    val INSERTION_ADAPTER: ClassName =
        ClassName.get(DEPOT_PACKAGE, "EntityInsertionAdapter")
    val DELETE_OR_UPDATE_ADAPTER: ClassName =
        ClassName.get(DEPOT_PACKAGE, "EntityDeletionOrUpdateAdapter")
    val SHARED_SQLITE_STMT: ClassName =
        ClassName.get(DEPOT_PACKAGE, "SharedSQLiteStatement")
    val INVALIDATION_TRACKER: ClassName =
        ClassName.get(DEPOT_PACKAGE, "InvalidationTracker")
    val INVALIDATION_OBSERVER: ClassName =
        ClassName.get("$DEPOT_PACKAGE.InvalidationTracker", "Observer")
    val DEPOT_SQL_QUERY: ClassName =
        ClassName.get(DEPOT_PACKAGE, "DepotSQLiteQuery")
    val OPEN_HELPER: ClassName =
        ClassName.get(DEPOT_PACKAGE, "DepotOpenHelper")
    val OPEN_HELPER_DELEGATE: ClassName =
        ClassName.get(DEPOT_PACKAGE, "DepotOpenHelper.Delegate")
    val OPEN_HELPER_VALIDATION_RESULT: ClassName =
        ClassName.get(DEPOT_PACKAGE, "DepotOpenHelper.ValidationResult")
    val TABLE_INFO: ClassName =
        ClassName.get("$DEPOT_PACKAGE.util", "TableInfo")
    val TABLE_INFO_COLUMN: ClassName =
        ClassName.get("$DEPOT_PACKAGE.util", "TableInfo.Column")
    val TABLE_INFO_FOREIGN_KEY: ClassName =
        ClassName.get("$DEPOT_PACKAGE.util", "TableInfo.ForeignKey")
    val TABLE_INFO_INDEX: ClassName =
        ClassName.get("$DEPOT_PACKAGE.util", "TableInfo.Index")
    val FTS_TABLE_INFO: ClassName =
        ClassName.get("$DEPOT_PACKAGE.util", "FtsTableInfo")
    val VIEW_INFO: ClassName =
        ClassName.get("$DEPOT_PACKAGE.util", "ViewInfo")
    val LIMIT_OFFSET_DATA_SOURCE: ClassName =
        ClassName.get("$DEPOT_PACKAGE.paging", "LimitOffsetDataSource")
    val DB_UTIL: ClassName =
        ClassName.get("$DEPOT_PACKAGE.util", "DBUtil")
    val CURSOR_UTIL: ClassName =
        ClassName.get("$DEPOT_PACKAGE.util", "CursorUtil")
    val MIGRATION: ClassName = ClassName.get("$DEPOT_PACKAGE.migration", "Migration")
    val AUTO_MIGRATION_SPEC: ClassName = ClassName.get(
        "$DEPOT_PACKAGE.migration",
        "AutoMigrationSpec"
    )
}

object PagingTypeNames {
    val DATA_SOURCE: ClassName =
        ClassName.get(PAGING_PACKAGE, "DataSource")
    val POSITIONAL_DATA_SOURCE: ClassName =
        ClassName.get(PAGING_PACKAGE, "PositionalDataSource")
    val DATA_SOURCE_FACTORY: ClassName =
        ClassName.get(PAGING_PACKAGE, "DataSource", "Factory")
    val PAGING_SOURCE: ClassName =
        ClassName.get(PAGING_PACKAGE, "PagingSource")
}

object LifecyclesTypeNames {
    val LIVE_DATA: ClassName = ClassName.get(LIFECYCLE_PACKAGE, "LiveData")
    val COMPUTABLE_LIVE_DATA: ClassName = ClassName.get(
        LIFECYCLE_PACKAGE,
        "ComputableLiveData"
    )
}

object AndroidTypeNames {
    val CURSOR: ClassName = ClassName.get("android.database", "Cursor")
    val BUILD: ClassName = ClassName.get("android.os", "Build")
    val CANCELLATION_SIGNAL: ClassName = ClassName.get("android.os", "CancellationSignal")
}

object CollectionTypeNames {
    val ARRAY_MAP: ClassName = ClassName.get(COLLECTION_PACKAGE, "ArrayMap")
    val LONG_SPARSE_ARRAY: ClassName = ClassName.get(COLLECTION_PACKAGE, "LongSparseArray")
}

object CommonTypeNames {
    val ARRAYS = ClassName.get("java.util", "Arrays")
    val LIST = ClassName.get("java.util", "List")
    val MAP = ClassName.get("java.util", "Map")
    val SET = ClassName.get("java.util", "Set")
    val STRING = ClassName.get("java.lang", "String")
    val INTEGER = ClassName.get("java.lang", "Integer")
    val OPTIONAL = ClassName.get("java.util", "Optional")
    val ILLEGAL_ARG_EXCEPTION = ClassName.get(
        "java.lang", "IllegalArgumentException"
    )
}

object GuavaBaseTypeNames {
    val OPTIONAL = ClassName.get("com.google.common.base", "Optional")
}

object GuavaUtilConcurrentTypeNames {
    val LISTENABLE_FUTURE = ClassName.get("com.google.common.util.concurrent", "ListenableFuture")
}

object RxJava2TypeNames {
    val FLOWABLE = ClassName.get("io.reactivex", "Flowable")
    val OBSERVABLE = ClassName.get("io.reactivex", "Observable")
    val MAYBE = ClassName.get("io.reactivex", "Maybe")
    val SINGLE = ClassName.get("io.reactivex", "Single")
    val COMPLETABLE = ClassName.get("io.reactivex", "Completable")
}

object RxJava3TypeNames {
    val FLOWABLE = ClassName.get("io.reactivex.rxjava3.core", "Flowable")
    val OBSERVABLE = ClassName.get("io.reactivex.rxjava3.core", "Observable")
    val MAYBE = ClassName.get("io.reactivex.rxjava3.core", "Maybe")
    val SINGLE = ClassName.get("io.reactivex.rxjava3.core", "Single")
    val COMPLETABLE = ClassName.get("io.reactivex.rxjava3.core", "Completable")
}

object ReactiveStreamsTypeNames {
    val PUBLISHER = ClassName.get("org.reactivestreams", "Publisher")
}

object DepotGuavaTypeNames {
    val GUAVA_DEPOT = ClassName.get("$DEPOT_PACKAGE.guava", "GuavaDepot")
}

object DepotRxJava2TypeNames {
    val RX_DEPOT = ClassName.get(DEPOT_PACKAGE, "RxDepot")
    val RX_DEPOT_CREATE_FLOWABLE = "createFlowable"
    val RX_DEPOT_CREATE_OBSERVABLE = "createObservable"
    val RX_EMPTY_RESULT_SET_EXCEPTION = ClassName.get(DEPOT_PACKAGE, "EmptyResultSetException")
}

object DepotRxJava3TypeNames {
    val RX_DEPOT = ClassName.get("$DEPOT_PACKAGE.rxjava3", "RxDepot")
    val RX_DEPOT_CREATE_FLOWABLE = "createFlowable"
    val RX_DEPOT_CREATE_OBSERVABLE = "createObservable"
    val RX_EMPTY_RESULT_SET_EXCEPTION =
        ClassName.get("$DEPOT_PACKAGE.rxjava3", "EmptyResultSetException")
}

object DepotCoroutinesTypeNames {
    val COROUTINES_DEPOT = ClassName.get(DEPOT_PACKAGE, "CoroutinesDepot")
}

object KotlinTypeNames {
    val UNIT = ClassName.get("kotlin", "Unit")
    val CONTINUATION = ClassName.get("kotlin.coroutines", "Continuation")
    val COROUTINE_SCOPE = ClassName.get("kotlinx.coroutines", "CoroutineScope")
    val CHANNEL = ClassName.get("kotlinx.coroutines.channels", "Channel")
    val RECEIVE_CHANNEL = ClassName.get("kotlinx.coroutines.channels", "ReceiveChannel")
    val SEND_CHANNEL = ClassName.get("kotlinx.coroutines.channels", "SendChannel")
    val FLOW = ClassName.get("kotlinx.coroutines.flow", "Flow")
}

fun TypeName.defaultValue(): String {
    return if (!isPrimitive) {
        "null"
    } else if (this == TypeName.BOOLEAN) {
        "false"
    } else {
        "0"
    }
}

fun CallableTypeSpecBuilder(
    parameterTypeName: TypeName,
    callBody: MethodSpec.Builder.() -> Unit
) = TypeSpec.anonymousClassBuilder("").apply {
    superclass(ParameterizedTypeName.get(Callable::class.typeName, parameterTypeName))
    addMethod(
        MethodSpec.methodBuilder("call").apply {
            returns(parameterTypeName)
            addException(Exception::class.typeName)
            addModifiers(Modifier.PUBLIC)
            addAnnotation(Override::class.java)
            callBody()
        }.build()
    )
}

fun Function1TypeSpecBuilder(
    parameterTypeName: TypeName,
    parameterName: String,
    returnTypeName: TypeName,
    callBody: MethodSpec.Builder.() -> Unit
) = TypeSpec.anonymousClassBuilder("").apply {
    superclass(
        ParameterizedTypeName.get(
            Function1::class.typeName,
            parameterTypeName,
            returnTypeName
        )
    )
    addMethod(
        MethodSpec.methodBuilder("invoke").apply {
            addParameter(parameterTypeName, parameterName)
            returns(returnTypeName)
            addModifiers(Modifier.PUBLIC)
            addAnnotation(Override::class.java)
            callBody()
        }.build()
    )
}