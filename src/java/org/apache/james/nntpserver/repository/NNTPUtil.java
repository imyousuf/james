/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.nntpserver.repository;

import java.io.File;
import java.io.PrintStream;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.Loggable;
import org.apache.avalon.phoenix.BlockContext;
import org.apache.james.nntpserver.NNTPException;
import org.apache.log.Logger;

/**
 * Helper fuctions. 
 * The function in this class are useful but may not have cohesion. 
 * HB: Please revisit and cleanup
 *
 * @author Harmeet Bedi <harmeet@kodemuse.com>
 */
public class NNTPUtil {
    static File getDirectory(Context context, Configuration configuration, String child)
        throws ConfigurationException
    {
        String str = configuration.getChild(child).getValue();
        if (!str.toLowerCase().startsWith("file://") ) {
            throw new ConfigurationException
                ("Malformed " + child + " - Must be of the format \"file://<filename>\".");
        }
        str = str.substring("file://".length());
        str = ((BlockContext)context).getBaseDirectory() +
                File.separator + str;
        File f = new File(str);
        if ( f.exists() && f.isFile() )
            throw new NNTPException("Expecting '"+f.getAbsolutePath()+"' directory");
        if ( f.exists() == false )
            f.mkdirs();
        return f;
    }
    public static Object createInstance(Context context, 
                                        Configuration configuration,
                                        Logger logger,
                                        String clsName) 
            throws ConfigurationException
    {
        try { clsName = configuration.getAttribute("class");
        } catch(ConfigurationException ce) { }
        try {
            Object obj = Class.forName(clsName).newInstance();
            if ( obj instanceof Loggable )
                ((Loggable)obj).setLogger( logger );
            if (obj instanceof Contextualizable) 
                ((Contextualizable)obj).contextualize(context);
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
