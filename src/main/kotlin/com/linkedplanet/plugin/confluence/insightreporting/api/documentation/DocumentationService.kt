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

package com.linkedplanet.plugin.confluence.insightreporting.api.documentation

import com.atlassian.confluence.pages.Page

interface DocumentationService {

    /**
     * Collects the IDs of all documentation pages - these are all pages found below the
     *
     * 1. configured parent page (if any)
     * 2. home page of the space (if any)
     * 3. space
     *
     * IDs of all child pages will also be returned recursively. The returned list is sorted
     * depth-first.
     */
    fun getPageIds(spaceKey: String): List<Long>

    /**
     * Returns the parent page under which all documentation pages for the given space
     * reside.
     *
     * This could either be a configured parent page, the space' homepage or none.
     */
    fun getParentPage(spaceKey: String): Page?

    /**
     * Create (potentially overwrite) the documentation page for the given Insight object.
     *
     * @param spaceKey          Confluence Space to create the page in
     * @param level             The hierarchy level of the page (determines the template to be used)
     * @param objectKey         Insight Object Key identifying the Insight object
     * @param parentPageId      Optional parent page id
     * @return Documentation Page ID
     * @throws InsightDataNotFoundException if no such Insight object could be found
     */
    @Throws(InsightDataNotFoundException::class)
    fun createPage(spaceKey: String, level: Int, objectKey: String, parentPageId: Long?): Long

}
