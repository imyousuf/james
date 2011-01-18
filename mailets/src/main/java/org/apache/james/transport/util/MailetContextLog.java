/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/
package org.apache.james.transport.util;

import org.apache.commons.logging.Log;
import org.apache.mailet.MailetContext;

/**
 * {@link Log} implementation which delegate the logging to a {@link MailetContext}
 * 
 *
 */
public class MailetContextLog implements Log {

    private boolean isDebug;
    private MailetContext context;

    public MailetContextLog(MailetContext context, boolean isDebug) {
        this.context = context;
        this.isDebug = isDebug;
    }
    
    public MailetContextLog(MailetContext context) {
        this(context, false);
        
    }
    

    /**
     * Only log if {@link #isDebugEnabled()} is true
     */
    public void debug(Object arg0) {
        if (isDebug) {
            context.log(arg0.toString());
        }
    }

    /**
     * Only log if {@link #isDebugEnabled()} is true
     */
    public void debug(Object arg0, Throwable arg1) {
        if (isDebug) {
            context.log(arg0.toString(), arg1);
        }            
    }

    /*
     * (non-Javadoc)
     * @see org.apache.commons.logging.Log#error(java.lang.Object)
     */
    public void error(Object arg0) {
        context.log(arg0.toString());
        
    }

    /*
     * (non-Javadoc)
     * @see org.apache.commons.logging.Log#error(java.lang.Object, java.lang.Throwable)
     */
    public void error(Object arg0, Throwable arg1) {
        context.log(arg0.toString(), arg1);
        
    }

    /*
     * (non-Javadoc)
     * @see org.apache.commons.logging.Log#fatal(java.lang.Object)
     */
    public void fatal(Object arg0) {
        context.log(arg0.toString());
        
    }

    /*
     * (non-Javadoc)
     * @see org.apache.commons.logging.Log#fatal(java.lang.Object, java.lang.Throwable)
     */
    public void fatal(Object arg0, Throwable arg1) {
        context.log(arg0.toString(), arg1);
        
    }

    /*
     * (non-Javadoc)
     * @see org.apache.commons.logging.Log#info(java.lang.Object)
     */
    public void info(Object arg0) {
        context.log(arg0.toString());
        
    }

    /*
     * (non-Javadoc)
     * @see org.apache.commons.logging.Log#info(java.lang.Object, java.lang.Throwable)
     */
    public void info(Object arg0, Throwable arg1) {
        context.log(arg0.toString(), arg1);
        
    }

    /**
     * Return true if debug logging is enabled
     */
    public boolean isDebugEnabled() {
        return isDebug;
    }

    /**
     * Enabled, return true
     */
    public boolean isErrorEnabled() {
        return true;
    }

    /**
     * Enabled, return true
     */
    public boolean isFatalEnabled() {
        return true;
    }

    /**
     * Enabled, return true
     */
    public boolean isInfoEnabled() {
        return true;

    }

    /**
     * Not enabled return false
     */
    public boolean isTraceEnabled() {
        return false;
    }

    /**
     * Enabled, return true
     */
    public boolean isWarnEnabled() {
        return true;
    }

    /**
     * Do nothing
     */
    public void trace(Object arg0) {            
    }

    /**
     * Do nothing
     */
    public void trace(Object arg0, Throwable arg1) {
        
    }

    /*
     * (non-Javadoc)
     * @see org.apache.commons.logging.Log#warn(java.lang.Object)
     */
    public void warn(Object arg0) {
        context.log(arg0.toString());
        
    }

    /*
     * (non-Javadoc)
     * @see org.apache.commons.logging.Log#warn(java.lang.Object, java.lang.Throwable)
     */
    public void warn(Object arg0, Throwable arg1) {
        context.log(arg0.toString(), arg1);
        
    }
    
}
