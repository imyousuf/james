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

import javax.mail.MessagingException;

import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.james.transport.camel.DisposeProcessor;
import org.apache.james.transport.camel.JamesCamelConstants;

/**
 * RemoteDelivery implementation which use JMS for the outgoing spooling /
 * queue.
 * 
 * If you use ActiveMQ you should use {@link ActiveMQRemoteDelivery}
 * 
 * 
 */
public class JMSRemoteDelivery extends AbstractRemoteDelivery {

    private String outgoingRetryQueue;


    @Override
    public void init() throws MessagingException {
        super.init();

        outgoingRetryQueue = getInitParameter("outgoingRetryQueue");
        if (outgoingRetryQueue == null) {
            outgoingRetryQueue = "outgoing.retry";
        }

    }


    /**
     * RouteBuilder which builds the Camel Route for the whole RemoteDelivery
     * Process.
     * 
     * 
     * 
     */
    private final class RemoteDeliveryRouteBuilder extends RouteBuilder {
        private Processor disposeProcessor = new DisposeProcessor();

        @Override
        public void configure() throws Exception {
            
            // we need to store the message to offsite storage so use claimcheck
            from(outgoingQueueInjectorEndpoint).inOnly().beanRef("mailClaimCheck").to(getOutgoingQueueEndpoint());
            
            from(getOutgoingQueueEndpoint()).inOnly().transacted()
            .beanRef("mailEnricher")
            .process(new DeliveryProcessor()).choice().when(header(JamesCamelConstants.JAMES_RETRY_DELIVERY).isNotNull()).beanRef("mailClaimCheck").to(getOutgoingRetryQueueEndpoint()).otherwise().process(disposeProcessor).stop().end();

            fromF("pollingjms:queue?delay=30000&consumer.endpointUri=%s", getOutgoingRetryQueueEndpoint()).inOnly().transacted()
            .beanRef("mailEnricher")
            .process(new DeliveryProcessor()).choice().when(header(JamesCamelConstants.JAMES_RETRY_DELIVERY).isNotNull()).beanRef("mailClaimCheck").toF(getOutgoingRetryQueueEndpoint()).otherwise().process(disposeProcessor).stop().end();
        }

    }


    private String getOutgoingQueueEndpoint() {
        return "jms:queue:" + outgoingQueue;
    }

    
    private String getOutgoingRetryQueueEndpoint() {
        return "jms:queue:" + outgoingRetryQueue;

    }


    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RemoteDeliveryRouteBuilder();
    }

}
