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

import com.atlassian.confluence.mail.notification.Notification
import com.atlassian.confluence.mail.notification.NotificationManager
import com.atlassian.confluence.pages.PageManager
import com.atlassian.confluence.pages.persistence.dao.bulk.delete.PageDeleteOptions
import com.atlassian.confluence.spaces.SpaceManager
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal
import com.atlassian.confluence.util.longrunning.ConfluenceAbstractLongRunningTask
import com.atlassian.core.util.ProgressMeter
import com.atlassian.sal.api.message.I18nResolver
import com.linkedplanet.plugin.confluence.insightreporting.api.PluginComponent
import com.linkedplanet.plugin.confluence.insightreporting.api.documentation.DocumentationService
import com.linkedplanet.plugin.confluence.insightreporting.api.documentation.InsightDataNotFoundException
import com.linkedplanet.plugin.confluence.insightreporting.impl.insight.InsightApi
import com.linkedplanet.plugin.confluence.insightreporting.impl.util.RetryWithBackoff

class SpaceSyncTask(private val i18n: I18nResolver,
                    private val notificationManager: NotificationManager,
                    private val spaceManager: SpaceManager,
                    private val pageManager: PageManager,
                    private val insightApi: InsightApi,
                    private val documentationService: DocumentationService,
                    private val spaceSyncConfigService: SpaceSyncConfigService,
                    private val spaceKey: String,
                    private val delete: Boolean) : ConfluenceAbstractLongRunningTask() {

    override fun getName(): String = i18n.getText(I18N_LABEL)

    override fun runInternal() {
        try {
            val config = spaceSyncConfigService.getConfig(spaceKey)
                    ?: throw IllegalStateException("Cannot run space sync task without space sync config")
            run(config)

        } catch (e: Exception) {
            val message = i18n.getText(I18N_STATUS_FAILED, e.message)
            log.error(message, e)
            progress.status = message
            progress.isCompletedSuccessfully = false
        }
    }

    private fun run(config: SpaceSyncConfig) {
        val spaceWatchers = removeSpaceWatchers()
        try {
            val l1Keys = getInsightObjectKeys(config.schemaId, config.objectTypeId1)

            // parentKey -> List(childKeys)
            val l1KeysToL2Children: Map<String, List<String>> =
                    config.objectTypeId2?.let {
                        l1Keys
                                .map { key -> key to getInsightObjectKeys(config.schemaId, it, key) }.toMap()
                    } ?: emptyMap()

            // parentKey -> List(childKeys)
            val l2KeysToL3Children: Map<String, List<String>> =
                    config.objectTypeId3?.let {
                        l1KeysToL2Children.values.flatten()
                                .map { key -> key to getInsightObjectKeys(config.schemaId, it, key) }.toMap()
                    } ?: emptyMap()

            // level -> (parentKey -> List(childKeys))
            val pageKeyMap = listOf(mapOf<String?, List<String>>(null to l1Keys), l1KeysToL2Children, l2KeysToL3Children)
                    .filter { it.isNotEmpty() }

            val pageWorkItems = pageKeyMap.flatMap { it.values }.flatten().size
            progress.setTotalObjects(pageWorkItems)

            if (delete) {
                val deletePageIds = documentationService.getPageIds(spaceKey)
                progress.setTotalObjects(pageWorkItems + deletePageIds.size)
                deletePages(deletePageIds)
            }

            val parentPageId = documentationService.getParentPage(spaceKey)?.id
            createPages(config.schemaId, pageKeyMap, 0, null, parentPageId)

        } finally {
            restoreSpaceWatchers(spaceWatchers)
        }
        progress.status = i18n.getText(I18N_STATUS_COMPLETED)
    }

    private fun removeSpaceWatchers(): List<Notification> {
        progress.status = i18n.getText(I18N_STATUS_REMOVING_SPACE_WATCHERS)
        val serviceSpace = spaceManager.getSpace(spaceKey)
        val spaceWatchers = notificationManager.getNotificationsBySpaceAndType(serviceSpace, null)
        notificationManager.removeAllNotificationsForSpace(serviceSpace)
        return spaceWatchers
    }

    private fun restoreSpaceWatchers(spaceWatchers: List<Notification>) {
        progress.status = i18n.getText(I18N_STATUS_RESTORING_SPACE_WATCHERS)
        val serviceSpace = spaceManager.getSpace(spaceKey)
        spaceWatchers.forEach { notificationManager.addSpaceNotification(it.receiver, serviceSpace) }
    }

    private fun getInsightObjectKeys(objectSchemaId: Int, objectTypeId: Int, outboundToKey: String? = null): List<String> {
        progress.status = i18n.getText(I18N_STATUS_COLLECTING_INSIGHT_KEYS)
        return RetryWithBackoff.DEFAULT.execute("HTTP_INSIGHT_GET_OBJECT_KEYS") {
            // additional rest call being made here, to identify configuration error quickly
            insightApi.getObjectTypeById(objectSchemaId, objectTypeId)
                    ?: throw InsightDataNotFoundException(i18n.getText(I18N_ERROR_OBJECT_TYPE_DOES_NOT_EXIST, objectTypeId))

            val outboundIql = outboundToKey?.let { " AND object having outboundReferences(key = $it)" } ?: ""
            insightApi.reduceObjectsFromIqlAllPages(
                    objectSchemaId,
                    "ObjectTypeId=$objectTypeId$outboundIql",
                    false,
                    false,
                    1,
                    25,
                    { true },
                    { o -> o.objectKey })
        }
    }

    private fun deletePages(pageIds: List<Long>) {
        for (id in pageIds) {
            val message = i18n.getText(I18N_STATUS_DELETING_PAGE, id)
            progress.status = message
            log.info(message)

            val pageToDelete = pageManager.getPage(id)
            // page might have been deleted in the meantime
            if (pageToDelete != null) {
                pageManager.deepDeletePage(
                        PageDeleteOptions.builder()
                                .withProgressMeter(ProgressMeter())
                                .withPageId(pageToDelete.id)
                                .withUser(AuthenticatedUserThreadLocal.get())
                                .build(),
                        pageToDelete)
            }
            progressIncrement()
        }
    }

    private fun createPages(objectSchemaId: Int,
                            pageKeyMap: List<Map<out String?, List<String>>>,
                            level: Int,
                            parentKey: String?,
                            parentPageId: Long?) {
        for (key in pageKeyMap[level][parentKey]!!) { // programming error if there is no item
            val message = i18n.getText(I18N_STATUS_CREATING_PAGE, key)
            progress.status = message
            log.info(message)

            try {
                val pageId = documentationService.createPage(spaceKey, level, key, parentPageId)
                progressIncrement()
                if (level < pageKeyMap.size - 1) {
                    createPages(objectSchemaId, pageKeyMap, level + 1, key, pageId)
                }

            } catch (e: InsightDataNotFoundException) {
                // object might not exist anymore by the time we get here
                log.warn("Page for Insight object key $key not created. Reason: " + e.message, e)
            }
        }
    }

    @Synchronized
    private fun progressIncrement() {
        progress.currentCount = progress.currentCount + 1
    }

    companion object {
        private const val I18N_PREFIX = "${PluginComponent.PLUGIN_ID}.space-sync"
        private const val I18N_LABEL = "$I18N_PREFIX.label"
        private const val I18N_ERROR_OBJECT_TYPE_DOES_NOT_EXIST = "$I18N_PREFIX.error.objectTypeDoesNotExist"
        private const val I18N_STATUS_REMOVING_SPACE_WATCHERS = "$I18N_PREFIX.status.removingSpaceWatchers"
        private const val I18N_STATUS_COLLECTING_INSIGHT_KEYS = "$I18N_PREFIX.status.collectingInsightKeys"
        private const val I18N_STATUS_RESTORING_SPACE_WATCHERS = "$I18N_PREFIX.status.restoringSpaceWatchers"
        private const val I18N_STATUS_DELETING_PAGE = "$I18N_PREFIX.status.deletingPage"
        private const val I18N_STATUS_CREATING_PAGE = "$I18N_PREFIX.status.creatingPage"
        private const val I18N_STATUS_COMPLETED = "$I18N_PREFIX.status.completed"
        private const val I18N_STATUS_FAILED = "$I18N_PREFIX.status.failed"
    }

}
