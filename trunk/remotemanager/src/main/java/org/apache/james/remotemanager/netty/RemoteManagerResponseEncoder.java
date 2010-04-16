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

package org.apache.james.remotemanager.netty;

import java.util.ArrayList;
import java.util.List;

import org.apache.james.remotemanager.RemoteManagerResponse;
import org.apache.james.socket.netty.AbstractResponseEncoder;

public class RemoteManagerResponseEncoder extends AbstractResponseEncoder<RemoteManagerResponse>{

    public RemoteManagerResponseEncoder() {
        super(RemoteManagerResponse.class, "UTF-8");
    }

    @Override
    protected List<String> getResponse(RemoteManagerResponse response) {
        List<String> responseList = new ArrayList<String>();
        for (int k = 0; k < response.getLines().size(); k++) {
            responseList.add(response.getLines().get(k).toString());
        }
        return responseList;
        
    }

}
