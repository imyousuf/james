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


package org.apache.james.core;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.SimpleLog;

public class SplitOutputStream extends FilterOutputStream {

    private OutputStream debugOutputStream;

    StringBuffer logString = new StringBuffer();

    private boolean DEEP_DEBUG = false;

    private Log log;

    public SplitOutputStream(OutputStream out, OutputStream debug) {
        super(out);
        debugOutputStream = debug;
    }

    public void flush() throws IOException {
        super.flush();
        if (debugOutputStream != null) {
            debugOutputStream.flush();
        }
    }

    public void write(int b) throws IOException {
        super.write(b);
        if (DEEP_DEBUG) {
            if (b == 10) {
                getLog().debug(logString);
                logString = new StringBuffer();
            } else if (b != 13) {
                logString.append((char) b);
            }
        }
        if (debugOutputStream != null) {
            debugOutputStream.write(b);
            debugOutputStream.flush();
        }
    }

    public void setLog(Log log) {
        this.log = log;
    }

    protected Log getLog() {
        if (log == null) {
            log = new SimpleLog("SplitOutputStream");
        }
        return log;
    }

}
