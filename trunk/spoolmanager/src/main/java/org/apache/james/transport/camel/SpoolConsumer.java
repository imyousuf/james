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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultConsumer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.commons.logging.Log;
import org.apache.james.services.SpoolRepository;
import org.apache.mailet.Mail;

/**
 * Consumer implementation which consum Mail objects of the SpoolRepository
 * 
 *
 */
public class SpoolConsumer extends DefaultConsumer{


    private SpoolRepository spool;
    private Processor processor;
    private Log log;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future<?> f;
    
    public SpoolConsumer(DefaultEndpoint endpoint, Processor processor, SpoolRepository spool, Log log) {
        super(endpoint, processor);
        this.spool = spool;
        this.processor = processor;
        this.log = log;
    }


    /**
     * Receive a Mail object of the SpoolRepository when one is ready. This method will block until a Mail object is ready to process
     * 
     * @return exchange exchange which holds the MailMessage build up on the Mail
     */
    private Exchange receive() {
        Exchange ex = getEndpoint().createExchange();        
        Mail mail;
        try {
            mail = spool.accept();
            ex.setIn(new MailMessage(mail));
            log.debug("Mail " + mail.toString() + " ready for process. Start to process exchange " +ex);
            processor.process(ex);
        } catch (Exception e) {
            ex.setException(e);
        }
        return ex;

    }


    @Override
    protected void doStart() throws Exception {
        super.doStart();
        f  = executor.submit(new Runnable() {

            public void run() {
                while(true) {
                    receive();
                }
            }
            
        });
    }


    @Override
    protected void doStop() throws Exception {
        super.doStop();
        try {
            f.cancel(true);
        } catch (Exception e) {
            // ignore
        }
    }
    
}
