/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.james.servlet;

import java.util.*;
import org.apache.arch.*;
import org.apache.mail.*;
import org.apache.mail.servlet.*;
import org.apache.avalon.blocks.*;

/**
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri   <scoobie@pop.systemy.it>
 * @author  Stefano Mazzocchi   <stefano@apache.org>
 * @author  Pierpaolo Fumagalli <pier@apache.org>
 * @author  Serge Knystautas    <sergek@lokitech.com>
 */
public abstract class GenericMailServlet implements MailServlet, Configurable, Composer {

    private Configuration conf;
    private ComponentManager comp;
    private Logger logger;
    private MailServletContext context;

    public void setConfiguration(Configuration conf) {
        this.conf = conf;
    }
    
    public void setComponentManager(ComponentManager comp) {
        this.comp = comp;
        this.logger = (Logger) comp.getComponent(Interfaces.LOGGER);
// This is commented 'couse I don't need it right now and it was annong to me 
// for some strange reason I didn't want to care of.
//        this.context = (MailServletContext) comp.getComponent(MailServletContext.CONTEXT);
    }
    
    public Configuration getConfiguration(String name) {
        return conf.getConfiguration(name);
    }
    
    public Configuration getConfiguration(String name, String defaultValue) {
        return conf.getConfiguration(name, defaultValue);
    }
    
    public Enumeration getConfigurations(String name) {
        return conf.getConfigurations(name);
    }
    
    public MailServletContext getContext() {
        return context;
    }

    public abstract void init() throws Exception;
    
    public abstract MessageContainer service(MessageContainer mc);
    
    public abstract void destroy();
    
    public void log(String msg) {
        logger.log(msg, "Mail Servlet", Logger.INFO);
    }
    
    public String getServletInfo() {
        return "Generic Mail Servlet";
    }
}

    
