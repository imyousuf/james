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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.SimpleLog;

public class CopyInputStream extends InputStream
{

    private InputStream is;

    private OutputStream copy;

    private Log log;

    StringBuilder logString = new StringBuilder();
    
    private boolean DEEP_DEBUG = false;

    public CopyInputStream(InputStream is, OutputStream copy)
    {
        this.is = is;
        this.copy = copy;
    }

    public int read() throws IOException {
        int in = is.read();
        copy.write(in);
        if (DEEP_DEBUG) {
            if (in == 10) {
                getLog().debug(logString);
                logString = new StringBuilder();
            } else if (in != 13) {
                logString.append((char) in);
            }
        }
        return in;
    }
    
    protected Log getLog() {
        if (log==null) {
            log=new SimpleLog("CopyInputStream");
        }
        return log;
    }
    
    public void setLog(Log log) {
        this.log=log;
    }

}
