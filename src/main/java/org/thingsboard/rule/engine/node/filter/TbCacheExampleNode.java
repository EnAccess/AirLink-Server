/**
 * Copyright © 2018 The Thingsboard Authors
 *
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
 */
package org.thingsboard.rule.engine.node.filter;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.EmptyNodeConfiguration;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;


@Slf4j
@RuleNode(
        type = ComponentType.FILTER,
        name = "Cache example",
        relationTypes = {"True", "False"},
        configClazz = EmptyNodeConfiguration.class,
        nodeDescription = "Checks that the incoming value exceeds certain threshold",
        nodeDetails = "If temperature is too high - send Message via <b>True</b> chain, otherwise <b>False</b> chain is used.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbNodeEmptyConfig")
public class TbCacheExampleNode implements TbNode {

    private static final ObjectMapper mapper = new ObjectMapper();
    private ConcurrentMap<EntityId, Double> cache;


    @Override
    public void init(TbContext tbContext, TbNodeConfiguration configuration) throws TbNodeException {
        this.cache = new ConcurrentHashMap<>();
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        try {
            // Parsing the incoming message;
            ObjectNode json = (ObjectNode) mapper.readTree(msg.getData());
            double temperature = json.get("temperature").asDouble();
            // Fetching temperatureThreshold attribute from cache or from the database
            Double temperatureThreshold = getCacheValue(ctx, msg.getOriginator(), "temperatureAlarmThreshold", 42);
//            msg.getMetaData().putValue(outputKey, temperatureThreshold);
            // Compare and do something with the result of comparison;
            ctx.tellNext(msg, temperature > temperatureThreshold ? "True" : "False");
        } catch (JsonProcessingException e) {
            ctx.tellFailure(msg, e);
        }
    }

    @Override
    public void onPartitionChangeMsg(TbContext ctx, PartitionChangeMsg msg) {
        // Cleanup the cache for all entities that are no longer assigned to current server partitions
        cache.entrySet().removeIf(entry -> !ctx.isLocalEntity(entry.getKey()));
    }

    private double getCacheValue(TbContext ctx, EntityId entityId, String attributeKey, double defaultValue) {
        // Get value from cache or from the database.
        return cache.computeIfAbsent(entityId, id -> {
            try {
                Optional<AttributeKvEntry> attr = ctx.getAttributesService().find(ctx.getTenantId(), entityId, DataConstants.SERVER_SCOPE, attributeKey).get();
                if (attr.isPresent()) {
                    return attr.get().getDoubleValue().orElse(defaultValue);
                } else {
                    return defaultValue;
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }


    @Override
    public void destroy() {
        // In case you have changed the configuration, it is good idea to clear the entire cache.
        cache.clear();
    }
}
