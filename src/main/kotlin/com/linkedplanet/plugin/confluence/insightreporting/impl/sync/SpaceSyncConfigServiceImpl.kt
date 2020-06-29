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

import com.atlassian.activeobjects.external.ActiveObjects
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport
import net.java.ao.DBParam
import net.java.ao.Query
import javax.inject.Inject
import javax.inject.Named

@Named
class SpaceSyncConfigServiceImpl
@Inject constructor(@ComponentImport private val activeObjects: ActiveObjects) : SpaceSyncConfigService {

    override fun getConfig(spaceKey: String): SpaceSyncConfig? =
    // SpaceSyncTask will fail with "No Hibernate Session bound to thread" otherwise
    // don't understand why, interface is marked as @Transactional
            activeObjects.executeInTransaction {
                activeObjects.find(
                        SpaceSyncConfig::class.java,
                        Query.select().where("${SpaceSyncConfig.COLUMN_SPACE_KEY} = ?", spaceKey))
                        .firstOrNull()
            }

    override fun create(spaceKey: String,
                        parentPageId: Long?,
                        schemaId: Int,
                        confluenceViewAttributeName: String?,
                        objectTypeId1: Int,
                        objectTypeId2: Int?,
                        objectTypeId3: Int?,
                        templateId1: Long,
                        templateId2: Long?,
                        templateId3: Long?): SpaceSyncConfig =
            activeObjects.create(
                    SpaceSyncConfig::class.java,
                    DBParam(SpaceSyncConfig.COLUMN_SPACE_KEY, spaceKey),
                    DBParam(SpaceSyncConfig.COLUMN_SCHEMA_ID, schemaId),
                    DBParam(SpaceSyncConfig.COLUMN_CONFLUENCE_VIEW_ATTRIBUTE_NAME, confluenceViewAttributeName),
                    DBParam(SpaceSyncConfig.COLUMN_PARENT_PAGE_ID, parentPageId),
                    DBParam(SpaceSyncConfig.COLUMN_OBJECT_TYPE_ID_1, objectTypeId1),
                    DBParam(SpaceSyncConfig.COLUMN_OBJECT_TYPE_ID_2, objectTypeId2),
                    DBParam(SpaceSyncConfig.COLUMN_OBJECT_TYPE_ID_3, objectTypeId3),
                    DBParam(SpaceSyncConfig.COLUMN_TEMPLATE_ID_1, templateId1),
                    DBParam(SpaceSyncConfig.COLUMN_TEMPLATE_ID_2, templateId2),
                    DBParam(SpaceSyncConfig.COLUMN_TEMPLATE_ID_3, templateId3))

    override fun update(spaceKey: String,
                        parentPageId: Long?,
                        schemaId: Int,
                        confluenceViewAttributeName: String?,
                        objectTypeId1: Int,
                        objectTypeId2: Int?,
                        objectTypeId3: Int?,
                        templateId1: Long,
                        templateId2: Long?,
                        templateId3: Long?): SpaceSyncConfig =
            getConfig(spaceKey)!!.apply {
                this.schemaId = schemaId
                this.confluenceViewAttributeName = confluenceViewAttributeName
                this.parentPageId = parentPageId
                this.objectTypeId1 = objectTypeId1
                this.objectTypeId2 = objectTypeId2
                this.objectTypeId3 = objectTypeId3
                this.templateId1 = templateId1
                this.templateId2 = templateId2
                this.templateId3 = templateId3
                this.save()
            }

    override fun delete(spaceKey: String) {
        getConfig(spaceKey)?.let { activeObjects.delete(it) }
    }

}
