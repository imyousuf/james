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
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultConsumer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.james.lifecycle.LifecycleUtil;
import org.apache.james.services.SpoolRepository;
import org.apache.mailet.Mail;

public class SpoolConsumer extends DefaultConsumer{


    private SpoolRepository spool;
    private Processor processor;
    
    public SpoolConsumer(DefaultEndpoint endpoint, Processor processor, SpoolRepository spool) {
        super(endpoint, processor);
        this.spool = spool;
        this.processor = processor;
    }


    public Exchange receive() {
        Exchange ex = getEndpoint().createExchange();        
        Mail mail;
        try {
            mail = spool.accept();

            // Only remove an email from the spool is processing is
            // complete, or if it has no recipients
            if ((Mail.GHOST.equals(mail.getState())) ||
                (mail.getRecipients() == null) ||
                (mail.getRecipients().size() == 0)) {
                spool.remove(mail.getName());
                //if (logger.isDebugEnabled()) {
                    StringBuffer debugBuffer =
                        new StringBuffer(64)
                                .append("==== Removed from spool mail ")
                                .append(mail.getName())
                                .append("====");
                 //   logger.debug(debugBuffer.toString());
               // }
                    System.out.println(debugBuffer.toString());
                    LifecycleUtil.dispose(mail);

            }
            
            ex.setIn(new MailMessage(mail));
            processor.process(ex);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ex.setException(e);
            
        }
        return ex;

    }


    @Override
    protected void doStart() throws Exception {
        super.doStart();
        Thread t = new Thread(new Runnable() {

            public void run() {
                while(true) {
                    receive();
                }
            }
            
        });
        t.setDaemon(true);
        t.start();
    }


    @Override
    protected void doStop() throws Exception {
        super.doStop();
        
    }
    
}
