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

package com.linkedplanet.plugin.confluence.insightreporting.impl.util

import org.slf4j.LoggerFactory

class RetryWithBackoff private constructor(private val maxRetries: Int,
                                           private val backoffMinSeconds: Int,
                                           private val backoffMaxSeconds: Int,
                                           private val backoffFactor: Int) {

    fun <T> execute(description: String, func: () -> T): T =
            execute(description, func, 0, backoffMinSeconds)

    private fun <T> execute(description: String, func: () -> T, attempt: Int, backoff: Int): T =
            try {
                func.invoke()
            } catch (e: Exception) {
                if (attempt >= maxRetries) {
                    throw RuntimeException("Giving up on operation: $description (tried $maxRetries times)", e)
                } else {
                    LOG.error("Operation failed: {} (attempt {}/{}, retry in: {} seconds)", description, attempt, maxRetries, backoff, e)
                    try {
                        Thread.sleep((backoff * 1000).toLong())
                    } catch (e1: InterruptedException) {
                        throw RuntimeException(e)
                    }
                    execute(description, func, attempt + 1, Math.min(backoffMaxSeconds, backoff * backoffFactor))
                }
            }

    companion object {
        private val LOG = LoggerFactory.getLogger(RetryWithBackoff::class.java)
        val DEFAULT = RetryWithBackoff(10, 1, 120, 2)
    }

}
