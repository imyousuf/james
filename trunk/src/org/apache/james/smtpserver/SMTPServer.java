/*
 * Copyright (c) 1999 The Java Apache Project.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. All advertising materials mentioning features or use of this
 *    software must display the following acknowledgment:
 *    "This product includes software and design ideas developed by the Java
 *    Apache Project (http://java.apache.org/)."
 *
 * 4. Redistributions of any form whatsoever must retain the following
 *    acknowledgment:
 *    "This product includes software and design ideas developed by the Java
 *    Apache Project (http://java.apache.org/)."
 *
 * THIS SOFTWARE IS PROVIDED BY THE JAVA APACHE PROJECT "AS IS" AND ANY
 * EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE JAVA APACHE PROJECT OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Java Apache Project. For more information
 * on the Java Apache Project please see <http://java.apache.org/>.
 */

/**
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */

package org.apache.james.smtpserver;

import org.apache.avalon.blocks.*;
import org.apache.avalon.*;
import org.apache.avalon.util.*;
import org.apache.java.util.*;
import org.apache.java.recycle.*;
import org.apache.java.lang.*;
import org.apache.james.*;
import java.net.*;
import java.util.Date;

public class SMTPServer implements SocketHandler, Block {

    private Configuration conf;
    private Logger logger;
    private ThreadManager threadManager;
    private JamesSpoolManager spoolMgr;
    private MessageSpool spool;
    private Store store;
    protected static long mid = 0;
    private ContextualizableContainer smtpHandlerPool;

    public SMTPServer() {}

    public void init(Context context) throws Exception {

        this.conf = context.getConfiguration();
        this.threadManager = (ThreadManager) context.getImplementation(Interfaces.THREADMANAGER);
        this.logger = (Logger) context.getImplementation(Interfaces.LOGGER);
        this.store = (Store) context.getImplementation(Interfaces.STORE);

        try {
            spool = new MessageSpool(logger);
            spool.init(conf.getChild("spool"));
        } catch (Exception e) {
            logger.log("Exception in Message spool init: " + e.getMessage(), "SMTPServer", logger.ERROR);
            throw e;
        }
        logger.log("Message spool instantiated.", "SMTPServer", logger.INFO);

        MessageId mi = new MessageId();

        SimpleContext spoolManagerContext = new SimpleContext(conf.getChild("spoolmanager"));
        spoolManagerContext.put(Interfaces.LOGGER, logger);
        spoolManagerContext.put(Interfaces.STORE, store);
        spoolManagerContext.put("spool", spool);
        spoolManagerContext.put("messageid", mi );

        int threads = 1;
        try {
            threads = conf.getChild("spoolmanagerthreads").getValueAsInt();
        } catch (Exception e) {
            logger.log("Unknown spoolManagerthreads. Using default(" + threads + "): " + e.getMessage(), "SMTPServer", logger.WARNING);
        }

        while (threads-- > 0) {
            try {
                spoolMgr = new JamesSpoolManager();
                spoolMgr.init(spoolManagerContext);
                threadManager.execute(spoolMgr);
            } catch (Exception e) {
                logger.log("Exception in SpoolManager thread-" + threads + " init: " + e.getMessage(), "SMTPServer", logger.ERROR);
                throw e;
            }
            logger.log("SpoolManager thread-" + (threads + 1) + " started", "SMTPServer", logger.INFO);
        }

        smtpHandlerPool = new ContextualizableContainer();

        SimpleContext smtpHandlerContext = new SimpleContext(conf.getChild("smtphandler"));
        smtpHandlerContext.put(Interfaces.LOGGER, logger);
        smtpHandlerContext.put("spool", spool);
        smtpHandlerContext.put("pool", smtpHandlerPool);
        smtpHandlerContext.put("messageid", mi );

        String levelController = conf.getChild("smtphandlerpool").getChild("levelcontroller").getValueAsString();
        int capacity = conf.getChild("smtphandlerpool").getChild("capacity").getValueAsInt();
        RecycleBin container = new ControlledContainer(new Container(), ControllerFactory.create(levelController));
        smtpHandlerPool.init(container, capacity, new SMTPHandler().getClass(), smtpHandlerContext);
                
    }

    public void parseRequest(Socket s) {

        try {
            Stoppable handler = (Stoppable) smtpHandlerPool.getRecyclable();
logger.log("parsereq");
            ((SMTPHandler) handler).parseRequest(s);
logger.log("parsereq2");
            threadManager.execute(handler);
        } catch (SecurityException e) {
            logger.log("Cannot parse request on socket " + s + " : " + e.getMessage(), "SMTPServer", logger.ERROR);
        }
    }

    public void destroy() {
    }

    public BlockInfo getBlockInfo() {
        // fill me
        return (BlockInfo) null;
    }

    public class MessageId {
        
        private String serverName;
        private int messageCount;

        public MessageId() {
            messageCount = 0;
            try {
                serverName = conf.getChild("smtphandler").getChild("servername").getValueAsString();
            } catch (ConfigurationException ce) {
                serverName = "SERVERNAME-NOTFOUND";
            } catch (Exception e) {
                serverName = "SERVERNAME-EXCEPTION";
            }
        }

        public synchronized String getNew() {
            messageCount++;
            return new String(new Date().getTime() + "." + new Integer(messageCount) + "@" + serverName );
        }
    }

}
    
