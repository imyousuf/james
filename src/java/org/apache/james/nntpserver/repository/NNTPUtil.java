/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.nntpserver.repository;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.phoenix.BlockContext;
import org.apache.james.nntpserver.NNTPException;

import java.io.File;
import java.io.PrintStream;
import java.util.Locale;

/**
 * Helper fuctions. 
 * The function in this class are useful but may not have cohesion. 
 * HB: Please revisit and cleanup
 *
 * @author Harmeet Bedi <harmeet@kodemuse.com>
 */
public class NNTPUtil {

    private final static int prefixLength = "file://".length();

    static File getDirectory(Context context, Configuration configuration, String child)
        throws ConfigurationException
    {
        String fileName = configuration.getChild(child).getValue();
        if (!fileName.toLowerCase(Locale.US).startsWith("file://") ) {
            StringBuffer exceptionBuffer =
                new StringBuffer(128)
                        .append("Malformed ")
                        .append(child)
                        .append(" - Must be of the format \"file://<filename>\".");
            throw new ConfigurationException(exceptionBuffer.toString());
        }
        fileName = fileName.substring(prefixLength);
        if (!(fileName.startsWith("/"))) {
            fileName = ((BlockContext)context).getBaseDirectory() +
                       File.separator + fileName;
        }
        File f = new File(fileName);
        if ( f.exists() && f.isFile() )
        {
            StringBuffer exceptionBuffer =
                new StringBuffer(160)
                        .append("Expecting '")
                        .append(f.getAbsolutePath())
                        .append("' directory");
            throw new NNTPException(exceptionBuffer.toString());
        }
        if ( f.exists() == false ) {
            f.mkdirs();
        }
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
            if ( obj instanceof LogEnabled )
                ((LogEnabled)obj).enableLogging( logger );
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
        prt.println("conf.getClass=" + conf.getClass().getName());
        prt.println("name=" + conf.getName());
        Configuration[] children = conf.getChildren();
        for ( int i = 0 ; i < children.length ; i++ ) {
            
            StringBuffer showBuffer =
                new StringBuffer(64)
                        .append(i)
                        .append(". ")
                        .append(children[i].getName());
            prt.println(showBuffer.toString());
        }
    }
}
