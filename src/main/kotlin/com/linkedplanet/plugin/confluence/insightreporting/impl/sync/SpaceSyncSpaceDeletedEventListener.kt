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

import com.atlassian.confluence.event.events.space.SpaceRemoveEvent
import com.atlassian.event.api.EventListener
import com.atlassian.event.api.EventPublisher
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport
import org.slf4j.LoggerFactory
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import javax.inject.Inject
import javax.inject.Named


@Named
class SpaceSyncSpaceDeletedEventListener
@Inject constructor(@ComponentImport private val eventPublisher: EventPublisher,
                    private val spaceSyncConfigService: SpaceSyncConfigService) {

    @EventListener
    fun onEvent(event: SpaceRemoveEvent) {
        try {
            LOG.info("Removing space sync configuration for deleted space: ${event.space}.")
            spaceSyncConfigService.delete(event.space.key)
        } catch (e: Exception) {
            LOG.error("Error processing $event: $e.message", e)
        }
    }

    @PostConstruct
    private fun postConstruct() {
        eventPublisher.register(this)
    }

    @PreDestroy
    private fun preDestroy() {
        eventPublisher.unregister(this)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(SpaceSyncSpaceDeletedEventListener::class.java)
    }

}
