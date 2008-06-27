/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.nntpserver.repository;

import java.io.*;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.Loggable;
import org.apache.james.nntpserver.NNTPException;
import org.apache.log.Logger;

// processes entries and sends to appropriate groups.
// eats up inappropriate entries.
public class NNTPUtil {
    static File getDirectory(Configuration configuration,String child)
        throws ConfigurationException
    {
        String str = configuration.getChild(child).getValue();
        if ( str.toLowerCase().startsWith("file://") )
            str = str.substring("file://".length());
        File f = new File(str);
        if ( f.exists() && f.isFile() )
            throw new NNTPException("Expecting '"+f.getAbsolutePath()+"' directory");
        if ( f.exists() == false )
            f.mkdirs();
        return f;
    }
    public static Object createInstance(Configuration configuration,Logger logger,
                                        String clsName) throws ConfigurationException
    {
        try { clsName = configuration.getAttribute("class");
        } catch(ConfigurationException ce) { }
        try {
            Object obj = Class.forName(clsName).newInstance();
            if ( obj instanceof Loggable )
                ((Loggable)obj).setLogger( logger );
            if ( obj instanceof Configurable )
                ((Configurable)obj).configure(configuration.getChild("configuration"));
            return obj;
        } catch(Exception ex) {
            ex.printStackTrace();
            throw new ConfigurationException("spooler initialization failed",ex);
        }
    }

    public static void show(Configuration conf,PrintStream prt) {
        prt.println("conf.getClass="+conf.getClass().getName());
        prt.println("name="+conf.getName());
        Configuration[] children = conf.getChildren();
        for ( int i = 0 ; i < children.length ; i++ )
            prt.println(i+". "+children[i].getName());
    }
}
