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
 *
 * @author Serge Knystautas <sergek@lokitech.com>
 */
public class MailetConfigImpl implements MailetConfig {
    private MailetContext mailetContext;
    private String name;
    //This would probably be better.
    //Properties params = new Properties();
    //Instead, we're tied to the Configuration object
    private Configuration configuration;

    public MailetConfigImpl() {

    }

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

    public Iterator getInitParameterNames() {
        throw new RuntimeException("Not yet implemented");
        //return params.keySet().iterator();
    }

    public MailetContext getMailetContext() {
        return mailetContext;
    }

    public void setMailetContext(MailetContext newContext) {
        mailetContext = newContext;
    }

    public void setConfiguration(Configuration newConfiguration) {
        configuration = newConfiguration;
    }

    public String getMailetName() {
        return name;
    }

    public void setMailetName(String newName) {
        name = newName;
    }
}
