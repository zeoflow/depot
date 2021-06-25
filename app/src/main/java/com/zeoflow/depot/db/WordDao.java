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

import androidx.lifecycle.LiveData;

import com.zeoflow.depot.dispatcher.Observable;
import com.zeoflow.depot.dispatcher.Void;
import com.zeoflow.depot.Dao;
import com.zeoflow.depot.Insert;
import com.zeoflow.depot.OnConflictStrategy;
import com.zeoflow.depot.Query;

import java.util.List;

/**
 * The Depot Magic is in this file, where you map a Java method call to an SQL query.
 * <p>
 * When you are using complex data types, such as Date, you have to also supply type converters.
 * To keep this example basic, no types that require type converters are used.
 * See the documentation at
 * https://developer.android.com/topic/libraries/architecture/depot.html#type-converters
 */

@Dao
public interface WordDao
{
    @Observable
    @Query("SELECT * FROM word_table ORDER BY word ASC")
    LiveData<List<Word>> getAlphabetizedWords();

    @Observable
    @Query("SELECT * FROM word_table ORDER BY word ASC")
    LiveData<List<Word>> getWords();

    @Query("SELECT * FROM word_table ORDER BY word ASC")
    List<Word> getAllWords();

    @Void("insertWord")
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Word word);

    @Void("deleteAll")
    @Query("DELETE FROM word_table")
    void deleteAll();

}
