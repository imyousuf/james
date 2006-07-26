/***********************************************************************
 * Copyright (c) 2006 The Apache Software Foundation.                  *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/
package org.apache.james.test.mock.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

// Mocked SPAMD Daemon
public class MockSpamd implements Runnable {

    // The GTUBE teststring
    public final static String GTUBE = "-SPAM-";

    public final static String NOT_SPAM = "Spam: False ; 3 / 5";

    public final static String SPAM = "Spam: True ; 1000 / 5";

    BufferedReader in;

    OutputStream out;

    Socket spamd;

    ServerSocket socket;

    boolean stopped = false;

    public MockSpamd(int port) throws IOException {  
        socket = new ServerSocket(port);
    }

    public void run() {
        boolean spam = false;
        
        try {

            spamd = socket.accept();

            in = new BufferedReader(new InputStreamReader(spamd
                    .getInputStream()));
            out = spamd.getOutputStream();

            String line = null;

            while ((line = in.readLine()) != null) {
                if (line.indexOf(GTUBE) >= 0) {
                    spam = true;
                }
            }
            if (spam) {
                out.write(SPAM.getBytes());
                out.flush();
            } else {
                out.write(NOT_SPAM.getBytes());
                out.flush();
            }
            
            in.close();
            out.close();
            spamd.close();
            socket.close();
            
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
