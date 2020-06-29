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

interface InsightApi {

    fun getObjectSchemas(): InsightObjectSchemasResult

    fun getObjectTypes(objectSchemaId: Int): List<InsightObjectType>?

    fun getObjectTypeById(objectSchemaId: Int, id: Int): InsightObjectType? =
            getObjectTypes(objectSchemaId)?.find { it.id == id }

    fun getObjectsViaIql(objectSchemaId: Int,
                         iql: String,
                         includeAttributes: Boolean,
                         extended: Boolean,
                         page: Int,
                         resultPerPage: Int): InsightIqlObjectResult

    /**
     * Calls [InsightApi.getObjectsViaIql] as often as there are result pages remaining, starting
     * from the given page. For each result page, all objects are:
     *
     *  1. filtered via the provided filter function
     *  2. transformed via the provided mapper function
     *
     * The mapped results are collected and returned.
     */
    fun <T> reduceObjectsFromIqlAllPages(objectSchemaId: Int,
                                         iql: String,
                                         includeAttributes: Boolean,
                                         extended: Boolean,
                                         page: Int,
                                         resultPerPage: Int,
                                         filter: (InsightObject) -> Boolean,
                                         mapper: (InsightObject) -> T): List<T> {
        val result = getObjectsViaIql(objectSchemaId, iql, includeAttributes, extended, page, resultPerPage)
        val mapped = result.objectEntries.filter(filter).map(mapper)
        return if (result.pageSize > page) {
            mapped + reduceObjectsFromIqlAllPages(objectSchemaId, iql, includeAttributes, extended, page + 1, resultPerPage, filter, mapper)
        } else {
            mapped
        }
    }

    /**
     * Create or update an object attribute value.
     *
     * @see <a href="https://documentation.riada.se/display/ICV53/Object+Attributes+-+REST">Insight REST Documentation - Object Attributes</a>
     */
    fun upsertObjectAttributeValue(insightObject: InsightObject, attribute: InsightObjectAttribute, value: String)


    data class InsightIqlObjectResult(val objectEntries: List<InsightObject>, val pageSize: Int)

    data class InsightObjectSchemasResult(val objectschemas: List<InsightObjectSchema>)

    data class InsightObjectSchema(val id: Int, val name: String)

    data class InsightObject(val id: Int,
                             val objectKey: String,
                             val name: String,
                             val objectType: InsightObjectType,
            // attributes will be null when iql endpoint is called with includeAttributes set to false
                             val attributes: List<InsightObjectAttribute>?)

    data class InsightObjectType(val id: Int, val parentObjectTypeId: Int?, val name: String, val objectSchemaId: Int)

    data class InsightObjectTypeAttribute(val id: Int, val name: String, val objectType: InsightObjectType)

    data class InsightObjectAttribute(val id: Int,
                                      val objectTypeAttribute: InsightObjectTypeAttribute,
                                      val objectAttributeValues: List<InsightObjectAttributeValue>)

    data class InsightObjectAttributeValue(val value: String?, val referencedObject: InsightObjectReference? = null)

    data class InsightObjectReference(val id: Int, val key: String, val name: String, val objectType: InsightObjectType)

    companion object {
        const val API_PATH = "/rest/insight/1.0"


    }

}
