/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
package org.apache.james.transport;

import java.io.*;
import java.net.*;
import java.util.*;
import org.apache.arch.*;
import org.apache.avalon.blocks.*;
import org.apache.java.util.*;
import org.apache.james.*;
import org.apache.mail.*;
import org.apache.james.transport.servlet.*;
import org.apache.james.transport.match.*;

/**
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Federico Barbieri <scoobie@systemy.it>
 */ 
public class JamesSpoolManager implements Component, Composer, Configurable, Stoppable, Service, Contextualizable {

    private SimpleComponentManager comp;
    private Configuration conf;
    private Context context;
    private MailRepository spool;
    private Logger logger;
    private Vector servlets;
    private Vector servletMatchs;
    private String servletsRootPath;
    
    private static final String OP_NOT = "!";
    private static final String OP_OR = "|";
    private static final String OP_AND = "&";

    /**
     * SpoolManager constructor comment.
     */
    public JamesSpoolManager() {
    }

    public void setConfiguration(Configuration conf) {
        this.conf = conf;
    }
    
    public void setContext(Context context) {
        this.context = context;
    }

    public void setComponentManager(ComponentManager comp) {
        this.comp = new SimpleComponentManager(comp);
    }

	public void init() throws Exception {

        this.logger = (Logger) comp.getComponent(Interfaces.LOGGER);
        logger.log("JamesSpoolManager init...", "JAMES", logger.INFO);
        this.spool = (MailRepository) comp.getComponent(Constants.SPOOL_REPOSITORY);

		StringBuffer servers = new StringBuffer ();
		for (Enumeration e = conf.getConfigurations("DNSservers.server") ; e.hasMoreElements(); ) {
			Configuration c = (Configuration)e.nextElement ();
			servers.append (c.getValue () + " ");
		}
		boolean authoritative = conf.getConfiguration("authoritative", "false").getValueAsBoolean();
		SmartTransport transport = new SmartTransport (servers.toString(), authoritative);
		comp.put(Resources.TRANSPORT, transport);

		String delayedRepository = conf.getConfiguration("repository", "file://../tmp/").getValue();
		Store store = (Store) comp.getComponent(Interfaces.STORE);
		MailRepository delayed = (MailRepository) store.getPrivateRepository(delayedRepository, MailRepository.MAIL, Store.ASYNCHRONOUS);
		comp.put(Resources.TMP_REPOSITORY, delayed);
		
        this.servletMatchs = new Vector();
        this.servlets = new Vector();
        servletsRootPath = conf.getConfiguration("servlets").getAttribute("rootpath");
        for (Enumeration e = conf.getConfigurations("servlets.servlet"); e.hasMoreElements(); ) {
            Configuration c = (Configuration) e.nextElement();
            String className = servletsRootPath + c.getAttribute("class");
            String match = c.getAttribute("match");
            try {
                GenericMailServlet servlet = (GenericMailServlet) Class.forName(className).newInstance();
                servlet.setConfiguration(c);
                servlet.setContext(context);
                servlet.setComponentManager(comp);
                servlet.init();
                servlets.addElement(servlet);
                servletMatchs.addElement(match);
            } catch (Exception ex) {
                logger.log("Unable to init mail servlet " + className + ": " + ex, "JAMES", logger.INFO);
                ex.printStackTrace();
            }
        }
    }

    /**
     * This routinely checks the message spool for messages, and processes them as necessary
     */
    public void run() {

        logger.log("run JamesSpoolManager", "JAMES", logger.INFO);

        Vector unprocessed = new Vector(servlets.size() + 1, 2);
        unprocessed.setSize(servlets.size() + 1);
        GenericMailServlet errorServlet = (GenericMailServlet) servlets.elementAt(servlets.size() - 1);
        Stack errors = new Stack();
        while(true) {

            try {
                String key = spool.accept();
                Mail mc = spool.retrieve(key);
                logger.log("==== Begin processing mail " + key + " ====", "JAMES", logger.INFO);
                unprocessed.insertElementAt(mc, 0);
// ---- Reactor begin ----
/*DEBUG*/       printPipe(unprocessed);
                for (int i = 0; true ; i++) {
                    logger.log("===== i = " + i + " =====", "JAMES", logger.DEBUG);
                    Mail next = (Mail) unprocessed.elementAt(i);
                    if (!isEmpty(next)) {
                        split(unprocessed, i, (String) servletMatchs.elementAt(i));
                        logger.log("--- after split (" + i + ")---", "JAMES", logger.DEBUG);
/*DEBUG*/               printPipe(unprocessed);
                    } else {
                        try {
                            do {
                                next = (Mail) unprocessed.elementAt(--i);
                            } while (isEmpty(next));
                        } catch (ArrayIndexOutOfBoundsException emptyPipe) {
                            break;
                        }
                        GenericMailServlet servlet = (GenericMailServlet) servlets.elementAt(i);
                        Mail response = null;
                        try {
                            response = servlet.service(next);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            response = next;
                            response.setState(Mail.ERROR);
                            response.setErrorMessage("Exception during mail servlet service: " + ex.getMessage());
                            logger.log("Exception during mail servlet service: " + ex.getMessage(), "JAMES", logger.ERROR);
                        }
                        if (response == null) {
                            unprocessed.setElementAt(null, i + 1);
                        } else if (response.getState() == Mail.ERROR) {
                            try {
                                errorServlet.service(response);
                            } catch (Exception ex) {
                                logger.log("Exception trying to store Mail " + response + " in error repository... deleting it", "JAMES", logger.ERROR);
                            }
                            unprocessed.setElementAt(null, i + 1);
                        } else if (isEmpty(response)) {
                            unprocessed.setElementAt(null, i + 1);
                        } else {
                            unprocessed.setElementAt(response, i + 1);
                        }
                        unprocessed.setElementAt(null, i);
                        logger.log("--- after service (" + i + ")---", "JAMES", logger.DEBUG);
/*DEBUG*/               printPipe(unprocessed);
                    }
                }
// ---- Reactor end ----                
                spool.remove(key);
                logger.log("==== Removed from spool mail " + mc.getName() + " ====", "JAMES", logger.INFO);
            } catch (Exception e) {
                logger.log("Exception in JamesSpoolManager.run " + e.getMessage(), "JAMES", logger.ERROR);
                e.printStackTrace();
            }
        }
    }

    private void split(Vector pipe, int i, String conditions) {

        Mail mc = (Mail) pipe.elementAt(i);
        StringTokenizer matchT = new StringTokenizer(conditions.trim(), " ");
        Vector matchingRecipients = new Vector();
// Fix Me!!! some recursive pattern needed. Now only ONE or TWO conditions allowed.
        if (matchT.countTokens() == 1) {
            matchingRecipients = singleMatch(mc, matchT.nextToken());
        } else if (matchT.countTokens() == 3) {
            matchingRecipients = doubleMatch(mc, matchT.nextToken(), matchT.nextToken(), matchT.nextToken());
        }
        if (matchingRecipients == null || matchingRecipients.isEmpty()) {
            pipe.setElementAt(null, i);
            pipe.setElementAt(mc, i + 1);
        } else {
            Vector unMatchingRecipients = VectorUtils.subtract(mc.getRecipients(), matchingRecipients);
            if (unMatchingRecipients.isEmpty()) {
                pipe.setElementAt(mc, i);
                pipe.setElementAt(null, i + 1);
            } else {
                Mail response = mc.duplicate();
                response.setRecipients(matchingRecipients);
                mc.setRecipients(unMatchingRecipients);
                pipe.setElementAt(response, i);
                pipe.setElementAt(mc, i + 1);
            }
        }
    }
    
    private Vector doubleMatch(Mail mc, String condition1, String operator, String condition2) {
        Vector r1 = singleMatch(mc, condition1);
        Vector r2 = singleMatch(mc, condition2);
        if (operator.equals(OP_AND)) {
            return VectorUtils.intersection(r1, r2);
        } else {
            return VectorUtils.sum(r1, r2);
        }
    }
    
    private Vector singleMatch(Mail mc, String conditions) {
        boolean opNot = conditions.startsWith(OP_NOT);
        if (opNot) {
            conditions = conditions.substring(1);
        }
        String matchClass = "org.apache.james.transport.match." + conditions;
        String param = "";
        int sep = conditions.indexOf("=");
        if (sep != -1) {
            matchClass = "org.apache.james.james.match." + conditions.substring(0, sep);
            param = conditions.substring(sep + 1);
        }
        Match match = (Match) null;
        try {
            match = (Match) comp.getComponent(matchClass);
        } catch (ComponentNotFoundException cnfe) {
            try {
                match = (Match) Class.forName(matchClass).newInstance();
                match.setContext(context);
                match.setComponentManager(comp);
                comp.put(matchClass, match);
            } catch (Exception ex) {
                logger.log("Exception instantiationg match " + matchClass + " : " + ex, "JAMES", logger.ERROR);
                return (Vector) null;
            }
        }
        if (opNot) return VectorUtils.subtract(mc.getRecipients(), match.match(mc, param));
        else return match.match(mc, param);
    }
    
    private boolean isEmpty(Mail mc) {
        if (mc == null) return true;
        else if (mc.getRecipients().isEmpty()) return true;
        else return false;
    }
    
    public void stop() {
    }
    
    public void destroy()
    throws Exception {
    }

// Debuggin methods...
    private String printRecipients(Mail mc) {
        if (mc == null) return "Null ";
        Vector rec = mc.getRecipients();
        StringBuffer buffer = new StringBuffer("Recipients: ");
        boolean empty = true;
        for (Enumeration e = rec.elements(); e.hasMoreElements(); ) {
            buffer.append((String) e.nextElement() + " ");
            empty = false;
        }
        if (empty) return "Empty";
        else return buffer.toString();
    }
    
    private void printPipe(Vector unprocessed) {
        for (int j = 0; j < unprocessed.size(); j++) {
            Mail m = (Mail) unprocessed.elementAt(j);
            if (m == null) {
                logger.log("unprocessed " + j + " -> Null ", "JAMES", logger.DEBUG);
            } else {
                logger.log("unprocessed " + j + " -> " + printRecipients(m), "JAMES", logger.DEBUG);
            }
        }
    }
}
