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

package org.apache.james.socket;

import org.apache.avalon.framework.logger.Logger;
import org.apache.commons.logging.Log;

/**
 * Context sensitive logger for handlers.
 */
public final class HandlerLog implements Log {

    private final Logger logger;
    private final String prefix;
    
    public HandlerLog(final Logger logger, final String prefix) {
        this.logger = logger;
        this.prefix = prefix;
    }
    
    public final void debug(Object message) {
        if (isDebugEnabled()) {
            logger.debug(toString(message));
        }
    }

    public final void debug(Object message, Throwable t) {
        if (isDebugEnabled()) {
            logger.debug(toString(message), t);
        }
    }

    public final void error(Object message) {
        if (isErrorEnabled()) {
            logger.error(toString(message));
        }
    }

    public final void error(Object message, Throwable t) {
        if (isErrorEnabled()) {
            logger.error(toString(message), t);
        }
    }

    public final void fatal(Object message) {
        if (isFatalEnabled()) {
            logger.fatalError(toString(message));
        }
    }

    public final void fatal(Object message, Throwable t) {
        if (isFatalEnabled()) {
            logger.fatalError(toString(message), t);
        }
    }

    public final void info(Object message) {
        if (isInfoEnabled()) {
            logger.info(toString(message));
        }
    }

    public final void info(Object message, Throwable t) {
        if (isInfoEnabled()) {
            logger.info(toString(message), t);
        }
    }

    public final boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public final boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    public final boolean isFatalEnabled() {
        return logger.isFatalErrorEnabled();
    }

    public final boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    public final boolean isTraceEnabled() {
        return isDebugEnabled();
    }

    public final boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    public final void trace(Object message) {
        debug(message);
    }

    public final void trace(Object message, Throwable t) {
        debug(message, t);
    }

    public final void warn(Object message) {
        if (isWarnEnabled()) {
            logger.warn(toString(message));
        }
    }

    public final void warn(Object message, Throwable t) {
        if (isWarnEnabled()) {
            logger.warn(toString(message), t);
        }
    }

    private String toString(final Object message) {
        return prefix + message;
    }
}
