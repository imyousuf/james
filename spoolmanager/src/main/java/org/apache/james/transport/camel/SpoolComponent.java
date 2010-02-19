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

package org.apache.james.transport.camel;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;
import org.apache.james.services.SpoolManager;
import org.apache.james.services.SpoolRepository;
import org.apache.mailet.MailetConfig;
import org.apache.mailet.MatcherConfig;

public class SpoolComponent extends DefaultComponent implements SpoolManager {

    private SpoolRepository spool;

    @Resource(name="spoolrepository")
    public void setSpoolRepository(SpoolRepository spool) {
        this.spool = spool;
    }
    
    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        
        return new SpoolEndPoint(uri,this, spool);
    }

    public List<MailetConfig> getMailetConfigs(String processorName) {
        // TODO Auto-generated method stub
        return null;
    }

    public List<MatcherConfig> getMatcherConfigs(String processorName) {
        // TODO Auto-generated method stub
        return null;
    }

    public String[] getProcessorNames() {
        // TODO Auto-generated method stub
        return null;
    }
}
