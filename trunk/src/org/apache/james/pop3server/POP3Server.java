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

package org.apache.james.pop3server;

import org.apache.avalon.blocks.*;
import org.apache.avalon.*;
import org.apache.avalon.util.*;
import org.apache.java.util.*;
import org.apache.java.recycle.*;
import org.apache.java.lang.*;
import org.apache.james.*;
import java.net.*;
import java.util.Date;

public class POP3Server implements SocketHandler, Block {

    private Configuration conf;
    private Logger logger;
    private ThreadManager threadManager;
    private Store store;
    private ContextualizableContainer pop3HandlerPool;

    public POP3Server() {}

    public void init(Context context) throws Exception {

        this.conf = context.getConfiguration();
        this.logger = (Logger) context.getImplementation(Interfaces.LOGGER);
        logger.log("POP3Server init...", "POP3Server", logger.INFO);
        this.threadManager = (ThreadManager) context.getImplementation(Interfaces.THREADMANAGER);
        this.store = (Store) context.getImplementation(Interfaces.STORE);

        SimpleContext pop3HandlerContext = new SimpleContext(conf.getChild("pop3handler"));
        pop3HandlerContext.put(Interfaces.LOGGER, logger);
        pop3HandlerContext.put(Interfaces.STORE, store);
        pop3HandlerPool = new ContextualizableContainer();
        pop3HandlerContext.put("pool", pop3HandlerPool);
        String levelController = conf.getChild("pop3handlerpool").getChild("levelcontroller").getValueAsString();
        int capacity = conf.getChild("pop3handlerpool").getChild("capacity").getValueAsInt();
        RecycleBin container = new ControlledContainer(new Container(), ControllerFactory.create(levelController));
        pop3HandlerPool.init(container, capacity, new POP3Handler().getClass(), pop3HandlerContext);

        logger.log("POP3Server ...init end", "POP3Server", logger.INFO);
    }

    public void parseRequest(Socket s) {

        try {
//            logger.log("Retriving POP3Handler form pool.", "POP3Server", logger.DEBUG);
            Stoppable handler = (Stoppable) pop3HandlerPool.getRecyclable();
//            logger.log("POP3Handler = " + handler, "POP3Server", logger.DEBUG);
            ((POP3Handler) handler).parseRequest(s);
//            logger.log("Executing handler.", "POP3Server", logger.DEBUG);
            threadManager.execute(handler);
        } catch (SecurityException e) {
            logger.log("Cannot parse request on socket " + s + " : " + e.getMessage(), "POP3Server", logger.ERROR);
        }
    }

    public void destroy() {
    }

    public BlockInfo getBlockInfo() {
        // fill me
        return (BlockInfo) null;
    }
}
    
