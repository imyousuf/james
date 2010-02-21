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
import org.apache.camel.impl.DefaultConsumer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.commons.logging.Log;
import org.apache.james.services.SpoolRepository;
import org.apache.mailet.Mail;

public class SpoolConsumer extends DefaultConsumer{


    private SpoolRepository spool;
    private Processor processor;
    private Log log;
    
    public SpoolConsumer(DefaultEndpoint endpoint, Processor processor, SpoolRepository spool, Log log) {
        super(endpoint, processor);
        this.spool = spool;
        this.processor = processor;
        this.log = log;
    }


    public Exchange receive() {
        Exchange ex = getEndpoint().createExchange();        
        Mail mail;
        try {
            mail = spool.accept();
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
