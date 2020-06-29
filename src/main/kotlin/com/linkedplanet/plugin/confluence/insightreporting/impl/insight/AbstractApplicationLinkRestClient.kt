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
import com.atlassian.applinks.api.ApplicationLinkResponseHandler
import com.atlassian.applinks.api.ApplicationLinkService
import com.atlassian.sal.api.net.Request
import com.atlassian.sal.api.net.Response
import com.atlassian.sal.api.net.ResponseException
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.slf4j.LoggerFactory
import java.lang.reflect.Type
import javax.ws.rs.core.HttpHeaders

abstract class AbstractApplicationLinkRestClient(
        private val applicationLinkService: ApplicationLinkService,
        private val apiPath: String
) {

    private val targetUrl: String
        get() = getApplicationLink(applicationLinkService).displayUrl.toString()

    fun <T> executeGet(path: String, returnType: Type): T? =
            executeGetCall(path)?.let { GSON.fromJson<T>(it, returnType) }

    fun <T> executeGetReturnList(path: String, returnType: Type): List<T>? =
            executeGetCall(path)?.let { GSON.fromJson(it, returnType) }

    private fun executeGetCall(path: String): String? =
            executeRestCall(Request.MethodType.GET, path, null, null)

    /**
     * Return the relevant application link for this REST client.
     *
     * @throws IllegalStateException if the required application link is not configured
     */
    @Throws(IllegalStateException::class)
    internal abstract fun getApplicationLink(applicationLinkService: ApplicationLinkService): ApplicationLink

    protected fun executeRestCall(method: Request.MethodType, path: String, body: String?, contentType: String?): String? {
        val fullPath = apiPath + path
        LOG.debug("Call to: $targetUrl$fullPath")

        try {
            val requestFactory = getApplicationLink(applicationLinkService).createAuthenticatedRequestFactory()
            val requestWithoutBody = requestFactory.createRequest(method, fullPath)
            val request = if (body == null) {
                requestWithoutBody
            } else {
                requestWithoutBody.setRequestBody(body).setHeader(HttpHeaders.CONTENT_TYPE, contentType)
            }
            return request.execute(object : ApplicationLinkResponseHandler<String> {
                override fun credentialsRequired(response: Response): String? {
                    return null
                }

                @Throws(ResponseException::class)
                override fun handle(response: Response): String? {
                    return when {
                        response.isSuccessful -> response.responseBodyAsString
                        response.statusCode == 404 -> null
                        else -> {
                            val errorWithStatusCode = "Call to " + fullPath + " failed - " + response.statusCode
                            val errorResponse = GSON.fromJson(response.responseBodyAsString, ErrorResponse::class.java)
                            val message = if (errorResponse == null)
                                errorWithStatusCode
                            else
                                errorWithStatusCode + ": " + errorResponse.errorMessages
                            throw ResponseException(message)
                        }
                    }
                }
            })

        } catch (e: ResponseException) {
            throw RuntimeException(e)
        }
    }

    private class ErrorResponse(val errorMessages: List<String>)

    companion object {
        private val LOG = LoggerFactory.getLogger(AbstractApplicationLinkRestClient::class.java)
        private val GSON: Gson = GsonBuilder().create()
    }

}
