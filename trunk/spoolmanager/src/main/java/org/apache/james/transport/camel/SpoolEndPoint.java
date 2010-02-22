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

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.commons.logging.Log;
import org.apache.james.services.SpoolRepository;

/**
 * Endpoint which handles Producer and Consumer for SpoolRepositories
 *
 */
public class SpoolEndPoint extends DefaultEndpoint{

    private SpoolRepository spool;
    private Log log;


    public SpoolEndPoint(String endpointUri, SpoolComponent component, SpoolRepository spool, Log log) {
        super(endpointUri, component);
        this.spool = spool;
        this.log = log;
    }

    
    /*
     * (non-Javadoc)
     * @see org.apache.camel.Endpoint#createProducer()
     */
    public Producer createProducer() throws Exception {
        return new SpoolProducer(this,spool,log);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.camel.IsSingleton#isSingleton()
     */
    public boolean isSingleton() {
        return true;
    }


    /*
     * (non-Javadoc)
     * @see org.apache.camel.Endpoint#createConsumer(org.apache.camel.Processor)
     */
    public Consumer createConsumer(Processor processor) throws Exception {
        return new SpoolConsumer(this,processor,spool,log);        
    }

}
