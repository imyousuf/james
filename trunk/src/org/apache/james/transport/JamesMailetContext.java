/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.transport;

import org.apache.java.lang.*;
import org.apache.mail.*;

/**
 * Draft of a MailServlet inteface.
 *
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri   <scoobie@pop.systemy.it>
 * @author  Stefano Mazzocchi   <stefano@apache.org>
 * @author  Pierpaolo Fumagalli <pier@apache.org>
 * @author  Serge Knystautas    <sergek@lokitech.com>
 */
public class JamesMailetContext extends SimpleContext implements MailetContext, Configurable, Composer {
    
    private Configuration conf;
    private ComponentManager comp;

    protected JamesMailetContext(Context parent) {
        super(parent);
    }
    
    protected JamesMailetContext(MailetContext parent) {
        super(parent);
        setConfiguration(parent.getConfiguration());
        setComponentManager(parent.getComponentManager());
    }

    protected JamesMailetContext() {
        super();
    }
    
    public void setComponentManager(ComponentManager comp) {
        this.comp = comp;
    }
    
    public ComponentManager getComponentManager() {
        return comp;
    }
    
    public void setConfiguration(Configuration conf) {
        this.conf = conf;
    }
    
    public Configuration getConfiguration() {
        return conf;
    }
    
    public MailetContext getChildContext(Configuration conf) {
        JamesMailetContext child = new JamesMailetContext(this);
        child.setComponentManager(comp);
        child.setConfiguration(conf);
        return child;
    }

    public MailetContext getChildContext(String childName) {
        return getChildContext(conf.getConfiguration(childName));
    }
    
    // Fill Me!!!
}

    
