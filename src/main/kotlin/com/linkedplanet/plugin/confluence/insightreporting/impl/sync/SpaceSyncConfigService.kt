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

import com.atlassian.activeobjects.tx.Transactional

@Transactional
interface SpaceSyncConfigService {

    fun getConfig(spaceKey: String): SpaceSyncConfig?

    fun create(spaceKey: String,
               parentPageId: Long?,
               schemaId: Int,
               confluenceViewAttributeName: String?,
               objectTypeId1: Int,
               objectTypeId2: Int?,
               objectTypeId3: Int?,
               templateId1: Long,
               templateId2: Long?,
               templateId3: Long?): SpaceSyncConfig

    fun update(spaceKey: String,
               parentPageId: Long?,
               schemaId: Int,
               confluenceViewAttributeName: String?,
               objectTypeId1: Int,
               objectTypeId2: Int?,
               objectTypeId3: Int?,
               templateId1: Long,
               templateId2: Long?,
               templateId3: Long?): SpaceSyncConfig

    /**
     * Deletes the space sync config associated with the given space key.
     *
     * Does nothing if no such config exists.
     */
    fun delete(spaceKey: String)

}
