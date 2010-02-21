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

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;
import org.apache.commons.logging.Log;
import org.apache.james.lifecycle.LifecycleUtil;
import org.apache.james.services.SpoolRepository;
import org.apache.mailet.Mail;

public class SpoolProducer extends DefaultProducer {

    private SpoolRepository spool;
    private Log log;

    public SpoolProducer(Endpoint endpoint, SpoolRepository spool, Log log) {
        super(endpoint);
        this.spool = spool;
        this.log = log;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.camel.Processor#process(org.apache.camel.Exchange)
     */
    public void process(Exchange exchange) throws Exception {
        // Exchange newExchange = getEndpoint().createExchange(exchange);
        Mail mail = (Mail) exchange.getIn().getBody();

        // Only remove an email from the spool is processing is
        // complete, or if it has no recipients
        if ((Mail.GHOST.equals(mail.getState())) || (mail.getRecipients() == null) || (mail.getRecipients().size() == 0)) {
            spool.remove(mail.getName());
            if (log.isDebugEnabled()) {
                StringBuffer debugBuffer = new StringBuffer(64).append("==== Removed from spool mail ").append(mail.getName()).append("====");
                log.debug(debugBuffer.toString());
            }
            LifecycleUtil.dispose(mail);

        } else {

            try {
                spool.store(mail);
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }
    }

}
