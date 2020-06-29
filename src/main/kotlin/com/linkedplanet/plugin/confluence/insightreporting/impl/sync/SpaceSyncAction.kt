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

import com.atlassian.applinks.api.ApplicationLinkService
import com.atlassian.applinks.api.CredentialsRequiredException
import com.atlassian.applinks.api.application.jira.JiraApplicationType
import com.atlassian.confluence.pages.PageManager
import com.atlassian.confluence.spaces.actions.SpaceAdminAction
import com.atlassian.confluence.util.longrunning.LongRunningTaskManager
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport
import com.atlassian.sal.api.message.I18nResolver
import com.opensymphony.xwork.Action
import com.linkedplanet.plugin.confluence.insightreporting.api.PluginComponent
import com.linkedplanet.plugin.confluence.insightreporting.api.documentation.DocumentationService
import com.linkedplanet.plugin.confluence.insightreporting.impl.confluence.TemplateService
import com.linkedplanet.plugin.confluence.insightreporting.impl.insight.InsightApi
import javax.inject.Inject


class SpaceSyncAction() : SpaceAdminAction() {

    private lateinit var i18n: I18nResolver
    private lateinit var pageManager: PageManager
    private lateinit var longRunningTaskManager: LongRunningTaskManager
    private lateinit var applicationLinkService: ApplicationLinkService
    private lateinit var templateService: TemplateService
    private lateinit var insightApi: InsightApi
    private lateinit var documentationService: DocumentationService
    private lateinit var spaceSyncConfigService: SpaceSyncConfigService

    @Suppress("MemberVisibilityCanBePrivate") // set by form, sdk sends checkbox as string
    var delete: String = ""

    @Suppress("MemberVisibilityCanBePrivate") // used by framework to initialize the task execution view
    var taskId: String? = null

    lateinit var objectSchemas: Map<String, String>
    var insightHealthy = true
    var authUri: String? = null

    @Inject
    @Suppress("unused") // called by framework (also needs default constructor)
    constructor(@ComponentImport i18n: I18nResolver,
                @ComponentImport pageManager: PageManager,
                @ComponentImport longRunningTaskManager: LongRunningTaskManager,
                @ComponentImport applicationLinkService: ApplicationLinkService,
                templateService: TemplateService,
                insightApi: InsightApi,
                documentationService: DocumentationService,
                spaceSyncConfigService: SpaceSyncConfigService) : this() {
        this.i18n = i18n
        this.pageManager = pageManager
        this.longRunningTaskManager = longRunningTaskManager
        this.applicationLinkService = applicationLinkService
        this.templateService = templateService
        this.insightApi = insightApi
        this.documentationService = documentationService
        this.spaceSyncConfigService = spaceSyncConfigService

        // initialize object schemas right at the start to detect connectivity issues
        this.objectSchemas = try {
            insightApi.getObjectSchemas().objectschemas.map { Pair(it.id.toString(), it.name) }.toMap()
        } catch (c: CredentialsRequiredException) {
            this.authUri = c.authorisationURI.toString()
            emptyMap()
        } catch (e: Exception) {
            this.insightHealthy = false
            emptyMap()
        }
    }

    override fun doDefault(): String = INPUT

    override fun execute(): String {
        val task = SpaceSyncTask(
                i18n, notificationManager, spaceManager, pageManager,
                insightApi, documentationService, spaceSyncConfigService,
                spaceKey, !delete.isBlank())
        taskId = longRunningTaskManager.startLongRunningTask(authenticatedUser, task).toString()
        return Action.SUCCESS
    }

    fun isJiraApplicationLinkConfigured(): Boolean =
            applicationLinkService.getPrimaryApplicationLink(JiraApplicationType::class.java) != null

    fun getI18nFieldLabel(configKey: String): String =
            i18n.getText("$I18N_PREFIX.$configKey.label")

    fun getI18nFieldDescription(configKey: String): String =
            i18n.getText("$I18N_PREFIX.$configKey.description")

    fun getSpaceTemplates(spaceKey: String): Map<String, String> =
            templateService.getTemplates(spaceKey).map { Pair(it.contentTemplateId.serialise(), it.name) }.toMap()

    companion object {
        private const val I18N_PREFIX = "${PluginComponent.PLUGIN_ID}.space-sync"
    }

}
