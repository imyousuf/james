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
import org.apache.james.context.AvalonContextConstants;
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

    /**
     * The file prefix String
     */
    private final static String filePrefix = "file://";

    /**
     * The length of the file prefix String
     */
    private final static int filePrefixLength = filePrefix.length();

    /**
     * Get the explicit File represented by the URL String stored
     * in the child parameter.
     *
     * @param context the context for the NNTP repository
     * @param configuration the configuration for the NNTP repository
     * @param child the configuration parameter name that stores the URL
     */
    static File getDirectory(Context context, Configuration configuration, String child)
        throws ConfigurationException
    {
        String fileName = configuration.getChild(child).getValue();
        if (!fileName.toLowerCase(Locale.US).startsWith(filePrefix) ) {
            StringBuffer exceptionBuffer =
                new StringBuffer(128)
                        .append("Malformed ")
                        .append(child)
                        .append(" - Must be of the format \"file://<filename>\".");
            throw new ConfigurationException(exceptionBuffer.toString());
        }
        fileName = fileName.substring(filePrefixLength);
        if (!(fileName.startsWith("/"))) {
            String baseDirectory = "";
            try {
                File applicationHome =
                    (File)context.get(AvalonContextConstants.APPLICATION_HOME);
                baseDirectory = applicationHome.toString();
            } catch (ContextException ce) {
                throw new ConfigurationException("Encountered exception when resolving application home in Avalon context.", ce);
            } catch (ClassCastException cce) {
                throw new ConfigurationException("Application home object stored in Avalon context was not of type java.io.File.", cce);
            }
            StringBuffer fileNameBuffer =
                new StringBuffer(128)
                        .append(baseDirectory)
                        .append(File.separator)
                        .append(fileName);
            fileName = fileNameBuffer.toString();
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

    /**
     * Creates an instance of the spool class.
     *
     * @param context the context for the NNTP spooler
     * @param configuration the configuration for the NNTP spooler
     * @param logger the logger for the NNTP spooler
     * @param clsName the class name for the NNTP spooler
     *
     * TODO: This factory method doesn't properly implement the Avalon lifecycle.
     */
    static Object createInstance(Context context, 
                                        Configuration configuration,
                                        Logger logger,
                                        String clsName) 
            throws ConfigurationException
    {
        try {
            clsName = configuration.getAttribute("class");
        } catch(ConfigurationException ce) {
            // TODO: Why is this being caught and ignored?
        }
        try {
            Object obj = NNTPUtil.class.getClassLoader().loadClass(clsName).newInstance();
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
}
