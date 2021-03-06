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

package com.zeoflow.depot.writer

import com.zeoflow.depot.vo.Entity

/**
 * Sorts the entities by their foreign key dependencies. For example, when Entity A depends on
 * Entity B, A is ordered before B.
 */
class EntityDeleteComparator : Comparator<Entity> {

    override fun compare(lhs: Entity, rhs: Entity): Int {
        val ltr = lhs.shouldBeDeletedAfter(rhs)
        val rtl = rhs.shouldBeDeletedAfter(lhs)
        return when {
            ltr == rtl -> 0
            ltr -> -1
            rtl -> 1
            else -> 0 // Never happens
        }
    }
}
