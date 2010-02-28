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

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.mailet.Mail;

/**
 * Set the right header on the exchange based on the State of the Mail. This is used 
 * later to route it to the right endpoint
 *
 */
public class RoutingSlipHeaderProcessor implements Processor {
    
    public static final String ROUTESLIP_HEADER = "routeslipEndpoint";
    
    /*
     * (non-Javadoc)
     * @see org.apache.camel.Processor#process(org.apache.camel.Exchange)
     */
    public void process(Exchange arg0) throws Exception {
        arg0.getIn().setHeader(ROUTESLIP_HEADER, "activemq:queue:processor." +  ((Mail) arg0.getIn().getBody()).getState());
    }
   
}
