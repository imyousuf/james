/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.transport;

import java.util.*;
import org.apache.java.lang.*;
import org.apache.james.*;
import org.apache.avalon.interfaces.*;
import org.apache.mail.*;
import org.apache.java.util.*;
import org.apache.james.transport.match.*;
import org.apache.james.transport.servlet.*;
import org.apache.mail.servlet.*;

/**
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
 
/*      SAMPLE CONFIGURATION

     <processor class="org.apache.james.transport.TreeProcessor">
        <node type="match" condition="All">
            <node type="match" condition="RecipientIsLocal">
                <node type="mailet" name="LocalDelivery"/>
            </node>
            <node type="mailet" name="RemoteDelivery"/>
        </node>
        
        <mailets>
            <mailet name="Null" class="org.apache.james.transport.servlet.Null">
            </mailet>
            <mailet name="LocalDelivery" class="org.apache.james.transport.servlet.LocalDelivery">
            </mailet>
            <mailet name="rootForward" class="org.apache.james.transport.servlet.Forward">
                <forwardTo> scoobie@localhost </forwardTo>
            </mailet>
            <mailet name="RemoteDelivery" class="org.apache.james.transport.servlet.RemoteDelivery">
                <delayTime>21600000</delayTime>
                <maxRetries>5</maxRetries>
            </mailet>
        </mailets>
    </processor>
*/
public class TreeProcessor extends GenericMailServlet {

    private Pipe rootPipe;

    public void init() throws Exception {
        SimpleContext context = new SimpleContext(getContext());
        ComponentManager comp = getComponentManager();
        MailetLoader loader = (MailetLoader) comp.getComponent(Resources.MAILET_LOADER);
        for (Enumeration e = getConfigurations("mailets.mailet"); e.hasMoreElements(); ) {
            Configuration c = (Configuration) e.nextElement();
            String name = c.getAttribute("name");
            String className = c.getAttribute("class");
            try {
                MailServlet ms = loader.getMailet(className, c, context, comp);
                context.put(name, ms);
                log("Mailet " + className + " initializated");
            } catch (Exception ex) {
                log("Unable to init mailet " + className + ": " + ex);
                ex.printStackTrace();
                throw ex;
            }
        }
        rootPipe = new Pipe();
        rootPipe.setConfiguration(getConfiguration("node"));
        rootPipe.setContext(context);
        rootPipe.setComponentManager(getComponentManager());
        rootPipe.init();
    }
    
    public Mail service(Mail mail) throws Exception {
        rootPipe.service(mail);
        return (Mail) null;
    }

    public String getServletInfo() {
        return "TreeProcessor";
    }
    
    public class Pipe extends GenericMailServlet {
    
        private Context context;
        private Configuration conf;
        private Match match;
        private String param;
        private Vector pipe;
    
        public void init() throws Exception {
            conf = getConfigurations();
            context = getContext();
            ComponentManager comp = getComponentManager();
            MatchLoader matchLoader = (MatchLoader) comp.getComponent(Resources.MATCH_LOADER);
            MailetLoader mailetLoader = (MailetLoader) comp.getComponent(Resources.MAILET_LOADER);
            String matchName = conf.getAttribute("condition");
            param = (String) null;
            int hasParam = matchName.indexOf('=');
            if (hasParam != -1) {
                param = matchName.substring(hasParam + 1);
                matchName = matchName.substring(0, hasParam);
            }   
            pipe = new Vector();
            match = (Match) matchLoader.getMatch(matchName);
            for (Enumeration e = conf.getConfigurations("node"); e.hasMoreElements(); ) {
                Configuration c = (Configuration) e.nextElement();
                String type = c.getAttribute("type");
                if (type.equals("match")) {
                    Pipe subPipe = new Pipe();
                    subPipe.setConfiguration(c);
                    subPipe.setContext(context);
                    subPipe.setComponentManager(comp);
                    subPipe.init();
                    pipe.addElement(subPipe);
                } else if (type.equals("mailet")) {
                    String name = c.getAttribute("name");
                    pipe.addElement(context.get(name));
                }
            }
        }
        
        public Mail service(Mail mail) throws Exception {
            Vector matching = match.match(mail, param);
            if (matching == null || matching.isEmpty()) return mail;
            Vector notMatching = VectorUtils.subtract(mail.getRecipients(), matching);
            Mail toBeParsed;
            Mail response;
            if (notMatching.isEmpty()) {
                toBeParsed = mail;
                response = (Mail) null;
            } else {
                toBeParsed = mail.duplicate();
                response = mail;
                response.setRecipients(notMatching);
                toBeParsed.setRecipients(matching);
            }
            for (Enumeration e = pipe.elements(); e.hasMoreElements(); ) {
                MailServlet ms = (MailServlet) e.nextElement();
                if (toBeParsed != null) {
                    toBeParsed = ms.service(toBeParsed);
                }
            }
            return response;
        }
    
        public String getServletInfo() {
            return "TreeProcessor Node";
        }
    }
}
    
