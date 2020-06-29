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

import com.google.gson.GsonBuilder
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/insight")
class InsightEndpoint
@Inject constructor(private val insightApi: InsightApi) {

    @GET
    @Path("schema/{schema_id}/object-types")
    @Produces(MediaType.APPLICATION_JSON)
    fun getObjectTypes(@PathParam("schema_id") schemaId: Int): Response {
        val objectTypes = insightApi.getObjectTypes(schemaId)
        return if (objectTypes == null) {
            Response.status(404).build()
        } else {
            val objectTypeMap = objectTypes.map { Pair(it.id, it.name) }.toMap()
            Response.ok().entity(GSON.toJson(objectTypeMap)).build()
        }
    }

    private companion object {
        private val GSON = GsonBuilder().create()
    }

}
