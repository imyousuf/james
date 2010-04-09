/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/
package org.apache.james.pop3server.netty;

import java.util.ArrayList;
import java.util.List;

import org.apache.james.pop3server.POP3Response;
import org.apache.james.socket.netty.AbstractResponseEncoder;

/**
 * {@link AbstractResponseEncoder} implementation which handles {@link POP3Response} messages
 *
 */
public class POP3ResponseEncoder extends AbstractResponseEncoder<POP3Response>{

    public POP3ResponseEncoder() {
        super(POP3Response.class, "US-ASCII");
    }

    @Override
    protected List<String> getResponse(POP3Response response) {
        List<String> responseList = new ArrayList<String>();
        for (int k = 0; k < response.getLines().size(); k++) {
            StringBuffer respBuff = new StringBuffer(256);
            if (k == 0) {
                respBuff.append(response.getRetCode());
                respBuff.append(" ");
                respBuff.append(response.getLines().get(k));

            } else {
                respBuff.append(response.getLines().get(k));
            }
            responseList.add(respBuff.toString());
        }
        return responseList;
    }

}
