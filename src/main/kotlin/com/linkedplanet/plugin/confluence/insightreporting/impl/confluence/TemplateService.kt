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

package com.linkedplanet.plugin.confluence.insightreporting.impl.confluence

import com.atlassian.confluence.pages.templates.PageTemplate
import com.atlassian.confluence.pages.templates.PageTemplateManager
import com.atlassian.confluence.spaces.SpaceManager
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport
import javax.inject.Inject
import javax.inject.Named

@Named
class TemplateService
@Inject constructor(@ComponentImport private val spaceManager: SpaceManager,
                    @ComponentImport private val pageTemplateManager: PageTemplateManager) {

    fun getTemplates(spaceKey: String): List<PageTemplate> {
        val spaceTemplates = spaceManager.getSpace(spaceKey)?.pageTemplates ?: emptyList<PageTemplate>()
        val globalTemplates = pageTemplateManager.globalPageTemplates ?: emptyList<PageTemplate>()
        @Suppress("UNCHECKED_CAST") // SDK uses raw types, so we are forced to cast
        return (spaceTemplates as List<PageTemplate>) + (globalTemplates as List<PageTemplate>)
    }

    fun getTemplate(id: Long): PageTemplate? = pageTemplateManager.getPageTemplate(id)

}
