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
package org.apache.james.transport.mailets;

import org.apache.activemq.ScheduledMessage;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.james.transport.camel.DisposeProcessor;
import org.apache.james.transport.camel.JamesCamelConstants;

/**
 * RemoteDelivery implementation which use ActiveMQ for the outgoing spooling /
 * queue
 * 
 * 
 */
public class ActiveMQRemoteDelivery extends AbstractRemoteDelivery {

    /**
     * RouteBuilder which builds the Camel Route for the whole RemoteDelivery
     * Process.
     * 
     * 
     * 
     */
    private final class RemoteDeliveryRouteBuilder extends RouteBuilder {
        private final Processor disposeProcessor = new DisposeProcessor();
        private final Processor headerProcessor = new ActiveMQHeaderProcessor();
        @Override
        public void configure() throws Exception {
            
            // we need to store the message to offsite storage so use claimcheck
            from(outgoingQueueInjectorEndpoint).inOnly().beanRef("mailClaimCheck").to(getOutgoingQueueEndpoint());
            
            from(getOutgoingQueueEndpoint()).inOnly().transacted()
            .beanRef("mailEnricher")
            .process(new DeliveryProcessor()).choice().when(header(JamesCamelConstants.JAMES_RETRY_DELIVERY).isNotNull()).process(headerProcessor).beanRef("mailClaimCheck").to(getOutgoingQueueEndpoint()).otherwise().process(disposeProcessor).stop().end();
        }

    }
    
    private final class ActiveMQHeaderProcessor implements Processor {

        public void process(Exchange exchange) throws Exception {
            Message in = exchange.getIn();
            long nextDeliver = (Long) in.getHeader(JamesCamelConstants.JAMES_NEXT_DELIVERY);
            long delay = nextDeliver - System.currentTimeMillis();
            if (delay < 0) {
                delay = 0;
            }
            in.setHeader(ScheduledMessage.AMQ_SCHEDULED_DELAY, delay);
            
        }
        
    }

    private String getOutgoingQueueEndpoint() {
        return "activemq:queue:" + outgoingQueue;
    }


    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RemoteDeliveryRouteBuilder();
    }

}
