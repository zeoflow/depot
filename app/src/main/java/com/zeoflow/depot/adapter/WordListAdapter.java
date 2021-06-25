package com.zeoflow.depot.adapter;

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

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.zeoflow.depot.db.Word;

import org.jetbrains.annotations.NotNull;

public class WordListAdapter extends ListAdapter<Word, WordViewHolder>
{

    public WordListAdapter(@NonNull DiffUtil.ItemCallback<Word> diffCallback)
    {
        super(diffCallback);
    }

    @NotNull
    @Override
    public WordViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType)
    {
        return WordViewHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(WordViewHolder holder, int position)
    {
        Word current = getItem(position);
        holder.bind(current.getWord());
    }

    public static class WordDiff extends DiffUtil.ItemCallback<Word>
    {

        @Override
        public boolean areItemsTheSame(@NonNull Word oldItem, @NonNull Word newItem)
        {
            return oldItem == newItem;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Word oldItem, @NonNull Word newItem)
        {
            return oldItem.getWord().equals(newItem.getWord());
        }

    }

}
