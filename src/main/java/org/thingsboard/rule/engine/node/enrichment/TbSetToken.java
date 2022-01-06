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
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.io.IOException;
import java.util.*;

import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;

/**
 * Created by Felix on 05.01.22.
 */
@RuleNode(
        type = ComponentType.ENRICHMENT,
        name = "Access Token",
        configClazz = TbGetSumIntoMetadataConfiguration.class,
        nodeDescription = "Genarate  Tokens",
        nodeDetails = "Okay fields in Message payload start with the <code>Input Key</code>, Sum of this fields added into metadata.",
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

    public static String unlockDevice(String deviceSecretKey,int msg_id) throws UnsupportedKeyMappingException, UnsupportedMessageDaysException, UnsupportedProtocolException, UnsupportedMessageIdException, IOException, UnsupportedMessageTypeException {
        byte[] secretKey = new HexToByteArray().convert(deviceSecretKey);

        KeycodeMetadata output = KeycodeFactory.unlock(msg_id, secretKey, KeycodeProtocol.FULL);
        String keycode = output.getKeycodeData().getKeycode();

        return keycode;
    }


    public static String generateFixedDaysToken(String deviceSecretKey,int msg_id,long neededTokens) throws UnsupportedKeyMappingException, UnsupportedMessageDaysException, UnsupportedProtocolException, UnsupportedMessageIdException, IOException, UnsupportedMessageTypeException {
        byte[] secretKey = new HexToByteArray().convert(deviceSecretKey);
        int messageId = msg_id;
        long tokens = neededTokens /* days */ * 24 /* hours */ * 60 /* minutes */ * 60 /* seconds */;
        Date clampedTime = new Date();
        KeycodeMetadata output = KeycodeFactory.addCredit(
                clampedTime,
                messageId,
                secretKey,
                KeycodeProtocol.FULL,
                tokens
        );
        String keycode = output.getKeycodeData().getKeycode();
       System.out.println(keycode);
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


        double tokens ;
        String tkn = null;
        String unlock = null;
        String set_tkn = null;

        boolean hasRecords = false;

        try {
            JsonNode jsonNode = mapper.readTree(msg.getData());

            long ss_msg_id = Long.parseLong(msg.getMetaData().getValue("ss_msg_id"));
            deviceSecretKey = msg.getMetaData().getValue("ss_device_secret");

            Iterator<String> iterator = jsonNode.fieldNames();
            while (iterator.hasNext()) {
                String field = iterator.next();
                if (field.startsWith(inputKey)) {
                    tkn = generateFixedDaysToken(deviceSecretKey, (int) ss_msg_id, jsonNode.get(field).asLong());
                    ss_msg_id++;
                    hasRecords = true;
                }
            }
            if (hasRecords) {
                AttributeKvEntry attrAOld = new BaseAttributeKvEntry(new LongDataEntry("msg_id",  ss_msg_id), msg.getTs());
                List<AttributeKvEntry> att = new ArrayList<>();
                att.add(0,attrAOld);

                ctx.getAttributesService().save(ctx.getTenantId(),msg.getOriginator(),"SERVER_SCOPE", att );

                msg.getMetaData().putValue(outputKey, tkn);
                msg.getMetaData().putValue("dev_id", String.valueOf(msg.getOriginator().getId()));
                msg.getMetaData().putValue("ss_msg_id", String.valueOf(ss_msg_id));
                msg.getMetaData().putValue("method", "POST");

                // Demonstrating Set using HashSet
                // Declaring object of type String
                Set<String> hash_Set = new HashSet<String>();

                // Adding elements to the Set
                // using add() method
                hash_Set.add("Geeks");
                hash_Set.add("For");
                hash_Set.add("Geeks");
                hash_Set.add("Example");
                hash_Set.add("Set");

                String juma = "Juma";

//                ctx.tellNext(msg, hash_Set);

                ctx.tellNext(msg, SUCCESS);


            } else {
                ctx.tellFailure(msg, new Exception("Message doesn't contains the Input Key: " + inputKey));
            }
        } catch (IOException | UnsupportedKeyMappingException | UnsupportedMessageDaysException | UnsupportedProtocolException | UnsupportedMessageIdException | UnsupportedMessageTypeException e) {
            ctx.tellFailure(msg, e);
        }
    }



    @Override
    public void destroy() {

    }
}
