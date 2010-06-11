/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.transport.servlet;

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
public abstract class GenericMailServlet implements MailServlet, Configurable, Composer, Contextualizable {

    private Configuration conf;
    private ComponentManager comp;
    private Context context;
    private Logger logger;

    public void setConfiguration(Configuration conf) {
        this.conf = conf;
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
    
    public void setComponentManager(ComponentManager comp) {
        this.comp = comp;
        logger = (Logger) comp.getComponent(Interfaces.LOGGER);
    }
    
    public ComponentManager getComponentManager() {
        return comp;
    }
    
    public void setContext(Context context) {
        this.context = context;
    }
    
    public Context getContext() {
        return context;
    }

    public void init() throws Exception {
    }
    
    public abstract Mail service(Mail mc);
    
    public void destroy() {
    }
    
    public void log(String msg) {
        logger.log(msg, "Mail Servlet", Logger.INFO);
    }
    
    public abstract String getServletInfo();
}

    
