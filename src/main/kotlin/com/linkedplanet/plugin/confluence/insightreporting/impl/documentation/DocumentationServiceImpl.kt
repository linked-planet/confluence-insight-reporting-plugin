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

package com.linkedplanet.plugin.confluence.insightreporting.impl.documentation

import com.atlassian.confluence.core.DefaultSaveContext
import com.atlassian.confluence.pages.Page
import com.atlassian.confluence.pages.PageManager
import com.atlassian.confluence.spaces.SpaceManager
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport
import com.atlassian.sal.api.ApplicationProperties
import com.atlassian.sal.api.UrlMode
import com.atlassian.sal.api.transaction.TransactionTemplate
import com.linkedplanet.plugin.confluence.insightreporting.api.documentation.DocumentationService
import com.linkedplanet.plugin.confluence.insightreporting.api.documentation.InsightDataNotFoundException
import com.linkedplanet.plugin.confluence.insightreporting.impl.confluence.TemplateService
import com.linkedplanet.plugin.confluence.insightreporting.impl.insight.InsightApi
import com.linkedplanet.plugin.confluence.insightreporting.impl.sync.SpaceSyncConfig
import com.linkedplanet.plugin.confluence.insightreporting.impl.sync.SpaceSyncConfigService
import com.linkedplanet.plugin.confluence.insightreporting.impl.util.RetryWithBackoff
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Named

@ExportAsService
@Named
class DocumentationServiceImpl
@Inject constructor(@ComponentImport private val pageManager: PageManager,
                    @ComponentImport private val spaceManager: SpaceManager,
                    @ComponentImport private val transactionTemplate: TransactionTemplate,
                    @ComponentImport private val applicationProperties: ApplicationProperties,
                    private val insightApi: InsightApi,
                    private val spaceSyncConfigService: SpaceSyncConfigService,
                    private val templateService: TemplateService) : DocumentationService {

    override fun getPageIds(spaceKey: String): List<Long> {
        val parentPageChildren by lazy {
            getParentPage(spaceKey)
                    ?.let { getPageChildrenRecursive(it) }
        }
        val spaceChildren by lazy {
            requireSpace(spaceKey)
                    .let { pageManager.getPages(it, true) }
                    .flatMap { getPageChildrenRecursive(it) }
        }

        return parentPageChildren ?: spaceChildren
    }

    private fun getPageChildrenRecursive(page: Page): List<Long> {
        val deepChildren = page.children.flatMap { getPageChildrenRecursive(it) }
        return deepChildren + (page.children.map { it.id })
    }

    override fun getParentPage(spaceKey: String): Page? =
            requireSpaceSyncConfig(spaceKey)
                    .parentPageId
                    ?.let { pageManager.getPage(it) }
                    ?: requireSpace(spaceKey).homePage

    override fun createPage(spaceKey: String, level: Int, objectKey: String, parentPageId: Long?): Long {
        LOG.info("Creating Page for Insight object (key= {}) ...", objectKey)
        val config = requireSpaceSyncConfig(spaceKey)
        val insightObject = requireInsightObject(config.schemaId, objectKey)
        val pageId = transactionTemplate.execute {
            createOrOverwritePage(config, spaceKey, level, insightObject, parentPageId)
        }
        updateConfluencePageLink(insightObject, pageId, config)
        return pageId
    }

    private fun requireSpace(spaceKey: String) = (spaceManager.getSpace(spaceKey)
            ?: throw IllegalStateException("Space $spaceKey does not exist"))

    private fun requireSpaceSyncConfig(spaceKey: String) = (spaceSyncConfigService.getConfig(spaceKey)
            ?: throw IllegalStateException("Space sync config does not exist"))

    @Throws(InsightDataNotFoundException::class)
    private fun requireInsightObject(objectSchemaId: Int, objectKey: String): InsightApi.InsightObject {
        val iqlServiceKey = "Key=$objectKey"
        val objectsViaIql = RetryWithBackoff.DEFAULT.execute("HTTP_INSIGHT_GET_OBJECTS_VIA_IQL") {
            insightApi.getObjectsViaIql(objectSchemaId, iqlServiceKey, true, true, 1, 1)
        }
        if (objectsViaIql.objectEntries.isEmpty()) {
            val message = "Object with key $objectKey not found in object schema $objectSchemaId"
            throw InsightDataNotFoundException(message)
        }
        return objectsViaIql.objectEntries.first()
    }

    private fun createOrOverwritePage(config: SpaceSyncConfig,
                                      spaceKey: String,
                                      level: Int,
                                      insightObject: InsightApi.InsightObject,
                                      parentPageId: Long?): Long {
        val pageTitle = insightObject.name
        return pageManager.getPage(spaceKey, pageTitle)
                ?.let { overwritePage(config, level, insightObject.id, it) }
                ?: createPage(config, spaceKey, level, pageTitle, insightObject.id, parentPageId)
    }

    private fun overwritePage(config: SpaceSyncConfig, level: Int, objectId: Int, page: Page): Long {
        page.bodyAsString = getPageContent(config, level, objectId)
        // here be dragons if you remove the SUPPRESS_NOTIFICATIONS from the save context:
        // https://jira.atlassian.com/browse/CONFSERVER-37161
        pageManager.saveContentEntity(page, DefaultSaveContext.SUPPRESS_NOTIFICATIONS)
        return page.id
    }

    private fun createPage(config: SpaceSyncConfig,
                           spaceKey: String,
                           level: Int,
                           pageTitle: String,
                           objectId: Int,
                           parentPageId: Long?): Long {
        val page = Page().apply {
            title = pageTitle
            bodyAsString = getPageContent(config, level, objectId)
            space = spaceManager.getSpace(spaceKey)

            parentPageId?.let {
                pageManager.getPage(it)?.let { parentPage ->
                    setParentPage(parentPage)
                    parentPage.addChild(this)
                }
            }

            pageManager.saveContentEntity(this, DefaultSaveContext.DEFAULT)
        }
        return page.id
    }

    private fun getPageContent(config: SpaceSyncConfig, level: Int, objectId: Int): String {
        val templateId = when (level) {
            0 -> config.templateId1
            1 -> config.templateId2
            2 -> config.templateId3
            else -> throw IllegalArgumentException("Level '$level' must be one of [0, 1, 2]")
        } ?: throw java.lang.IllegalStateException("No template configured for level $level")

        val template = templateService.getTemplate(templateId)
                ?: throw IllegalStateException("Page template with id '$templateId' does not exist.")

        return template.content.replaceFirst("\$INSIGHT_OBJECT_ID", objectId.toString())
    }

    private fun updateConfluencePageLink(insightObject: InsightApi.InsightObject, pageId: Long?, spaceSyncConfig: SpaceSyncConfig) {
        spaceSyncConfig.confluenceViewAttributeName?.let { viewAttrName ->
            val maybeConfluenceViewAttribute = insightObject.attributes?.firstOrNull { it.objectTypeAttribute.name == viewAttrName }
            if (maybeConfluenceViewAttribute != null) {
                val pageUrl = "${applicationProperties.getBaseUrl(UrlMode.CANONICAL)}/pages/viewpage.action?pageId=$pageId"
                LOG.info("Set confluence view for Insight object ${insightObject.objectKey} to page $pageUrl")
                RetryWithBackoff.DEFAULT.execute("HTTP_INSIGHT_UPSERT_OBJECT_ATTRIBUTE_VALUE") {
                    insightApi.upsertObjectAttributeValue(insightObject, maybeConfluenceViewAttribute, pageUrl)
                }
            } else {
                LOG.error("Cannot find attribute '$viewAttrName' in Insight object: ${insightObject.objectKey}."
                        + " Cannot link Confluence page to Insight object.")
            }
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(DocumentationServiceImpl::class.java)
    }

}
