package com.zeoflow.depot.db;

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

import com.zeoflow.depot.dispatcher.DispatcherName;
import com.zeoflow.depot.dispatcher.DepotDispatcher;
import com.zeoflow.depot.DepotDatabase;

/**
 * This is the backend. The database. This used to be done by the OpenHelper.
 * The fact that this has very few comments emphasizes its coolness.  In a real
 * app, consider exporting the schema to help you with migrations.
 */

@DispatcherName("ZeoFlow")
@DepotDispatcher(
        entities = {
                Word.class,
                User.class
        },
        dbBeans = {
                WordDao.class,
                UserDao.class
        },
        converters = {
                Converters.class
        },
        version = 1,
        exportSchema = false
)
public abstract class WordDepotDatabase extends DepotDatabase
{
    // empty constructor
}
