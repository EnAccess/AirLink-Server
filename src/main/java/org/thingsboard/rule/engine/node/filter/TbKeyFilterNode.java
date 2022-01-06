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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.io.IOException;
import java.util.Date;

import com.angaza.nexus.keycode.KeycodeFactory;
import com.angaza.nexus.keycode.KeycodeMetadata;
import com.angaza.nexus.keycode.KeycodeProtocol;
import com.angaza.nexus.keycode.exceptions.*;
import com.angaza.nexus.keycode.util.HexToByteArray;



class TestHelloWorld {
    static String deviceSecretKey = "deadbeefdeadbeefdeadbeefdeadbeef";

    public TestHelloWorld() {
    }

    public static void main(String[] args) throws UnsupportedKeyMappingException, UnsupportedMessageDaysException, UnsupportedProtocolException, UnsupportedMessageIdException, IOException, UnsupportedMessageTypeException {
        generateFixedDaysToken();
        generateUnlockCode();
    }

    public static void generateFixedDaysToken() throws UnsupportedKeyMappingException, UnsupportedMessageDaysException, UnsupportedProtocolException, UnsupportedMessageIdException, IOException, UnsupportedMessageTypeException {
        byte[] secretKey = new HexToByteArray().convert(deviceSecretKey);
        int messageId = 42;
        long seconds = 80 /* days */ * 24 /* hours */ * 60 /* minutes */ * 60 /* seconds */;
        Date clampedTime = new Date();
        KeycodeMetadata output = KeycodeFactory.addCredit(
                clampedTime,
                messageId,
                secretKey,
                KeycodeProtocol.FULL,
                seconds
        );
        String keycode = output.getKeycodeData().getKeycode();
       System.out.println(keycode);
    }

    public static void generateUnlockCode() throws UnsupportedKeyMappingException, UnsupportedMessageDaysException, UnsupportedProtocolException, UnsupportedMessageIdException, IOException, UnsupportedMessageTypeException {
        byte[] secretKey = new HexToByteArray().convert(deviceSecretKey);
        int messageId = 44;
        KeycodeMetadata output = KeycodeFactory.unlock(messageId, secretKey, KeycodeProtocol.FULL);
        String keycode = output.getKeycodeData().getKeycode();
        System.out.println(keycode);

    }
}


@Slf4j
@RuleNode(
        type = ComponentType.FILTER,
        name = "check felix key",
        relationTypes = {"True", "False"},
        configClazz = TbKeyFilterNodeConfiguration.class,
        nodeDescription = "Checks the existence of the selected key in the message payload.",
        nodeDetails = "If the selected key  exists - send Message via <b>True</b> chain, otherwise <b>False</b> chain is used.",
        uiResources = {"static/rulenode/custom-nodes-config.js"},
        configDirective = "tbFilterNodeCheckKeyConfig")
public class TbKeyFilterNode implements TbNode {

    private static final ObjectMapper mapper = new ObjectMapper();

    private TbKeyFilterNodeConfiguration config;
    private String key;


    @Override
    public void init(TbContext tbContext, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbKeyFilterNodeConfiguration.class);
        key = config.getKey();
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        try {
            ctx.tellNext(msg, mapper.readTree(msg.getData()).has(key) ? "True" : "False");
        } catch (IOException e) {
            ctx.tellFailure(msg, e);
        }
    }

    @Override
    public void destroy() {
    }
}
