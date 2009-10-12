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
package org.apache.james.container.spring.logging.log4j;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;


/**
 * behaves like an avalon logger, but logs to log4j
 */
public class AvalonToLog4jLogger implements org.apache.avalon.framework.logger.Logger {
    private Logger logger;

    public AvalonToLog4jLogger(Logger logger) {
        this.logger = logger;
    }

    public void debug(String s) {
        logger.debug(s);
    }

    public void debug(String s, Throwable throwable) {
        logger.debug(s, throwable);
    }

    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public void info(String s) {
        logger.info(s);
    }

    public void info(String s, Throwable throwable) {
        logger.info(s, throwable);
    }

    public boolean isInfoEnabled() {
        return logger.isEnabledFor(Level.INFO);
    }

    public void warn(String s) {
        logger.warn(s);
    }

    public void warn(String s, Throwable throwable) {
        logger.warn(s, throwable);
    }

    public boolean isWarnEnabled() {
        return logger.isEnabledFor(Level.WARN);
    }

    public void error(String s) {
        logger.error(s);
    }

    public void error(String s, Throwable throwable) {
        logger.error(s, throwable);
    }

    public boolean isErrorEnabled() {
        return logger.isEnabledFor(Level.ERROR);
    }

    public void fatalError(String s) {
        logger.fatal(s);
    }

    public void fatalError(String s, Throwable throwable) {
        logger.fatal(s, throwable);
    }

    public boolean isFatalErrorEnabled() {
        return logger.isEnabledFor(Level.FATAL);
    }

    public org.apache.avalon.framework.logger.Logger getChildLogger(String s) {
        return new AvalonToLog4jLogger(Logger.getLogger(s));
    }
}
