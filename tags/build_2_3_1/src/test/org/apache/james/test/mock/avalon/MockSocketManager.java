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
package org.apache.james.test.mock.avalon;

import org.apache.avalon.cornerstone.services.sockets.SocketManager;
import org.apache.avalon.cornerstone.services.sockets.ServerSocketFactory;
import org.apache.avalon.cornerstone.services.sockets.SocketFactory;
import org.apache.avalon.cornerstone.blocks.sockets.DefaultServerSocketFactory;
import org.apache.avalon.cornerstone.blocks.sockets.DefaultSocketFactory;

public class MockSocketManager implements SocketManager {
    private int m_port;

    public MockSocketManager(int port)
    {
        m_port = port;
    }

    public ServerSocketFactory getServerSocketFactory(String string) throws Exception {
        return new DefaultServerSocketFactory();
    }

    public SocketFactory getSocketFactory(String string) throws Exception {
        return new DefaultSocketFactory();
    }
}
