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
 
 
package org.apache.avalon.acceptors.serversocket;

import java.net.*;
import org.apache.avalon.*;
import org.apache.avalon.blocks.*;
import org.apache.avalon.configuration.*;

/**
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */

/**
 * This is an implementation example of a socket acceptor. A socket acceptor
 * waits on a defined (in its confs) socket for request. On a request it calls
 * the method parseRequest(Socket s) in the SocketHandler interface. The 
 * specific implementation of the SocketHandler is defined again in confs.
 * Definitivly this class listen on the specific port and then call the 
 * specific handler to parse the request on that socket. You must start an 
 * acceptor for each port or other "request generator" you want to listen to.
 */

public class ServerSocketAcceptor implements Acceptor { // Acceptor extends Block, Reconfigurable, Work

	private String socketHandler;
	private ServerSocket ss;
	private boolean running;
	private int port;
	private SocketHandler handler;
	private BlockManager blockManager;
    private Configuration conf;
    private Logger logger;

	public ServerSocketAcceptor() {
	}

	public void initBlock(BlockManager blockManager)
	throws Exception {

        this.conf = blockManager.getConfigurations();
  	    this.blockManager = blockManager;

        logger = (Logger) blockManager.getBlock("org.apache.avalon.blocks.Logger");
            
   		socketHandler = conf.getAttribute("handler");
   		port = conf.getChild("port").getValueAsInt();
   		handler = (SocketHandler) blockManager.getBlock(socketHandler);
   		ss = new ServerSocket(port);
	}

    public void destroyBlock() {

        try {
            ss.close();
        } catch (Exception e) {
            logger.log(this + ".destroy: " + e.getMessage());
        }
    }

    public BlockInfo getBlockInfo() {

        return (BlockInfo) null;
    }

    /**
     * This is the actual work of an acceptor.
     * In particular a ServerSocketAcceptor will accept() on the specified port.
     */
	public void run() {

  	    this.running = true;
        logger.log(this + ".run start", "system", logger.INFO);
        try {
    		while (running) {
	    		handler.parseRequest(ss.accept());
		    }
        } catch (Exception e) {
        }
	}

    /**
     * This is called by the blockManager to reconfigure on fly the acceptor.
     */
	public void reconfigure(Configuration conf)
    throws Exception {

        this.conf = conf;
  	    blockManager = (BlockManager) conf.getChild("blockManager").getValue();
        logger = (Logger) blockManager.getBlock("org.apache.avalon.blocks.Logger");
		if (port != conf.getChild("port").getValueAsInt()) {
			throw new Exception("Unable to change port");
		}
		String newSocketHandler = conf.getChild("handler").getValueAsString();

		if (!socketHandler.equals(newSocketHandler)) {
			socketHandler = newSocketHandler;
			handler = (SocketHandler) blockManager.getBlock(socketHandler);
		}
        logger.log(this + ".reconfigure reconfigured", "system", logger.INFO);

	}

	public void stop() {

        try {
            running = false;
//		    ss.close();         // ? should we close the socket on stop ?
        } catch (Exception i) {
        }
        logger.log(this + ".stop stopped", "system", logger.INFO);
	}
}