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

import androidx.annotation.NonNull;
import com.zeoflow.sqlite.db.SupportSQLiteOpenHelper;

/**
 * Factory class for AutoClosingDepotOpenHelper
 */
final class AutoClosingDepotOpenHelperFactory implements SupportSQLiteOpenHelper.Factory {
    @NonNull
    private final SupportSQLiteOpenHelper.Factory mDelegate;

    @NonNull
    private final AutoCloser mAutoCloser;

    AutoClosingDepotOpenHelperFactory(
            @NonNull SupportSQLiteOpenHelper.Factory factory,
            @NonNull AutoCloser autoCloser) {
        mDelegate = factory;
        mAutoCloser = autoCloser;
    }

    /**
     * @return AutoClosingDepotOpenHelper instances.
     */
    @Override
    @NonNull
    public AutoClosingDepotOpenHelper create(
            @NonNull SupportSQLiteOpenHelper.Configuration configuration) {
        return new AutoClosingDepotOpenHelper(mDelegate.create(configuration), mAutoCloser);
    }
}
