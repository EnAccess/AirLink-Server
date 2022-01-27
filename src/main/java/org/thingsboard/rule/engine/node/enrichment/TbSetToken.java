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
package org.thingsboard.rule.engine.node.enrichment;

import com.angaza.nexus.keycode.KeycodeFactory;
import com.angaza.nexus.keycode.KeycodeMetadata;
import com.angaza.nexus.keycode.KeycodeProtocol;
import com.angaza.nexus.keycode.exceptions.*;
import com.angaza.nexus.keycode.full.FullMessage;
import com.angaza.nexus.keycode.util.HexToByteArray;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.io.IOException;
import java.util.*;

import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;

/**
 * Created by Simusolar on 05.01.22.
 */
@RuleNode(
        type = ComponentType.ENRICHMENT,
        name = "Set Specific PAYG Credits",
        configClazz = TbGetSumIntoMetadataConfiguration.class,
        nodeDescription = "Generate Token To Set Specific Days On Device",
        nodeDetails = "The Value of the input field will be set as the new value to the device <code>Input Key</code>.",
        uiResources = {"static/rulenode/custom-nodes-config.js"},
        configDirective = "tbEnrichmentNodeSumIntoMetadataConfig")
public class TbSetToken implements TbNode {

    private static final ObjectMapper mapper = new ObjectMapper();

    private TbGetSumIntoMetadataConfiguration config;
    private String inputKey;
    private String outputKey;


    public static String setCredit(String deviceSecretKey,int msg_id,int neededCredits) throws IOException, UnsupportedMessageIdException {
        byte[] secretKey = new HexToByteArray().convert(deviceSecretKey);

        int credits = neededCredits /* days */ * 24 /* hours */;
        FullMessage message = FullMessage.setCredit(msg_id, credits, secretKey);
        String keycode = message.toKeycode();

        return keycode;

    }

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbGetSumIntoMetadataConfiguration.class);
        inputKey = config.getInputKey();
        outputKey = config.getOutputKey();
    }


    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {

        String deviceSecretKey = null;
        String tkn = null;

        boolean hasRecords = false;

        try {
            JsonNode jsonNode = mapper.readTree(msg.getData());

            long ss_msg_id = Long.parseLong(msg.getMetaData().getValue("ss_msg_id"));
            deviceSecretKey = msg.getMetaData().getValue("ss_device_secret");

            Iterator<String> iterator = jsonNode.fieldNames();
            while (iterator.hasNext()) {
                String field = iterator.next();
                if (field.startsWith(inputKey)) {
                    tkn = setCredit(deviceSecretKey, (int) ss_msg_id, (int) jsonNode.get(field).asLong());
                    ss_msg_id++;
                    hasRecords = true;
                }
            }
            if (hasRecords) {
                //                Save Server Attributes
                AttributeKvEntry attrServer = new BaseAttributeKvEntry(new LongDataEntry("msg_id",  ss_msg_id), msg.getTs());
                List<AttributeKvEntry> server_att = new ArrayList<>();
                server_att.add(0,attrServer);

                ctx.getAttributesService().save(ctx.getTenantId(),msg.getOriginator(),"SERVER_SCOPE", server_att );

//                Save Shared Attributes
                AttributeKvEntry attrShared = new BaseAttributeKvEntry(new StringDataEntry("PC_tkn",  tkn), msg.getTs());
                List<AttributeKvEntry> shared_att = new ArrayList<>();
                shared_att.add(0,attrShared);

                ctx.getAttributesService().save(ctx.getTenantId(),msg.getOriginator(),"SHARED_SCOPE", shared_att );

                msg.getMetaData().putValue(outputKey, tkn);
                msg.getMetaData().putValue("dev_id", String.valueOf(msg.getOriginator().getId()));
                msg.getMetaData().putValue("ss_msg_id", String.valueOf(ss_msg_id));

                ctx.tellNext(msg, SUCCESS);

            } else {
                ctx.tellFailure(msg, new Exception("Message doesn't contains the Input Key: " + inputKey));
            }
        } catch (IOException | UnsupportedMessageIdException e) {
            ctx.tellFailure(msg, e);
        }
    }



    @Override
    public void destroy() {

    }
}
