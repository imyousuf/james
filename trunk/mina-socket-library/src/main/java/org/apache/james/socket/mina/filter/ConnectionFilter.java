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

package org.apache.james.socket.mina.filter;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;

/**
 * Use this class to set a connection limit
 */

public class ConnectionFilter extends IoFilterAdapter{

    private Map<String, Integer> connections = new HashMap<String, Integer>();

    private int maxConnections = 0;

    private int maxConnectionsPerIp = 0;

    private Log logger;
    
    public ConnectionFilter(Log logger) {
        this.logger = logger;
    }
    
    public ConnectionFilter(Log logger, int maxConnections, int maxConnectionsPerIp) {
        this.logger = logger;
        this.maxConnections = maxConnections;
        this.maxConnectionsPerIp = maxConnectionsPerIp;
    }
    
    public synchronized void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public synchronized void setMaxConnectionsPerIp(int maxConnectionsPerIp) {
        this.maxConnectionsPerIp = maxConnectionsPerIp;
    }

    public void setLogger(Log logger) {
        this.logger = logger;
    }


    
    /**
     * @see org.apache.mina.core.filterchain.IoFilterAdapter#sessionClosed(org.apache.mina.core.filterchain.IoFilter.NextFilter, org.apache.mina.core.session.IoSession)
     */
    public void sessionClosed(NextFilter arg0, IoSession arg1) throws Exception {
        if (isBlocked(arg1) == false) {
            removeIp(arg1);
        }

        arg0.sessionClosed(arg1);
    }

    /**
     * @see org.apache.mina.core.filterchain.IoFilterAdapter#sessionOpened(org.apache.mina.core.filterchain.IoFilter.NextFilter, org.apache.mina.core.session.IoSession)
     */
    public void sessionOpened(NextFilter arg0, IoSession arg1) throws Exception {
        if (isBlocked(arg1)) {
            arg1.close(true);
        } else {
            addIp(arg1);
            arg0.sessionOpened(arg1);
        }
    }

    /**
     * Check if the IPAdress of this session is allowed to connect
     * 
     * @param session
     *                the IoSession
     * @return true if the IPAddress should get blocked
     */
    private boolean isBlocked(IoSession session) {
        SocketAddress socketAddress = session.getRemoteAddress();

        if (socketAddress instanceof InetSocketAddress) {
            if (maxConnections > 0) {
                if (getIpCount() + 1 > maxConnections) {
                    logger.debug("Maximum allowed connections of "
                            + maxConnections + " reached.");
                    return true;
                }
            }
            if (maxConnectionsPerIp > 0) {
                String ipAddress = ((InetSocketAddress) socketAddress)
                        .getAddress().getHostAddress();
                Integer connectionCount = connections.get(ipAddress);

                if (connectionCount != null
                        && connectionCount + 1 > maxConnectionsPerIp) {
                    logger.debug("Maximum allowed connections of "
                            + maxConnectionsPerIp + " for ip " + ipAddress
                            + "reached.");
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Add IPAddress of the given session to the connection Map
     * 
     * @param session
     *                the IoSession
     */
    private void addIp(IoSession session) {
        SocketAddress socketAddress = session.getRemoteAddress();
        if (socketAddress instanceof InetSocketAddress) {
            String address = ((InetSocketAddress) socketAddress).getAddress()
                    .getHostAddress();
            int count = getIpCount(address) + 1;

            synchronized (connections) {
                connections.put(address, count);
            }
        }
    }

    /**
     * Remote the IPAdress of the given IoSession from the connection Map
     * 
     * @param session
     *                the IoSession
     */
    private synchronized void removeIp(IoSession session) {
        SocketAddress socketAddress = session.getRemoteAddress();
        if (socketAddress instanceof InetSocketAddress) {
            String address = ((InetSocketAddress) socketAddress).getAddress()
                    .getHostAddress();
            int count = getIpCount(address) - 1;

            synchronized (connections) {
                if (count > 0) {
                    connections.put(address, count);
                } else {
                    connections.remove(address);
                }
            }
        }

    }

    /**
     * Get the count of concurrancy connections of the given IPAddress
     * 
     * @param address
     *                the IPAddress
     * @return the count
     */
    private int getIpCount(String address) {
        Integer count = connections.get(address);

        if (count != null) {
            return count.intValue();
        }
        return 0;
    }

    /**
     * Get the count of all current connections
     * 
     * @return the count
     */
    private int getIpCount() {
        int count = 0;
        Collection<Integer> col = connections.values();

        Iterator<Integer> connCount = col.iterator();

        while (connCount.hasNext()) {
            count = count + connCount.next().intValue();
        }
        return count;
    }

}

