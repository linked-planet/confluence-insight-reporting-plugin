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

package com.linkedplanet.plugin.confluence.insightreporting.impl.insight

import com.atlassian.applinks.api.ApplicationLink
import com.atlassian.applinks.api.ApplicationLinkService
import com.atlassian.applinks.api.application.jira.JiraApplicationType
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport
import com.atlassian.sal.api.net.Request
import com.google.gson.reflect.TypeToken
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Named
import javax.ws.rs.core.MediaType

@Named
class ApplicationLinkInsightApi
@Inject constructor(@ComponentImport applicationLinkService: ApplicationLinkService)
    : AbstractApplicationLinkRestClient(applicationLinkService, InsightApi.API_PATH), InsightApi {

    override fun getApplicationLink(applicationLinkService: ApplicationLinkService): ApplicationLink =
            applicationLinkService.getPrimaryApplicationLink(JiraApplicationType::class.java)
                    ?: throw IllegalStateException(
                            JiraApplicationType::class.java.simpleName + " primary application link not configured.")

    override fun getObjectSchemas(): InsightApi.InsightObjectSchemasResult {
        val path = "/objectschema/list"
        return executeGet(path, InsightApi.InsightObjectSchemasResult::class.java)!!
    }

    override fun getObjectTypes(objectSchemaId: Int): List<InsightApi.InsightObjectType>? {
        val path = "/objectschema/$objectSchemaId/objecttypes/flat"
        return executeGetReturnList(path, object : TypeToken<List<InsightApi.InsightObjectType>>() {}.type)
    }

    override fun getObjectsViaIql(objectSchemaId: Int, iql: String, includeAttributes: Boolean, extended: Boolean, page: Int, resultPerPage: Int): InsightApi.InsightIqlObjectResult {
        val path = "/iql/objects?objectSchemaId=" + objectSchemaId + "" +
                "&iql=" + urlEncode(iql) + "" +
                "&includeAttributes=" + includeAttributes +
                "&extended=" + extended +
                "&page=" + page +
                "&resultPerPage=" + resultPerPage
        return executeGet(path, InsightApi.InsightIqlObjectResult::class.java)
                ?: InsightApi.InsightIqlObjectResult(emptyList(), 0)
    }


    override fun upsertObjectAttributeValue(insightObject: InsightApi.InsightObject, attribute: InsightApi.InsightObjectAttribute, value: String) {
        if (attribute.objectAttributeValues.isEmpty()) {
            postObjectAttributeValue(insightObject.id, attribute.objectTypeAttribute.id, value)
        } else {
            putObjectAttributeValue(attribute.id, value)
        }
    }

    private fun postObjectAttributeValue(objectId: Int, objectTypeAttributeId: Int, value: String) {
        val path = "/objectattribute/create"
        val jsonBody = "{" +
                "\"objectId\":" + objectId + "," +
                "\"objectTypeAttributeId\":" + objectTypeAttributeId + "," +
                "\"objectAttributeValues\":[{\"value\":\"" + value + "\"}]" +
                "}"
        executeRestCall(Request.MethodType.POST, path, jsonBody, MediaType.APPLICATION_JSON)
    }

    private fun putObjectAttributeValue(id: Int, value: String) {
        val path = "/objectattribute/$id"
        val jsonBody = "{\"objectAttributeValues\":[{\"value\": \"$value\"}]}"
        executeRestCall(Request.MethodType.PUT, path, jsonBody, MediaType.APPLICATION_JSON)
    }


    companion object {
        private fun urlEncode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())
    }


}
