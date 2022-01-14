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

import androidx.annotation.NonNull;

import com.zeoflow.depot.ColumnInfo;
import com.zeoflow.depot.Entity;
import com.zeoflow.depot.PrimaryKey;

import java.util.Date;

/**
 * A basic class representing an entity that is a row in a one-column database table.
 *
 * @ Entity - You must annotate the class as an entity and supply a table name if not class name.
 * @ PrimaryKey - You must identify the primary key.
 * @ ColumnInfo - You must supply the column name if it is different from the variable name.
 * <p>
 * See the documentation for the full rich set of annotations.
 * https://developer.android.com/topic/libraries/architecture/depot.html
 */

@Entity(tableName = "word_table")
public class Word
{

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "word")
    private final String mWord;

    private final Date date;

    public Word(@NonNull String mWord, Date date) {
        this.mWord = mWord;
        this.date = date;
    }

    @com.zeoflow.depot.Ignore
    public Word(@NonNull String word)
    {
        this.mWord = word;
        this.date = new Date();
    }

    @NonNull
    public String getWord()
    {
        return this.mWord;
    }

    public Date getDate() {
        return date;
    }

}
