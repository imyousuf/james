/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
 
package org.apache.james.transport;

import java.util.*;
import org.apache.arch.*;
import org.apache.avalon.blocks.*;
import org.apache.java.util.*;
import org.apache.james.*;
import org.apache.mail.*;
import org.apache.james.transport.servlet.*;
import org.apache.james.transport.match.*;
import org.apache.mail.servlet.*;

/**
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Federico Barbieri <scoobie@systemy.it>
 */
 
/*      SAMPLE CONFIGURATION
    <processor class="org.apache.james.transport.LinearProcessor">
        <servlets>
            <servlet match="RecipientIsLocal" class="LocalDelivery">
            </servlet>
            <servlet match="All" class="RemoteDelivery">
                <delayTime>21600000</delayTime>
                <maxRetries>5</maxRetries>
            </servlet>
        </servlets>
    </processor>
*/ 
public class LinearProcessor extends GenericMailServlet {

    private SimpleComponentManager comp;
    private Configuration conf;
    private Context context;
    private MatchLoader matchLoader;
    private MailetLoader mailetLoader;
    private Vector servlets;
    private Vector servletMatchs;
    private Vector servletPackages;

    private static final String OP_NOT = "!";
    private static final String OP_OR = "|";
    private static final String OP_AND = "&";

    public void init() throws Exception {
        
        conf = getConfigurations();
        context = getContext();
        comp = new SimpleComponentManager(getComponentManager());

        log("LinearProcessor init...");

        mailetLoader = (MailetLoader) comp.getComponent(Resources.MAILET_LOADER);
        matchLoader = (MatchLoader) comp.getComponent(Resources.MATCH_LOADER);

        this.servletMatchs = new Vector();
        this.servlets = new Vector();
        for (Enumeration e = conf.getConfigurations("servlets.servlet"); e.hasMoreElements(); ) {
            Configuration c = (Configuration) e.nextElement();
            String className = c.getAttribute("class");
            String match = c.getAttribute("match");
            try {
                MailServlet servlet = mailetLoader.getMailet(className, c, context, comp);
                servlets.addElement(servlet);
                servletMatchs.addElement(match);
                log("Mailet " + className + " instantiated");
            } catch (Exception ex) {
                log("Unable to init mailet " + className + ": " + ex);
                ex.printStackTrace();/*DEBUG*/
                throw ex;
            }
        }
    }

    public Mail service(Mail mail) throws Exception {

        log("Processing mail " + mail.getName());
        Vector unprocessed = new Vector(servlets.size() + 1, 2);
        unprocessed.setSize(servlets.size() + 1);
        unprocessed.insertElementAt(mail, 0);
        printPipe(unprocessed);/*DEBUG*/
        for (int i = 0; true ; i++) {
            log("===== i = " + i + " =====");
            Mail next = (Mail) unprocessed.elementAt(i);
            if (!isEmpty(next)) {
                split(unprocessed, i, (String) servletMatchs.elementAt(i));
                log("--- after split (" + i + ")---");
               printPipe(unprocessed);/*DEBUG*/
            } else {
                try {
                    do {
                        next = (Mail) unprocessed.elementAt(--i);
                    } while (isEmpty(next));
                } catch (ArrayIndexOutOfBoundsException emptyPipe) {
                    break;
                }
                MailServlet mailet = (GenericMailServlet) servlets.elementAt(i);
                try {
                    next = mailet.service(next);
                } catch (Exception ex) {
                    ex.printStackTrace();/*DEBUG*/
                    mail.setState(Mail.ERROR);
                    mail.setErrorMessage("Exception during " + mailet + " service: " + ex.getMessage());
                    log("Exception during " + mailet + " service: " + ex.getMessage());
                    return (Mail) null;
                }
                if (next == null) {
                    unprocessed.setElementAt(null, i + 1);
                } else if (isEmpty(next)) {
                    unprocessed.setElementAt(null, i + 1);
                } else {
                    unprocessed.setElementAt(next, i + 1);
                }
                unprocessed.setElementAt(null, i);
                log("--- after service (" + i + ")---");
                printPipe(unprocessed);/*DEBUG*/
            }
        }
        log("Mail " + mail.getName() + " processed");
        return (Mail) null;
    }

    private void split(Vector pipe, int i, String conditions) {

        Mail mail = (Mail) pipe.elementAt(i);
        StringTokenizer matchT = new StringTokenizer(conditions.trim(), " ");
        Vector matchingRecipients = new Vector();
// Fix Me!!! some recursive pattern needed. Now only ONE or TWO conditions allowed.
        if (matchT.countTokens() == 1) {
            matchingRecipients = singleMatch(mail, matchT.nextToken());
        } else if (matchT.countTokens() == 3) {
            matchingRecipients = doubleMatch(mail, matchT.nextToken(), matchT.nextToken(), matchT.nextToken());
        }
        if (matchingRecipients == null || matchingRecipients.isEmpty()) {
            pipe.setElementAt(null, i);
            pipe.setElementAt(mail, i + 1);
        } else {
            Vector unMatchingRecipients = VectorUtils.subtract(mail.getRecipients(), matchingRecipients);
            if (unMatchingRecipients.isEmpty()) {
                pipe.setElementAt(mail, i);
                pipe.setElementAt(null, i + 1);
            } else {
                Mail response = mail.duplicate();
                response.setRecipients(matchingRecipients);
                mail.setRecipients(unMatchingRecipients);
                pipe.setElementAt(response, i);
                pipe.setElementAt(mail, i + 1);
            }
        }
    }

    private Vector doubleMatch(Mail mail, String condition1, String operator, String condition2) {
        Vector r1 = singleMatch(mail, condition1);
        Vector r2 = singleMatch(mail, condition2);
        if (operator.equals(OP_AND)) {
            return VectorUtils.intersection(r1, r2);
        } else {
            return VectorUtils.sum(r1, r2);
        }
    }

    private Vector singleMatch(Mail mail, String conditions) {
        boolean opNot = conditions.startsWith(OP_NOT);
        if (opNot) {
            conditions = conditions.substring(1);
        }
        String matchClass = conditions;
        String param = "";
        int sep = conditions.indexOf("=");
        if (sep != -1) {
            matchClass = conditions.substring(0, sep);
            param = conditions.substring(sep + 1);
        }
        try {
            Match match = matchLoader.getMatch(matchClass);
            if (opNot) return VectorUtils.subtract(mail.getRecipients(), match.match(mail, param));
            else return match.match(mail, param);
        } catch (Exception ex) {
            log("Exception instantiating match " + matchClass + " : " + ex);
        }
        return (Vector) null;
    }

    private boolean isEmpty(Mail mail) {
        if (mail == null) return true;
        else if (mail.getRecipients().isEmpty()) return true;
        else return false;
    }

// Debuggin methods...
    private String printRecipients(Mail mail) {
        if (mail == null) return "Null ";
        Vector rec = mail.getRecipients();
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
                log("unprocessed " + j + " -> Null ");
            } else {
                log("unprocessed " + j + " -> " + printRecipients(m));
            }
        }
    }
    public String getServletInfo() {
        return "LinearProcessor";
    }
}
