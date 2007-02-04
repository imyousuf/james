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

package org.apache.james.mailboxmanager.util;

import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.SimpleLog;

public abstract class AbstractLogFactoryAware implements LogFactoryAware, LogEnabled {
    
    private Log log;
    private MyLogFactory logFactory;

    protected Log getLog() {
        if (log==null) {
            SimpleLog simpleLog=new SimpleLog(this.getClass().getName());
            simpleLog.setLevel(SimpleLog.LOG_LEVEL_ALL);
            log=simpleLog;
        }
        return log;
    }
    
    public void setLog(Log log) {
        this.log=log;
    }

    public void setLogFactory(MyLogFactory logFactory) {
        this.logFactory=logFactory;
    }
    
    protected MyLogFactory getLogFacory() {
        return logFactory;
    }
    
    protected void setupChildLog(Object object, String childName) {
        MyLogFactory childFactory=logFactory.getChildFactory(childName);
        if (object instanceof LogFactoryAware) {
            ((LogFactoryAware)object).setLogFactory(childFactory);
        }
        if (object instanceof LogAware) {
            ((LogAware)object).setLog(childFactory.getLog());
        }    
    }

    public void enableLogging(Logger logger) {
        this.logFactory=new AvalonLogFactory(logger);
        this.log=logFactory.getLog();
    }
    
    

}
