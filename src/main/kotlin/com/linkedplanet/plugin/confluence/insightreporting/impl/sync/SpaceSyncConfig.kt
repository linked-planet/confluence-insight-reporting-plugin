/*
 * #%L
 * insight-reporting
 * %%
 * Copyright (C) 2018 The Plugin Authors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package com.linkedplanet.plugin.confluence.insightreporting.impl.sync

import net.java.ao.Accessor
import net.java.ao.Entity
import net.java.ao.schema.Indexed
import net.java.ao.schema.NotNull
import net.java.ao.schema.Table
import net.java.ao.schema.Unique

@Suppress("unused") // entity
@Table("SpaceSyncConfig")
interface SpaceSyncConfig : Entity {

    @get:Indexed
    @get:NotNull
    @get:Unique
    var spaceKey: String

    @get:NotNull
    var schemaId: Int

    var confluenceViewAttributeName: String?

    var parentPageId: Long?

    @get:NotNull
    var objectTypeId1: Int
    var objectTypeId2: Int?
    var objectTypeId3: Int?

    @get:NotNull
    var templateId1: Long
    var templateId2: Long?
    var templateId3: Long?

    companion object {
        const val COLUMN_SPACE_KEY = "SPACE_KEY"
        const val COLUMN_SCHEMA_ID = "SCHEMA_ID"
        const val COLUMN_CONFLUENCE_VIEW_ATTRIBUTE_NAME = "CONFLUENCE_VIEW_ATTRIBUTE_NAME"
        const val COLUMN_PARENT_PAGE_ID = "PARENT_PAGE_ID"
        const val COLUMN_OBJECT_TYPE_ID_1 = "OBJECT_TYPE_ID1"
        const val COLUMN_OBJECT_TYPE_ID_2 = "OBJECT_TYPE_ID2"
        const val COLUMN_OBJECT_TYPE_ID_3 = "OBJECT_TYPE_ID3"
        const val COLUMN_TEMPLATE_ID_1 = "TEMPLATE_ID1"
        const val COLUMN_TEMPLATE_ID_2 = "TEMPLATE_ID2"
        const val COLUMN_TEMPLATE_ID_3 = "TEMPLATE_ID3"
    }

}
