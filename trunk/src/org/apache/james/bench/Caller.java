/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.bench;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.arch.*;
import org.apache.avalon.*;
import org.apache.avalon.blocks.*;
import org.apache.mail.Mail;

import javax.mail.internet.*;
import javax.mail.*;

/**
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class Caller implements Runnable {

    private String target;
    private static long count;
    private String name;
    private Logger logger;

	public Caller(String name, Logger logger, String target) {
	    
	    try {
    	    this.target = target;
    	    this.logger = logger;
    	    this.name = name;
            logger.log("Caller " + name + " ready", "Test", logger.INFO);
        } catch (Exception e) {
            logger.log("Exception in caller init: " + e, "Test", logger.ERROR);
        }
    }

    public void run() {
        logger.log("Caller " + name + " started", "Test", logger.INFO);
        try {
            Socket s = new Socket(target, 25);
            PrintStream out = new PrintStream(s.getOutputStream());
            int count = 1000;
            while (count-- > 0) {
                out.println("mail from:scoobie@betaversion.org");
                out.println("rcpt to:scoobie@maggie");
                out.println("data");
                out.println("Message-ID: <000701bfb8c9$0f572590$6601a8c0@maggie" + count + ">");
                out.println("From: Scoobie <scoobie@maggie>");
                out.println("To: scoobie <scoobie@maggie>");
                out.println("Subject: fxcgxc" + count);
                out.println("Date: Mon, 8 May 2000 01:40:27 -0700");
                out.println("MIME-Version: 1.0");
                out.println("");
                out.println("xgxcgxcfgdfg");
                out.println("xgxcgxcfgdfg");
                out.println("xgxcgxcfgdfg");
                out.println(".");
                out.println(".");
                logger.log("mail test-" + name + "@" + count + " SENT", "Test", logger.INFO);
        	}
        } catch (Exception e) {
            logger.log("Exception from " + name + ": " + e.getMessage(), "Test", logger.ERROR);
        }
    }
}