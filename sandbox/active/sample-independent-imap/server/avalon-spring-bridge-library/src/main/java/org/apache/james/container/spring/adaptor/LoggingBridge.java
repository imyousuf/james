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

import org.apache.avalon.framework.logger.Logger;
import org.apache.james.container.spring.logging.LoggerToComponentMapper;
import org.apache.james.container.spring.logging.LogWorker;

/**
 * bridge to act as a Avalon logger but effectively forward to a some other logging service
 */
public class LoggingBridge implements Logger, LoggerToComponentMapper {
    public static final String LEVEL_DEBUG = "debug";
    public static final String LEVEL_INFO  = "info";
    public static final String LEVEL_WARN  = "warn";
    public static final String LEVEL_ERROR = "error";
    public static final String LEVEL_FATAL = "fatal";

    private boolean m_debugEnabled = true;
    private LogWorker logWorker;

    public void setLogWorker(LogWorker logWorker) {
        this.logWorker = logWorker;
    }

    public Logger getComponentLogger(String beanName) {
        return this; // every bean gets the same logger, could be more finegrained
    }

    protected void forwardLogMessage(String level, String message) {
        logWorker.logMessage(level, message);
    }

    protected void forwardLogException(String level, String message, Throwable exception) {
        logWorker.logException(level, message, exception);
    }

    public org.apache.avalon.framework.logger.Logger getChildLogger(java.lang.String string) {
        return this;
    }

    public void debug(java.lang.String string) {
        forwardLogMessage(LEVEL_DEBUG, string);
    }

    public void debug(java.lang.String string, java.lang.Throwable throwable) {
        forwardLogException(LEVEL_DEBUG, string, throwable);
    }

    public boolean isDebugEnabled() {
        return m_debugEnabled;
    }

    public void disableDebug() {
        m_debugEnabled = false;
    }

    public void info(java.lang.String string) {
        forwardLogMessage(LEVEL_INFO, string);
    }

    public void info(java.lang.String string, java.lang.Throwable throwable) {
        forwardLogException(LEVEL_INFO, string, throwable);
    }

    public boolean isInfoEnabled() {
        return true;
    }

    public void warn(java.lang.String string) {
        forwardLogMessage(LEVEL_WARN, string);
    }

    public void warn(java.lang.String string, java.lang.Throwable throwable) {
        forwardLogException(LEVEL_WARN, string, throwable);
    }

    public boolean isWarnEnabled() {
        return true;
    }

    public void error(java.lang.String string) {
        forwardLogMessage(LEVEL_ERROR, string);
    }

    public void error(java.lang.String string, java.lang.Throwable throwable) {
        forwardLogException(LEVEL_ERROR, string, throwable);
    }

    public boolean isErrorEnabled() {
        return true;
    }

    public void fatalError(java.lang.String string) {
        forwardLogMessage(LEVEL_FATAL, string);
    }

    public void fatalError(java.lang.String string, java.lang.Throwable throwable) {
        forwardLogException(LEVEL_FATAL, string, throwable);
    }

    public boolean isFatalErrorEnabled() {
        return true;
    }


}
