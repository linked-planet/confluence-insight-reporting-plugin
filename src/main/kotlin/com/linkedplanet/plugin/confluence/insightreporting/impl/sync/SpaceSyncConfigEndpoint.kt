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

import org.codehaus.jackson.annotate.JsonProperty
import javax.inject.Inject
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/space-sync")
class SpaceSyncConfigEndpoint
@Inject constructor(private val spaceSyncConfigService: SpaceSyncConfigService) {

    @GET
    @Path("config/{space_key}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getConfig(@PathParam("space_key") spaceKey: String): Response {
        val config = spaceSyncConfigService.getConfig(spaceKey)
        return if (config == null) {
            Response.status(404).build()
        } else {
            val responseBody = ConfigJsonBody(
                    config.schemaId,
                    config.confluenceViewAttributeName,
                    config.parentPageId,
                    config.objectTypeId1,
                    config.objectTypeId2,
                    config.objectTypeId3,
                    config.templateId1,
                    config.templateId2,
                    config.templateId3)
            Response.ok().entity(responseBody).build()
        }
    }

    @PUT
    @Path("config/{space_key}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun putConfig(@PathParam("space_key") spaceKey: String, configBody: ConfigJsonBody): Response {
        val validationErrors = configBody.validate()
        if (validationErrors.isNotEmpty()) {
            return Response.status(400).entity(validationErrors).build()
        }

        fun <T : Number> negativeToNull(value: T?): T? = if (value == null || value.toLong() < 0L) null else value

        val config = spaceSyncConfigService.getConfig(spaceKey)
        if (config == null) {
            spaceSyncConfigService.create(
                    spaceKey,
                    configBody.parentPageId,
                    configBody.schemaId!!,
                    configBody.confluenceViewAttributeName,
                    configBody.objectTypeId1!!,
                    negativeToNull(configBody.objectTypeId2),
                    negativeToNull(configBody.objectTypeId3),
                    configBody.templateId1!!,
                    negativeToNull(configBody.templateId2),
                    negativeToNull(configBody.templateId3))
        } else {
            spaceSyncConfigService.update(
                    spaceKey,
                    configBody.parentPageId,
                    configBody.schemaId!!,
                    configBody.confluenceViewAttributeName,
                    configBody.objectTypeId1!!,
                    negativeToNull(configBody.objectTypeId2),
                    negativeToNull(configBody.objectTypeId3),
                    configBody.templateId1!!,
                    negativeToNull(configBody.templateId2),
                    negativeToNull(configBody.templateId3))
        }
        return Response.ok().build()
    }

    class ConfigJsonBody() {

        @JsonProperty("schemaId")
        var schemaId: Int? = null

        @JsonProperty("confluenceViewAttributeName")
        var confluenceViewAttributeName: String? = null

        @JsonProperty("parentPageId")
        var parentPageId: Long? = null

        @JsonProperty("objectTypeId1")
        var objectTypeId1: Int? = null

        @JsonProperty("objectTypeId2")
        var objectTypeId2: Int? = null

        @JsonProperty("objectTypeId3")
        var objectTypeId3: Int? = null

        @JsonProperty("templateId1")
        var templateId1: Long? = null

        @JsonProperty("templateId2")
        var templateId2: Long? = null

        @JsonProperty("templateId3")
        var templateId3: Long? = null

        constructor(schemaId: Int?,
                    confluenceViewAttributeName: String?,
                    parentPageId: Long?,
                    objectTypeId1: Int?,
                    objectTypeId2: Int?,
                    objectTypeId3: Int?,
                    templateId1: Long?,
                    templateId2: Long?,
                    templateId3: Long?) : this() {

            this.schemaId = schemaId
            this.confluenceViewAttributeName = confluenceViewAttributeName
            this.parentPageId = parentPageId
            this.objectTypeId1 = objectTypeId1
            this.objectTypeId2 = objectTypeId2
            this.objectTypeId3 = objectTypeId3
            this.templateId1 = templateId1
            this.templateId2 = templateId2
            this.templateId3 = templateId3
        }

        fun validate(): List<String> = mutableListOf<String>().apply {
            fun addErrorIfUnset(value: Number?, fieldName: String) {
                if (value == null || value.toLong() == -1L) this.add("$fieldName must be provided!")
            }
            addErrorIfUnset(schemaId, "Object Schema")
            addErrorIfUnset(objectTypeId1, "Object Type (Level 1)")
            addErrorIfUnset(templateId1, "Template (Level 1)")
        }

    }

}
