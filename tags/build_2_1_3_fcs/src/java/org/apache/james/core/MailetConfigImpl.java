/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
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
