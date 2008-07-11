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
package org.apache.james.container.spring.adaptor;

import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.phoenix.BlockContext;

import java.io.File;

/**
 * mimmicks the behavior of an Avalon context. note: the Avalon context is in the process of being removed from James
 * by as well be needed for some Avalon/Cornerstone components
 */
public class AvalonContext implements Context {

    private String applicationHome = null;
    private String applicationName = null;

    public void setApplicationHome(String applicationHome) {
        this.applicationHome = applicationHome;
    }
    
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public Object get(Object key) throws ContextException {
        if( BlockContext.APP_NAME.equals( key ) )
        {
            return applicationName;
        }
        else if( BlockContext.APP_HOME_DIR.equals( key ) )
        {
            return new File(applicationHome);
        }
        else if( BlockContext.NAME.equals( key ) )
        {
            return "Avalon Context";
        }
        else
        {
            throw new ContextException( "Unknown key: " + key );
        }
    }
}
