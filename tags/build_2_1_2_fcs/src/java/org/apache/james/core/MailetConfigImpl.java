/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.core;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.mailet.MailetConfig;
import org.apache.mailet.MailetContext;

import java.util.Iterator;

/**
 * Implements the configuration object for a Mailet.
 *
 * @author Serge Knystautas <sergek@lokitech.com>
 */
public class MailetConfigImpl implements MailetConfig {

    /**
     * The mailet MailetContext
     */
    private MailetContext mailetContext;

    /**
     * The mailet name
     */
    private String name;

    //This would probably be better.
    //Properties params = new Properties();
    //Instead, we're tied to the Configuration object
    /**
     * The mailet Avalon Configuration
     */
    private Configuration configuration;

    /**
     * No argument constructor for this object.
     */
    public MailetConfigImpl() {}

    /**
     * Get the value of an parameter stored in this MailetConfig.  Multi-valued
     * parameters are returned as a comma-delineated string.
     *
     * @param name the name of the parameter whose value is to be retrieved.
     *
     * @return the parameter value
     */
    public String getInitParameter(String name) {
        try {
            String result = null;

            final Configuration[] values = configuration.getChildren( name );
            for ( int i = 0; i < values.length; i++ )
            {
                if (result == null) {
                    result = "";
                } else {
                    result += ",";
                }
                Configuration conf = values[i];
                result += conf.getValue();
            }
            return result;
            //return params.getProperty(name);
        } catch (ConfigurationException ce) {
            throw new RuntimeException("Embedded configuration exception was: " + ce.getMessage());
        }

    }

    /**
     * Returns an iterator over the set of configuration parameter names.
     *
     * @throws UnsupportedOperationException in all cases, as this is not implemented
     */ 
    public Iterator getInitParameterNames() {
        throw new UnsupportedOperationException("Not yet implemented");
        //return params.keySet().iterator();
    }

    /**
     * Get the mailet's MailetContext object.
     *
     * @return the MailetContext for the mailet
     */
    public MailetContext getMailetContext() {
        return mailetContext;
    }

    /**
     * Get the mailet's Avalon Configuration object.
     *
     * @return the Configuration for the mailet
     */
    public void setMailetContext(MailetContext newContext) {
        mailetContext = newContext;
    }

    /**
     * Set the Avalon Configuration object for the mailet.
     *
     * @param newConfiguration the new Configuration for the mailet
     */
    public void setConfiguration(Configuration newConfiguration) {
        configuration = newConfiguration;
    }

    /**
     * Get the name of the mailet.
     *
     * @return the name of the mailet
     */
    public String getMailetName() {
        return name;
    }

    /**
     * Set the name for the mailet.
     *
     * @param newName the new name for the mailet
     */
    public void setMailetName(String newName) {
        name = newName;
    }
}
