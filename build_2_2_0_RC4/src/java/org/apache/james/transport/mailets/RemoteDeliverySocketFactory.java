/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
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

package org.apache.james.transport.mailets;

import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.IOException;

/**
 * It is used by RemoteDelivery in order to make possible to bind the client
 * socket to a specific ip address.
 *
 * This is not a nice solution because the ip address must be shared by all 
 * RemoteDelivery instances. It would be better to modify JavaMail 
 * (current version 1.3) to support a corresonding property, e.g.
 * mail.smtp.bindAdress.
 * 
 * It should be a javax.net.SocketFactory descendant, but 
 * 1. it is not necessary because JavaMail 1.2 uses reflection when accessing
 * this class;
 * 2. it is not desirable because it would require java 1.4.
 */
public class RemoteDeliverySocketFactory {
    
    /**
     * @param addr the ip address or host name the delivery socket will bind to
     */
    static void setBindAdress(String addr) throws UnknownHostException {
        if (addr == null) bindAddress = null;
        else bindAddress = InetAddress.getByName(addr);
    }
    
    /**
     * the same as the similarly named javax.net.SocketFactory operation.
     */
    public static RemoteDeliverySocketFactory getDefault() {
        return new RemoteDeliverySocketFactory();
    }
    
    /**
     * the same as the similarly named javax.net.SocketFactory operation.
     * Just to be safe, it is not used by JavaMail 1.3.
     */
    public Socket createSocket() throws IOException {
        throw new IOException("Incompatible JavaMail version, " +
                "cannot bound socket");
    }
    
    /**
     * the same as the similarly named javax.net.SocketFactory operation.
     * This is the one which is used by JavaMail 1.3.
     */
    public Socket createSocket(String host, int port)
                            throws IOException, UnknownHostException {
        return new Socket(host, port, bindAddress, 0);
    }
    
    /**
     * the same as the similarly named javax.net.SocketFactory operation.
     * Just to be safe, it is not used by JavaMail 1.3.
     */
    public Socket createSocket(String host,
                                    int port,
                                    InetAddress clientHost,
                                    int clientPort)
                                    throws IOException,
                                    UnknownHostException {
        return new Socket(host, port, 
                clientHost == null ? bindAddress : clientHost, clientPort);
    }
    
    /**
     * the same as the similarly named javax.net.SocketFactory operation.
     * Just to be safe, it is not used by JavaMail 1.3.
     */
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return new Socket(host, port, bindAddress, 0);
    }
    
    /**
     * the same as the similarly named javax.net.SocketFactory operation.
     * Just to be safe, it is not used by JavaMail 1.3.
     */
    public Socket createSocket(InetAddress address,
                                    int port,
                                    InetAddress clientAddress,
                                    int clientPort)
                             throws IOException {
        return new Socket(address, port, 
                clientAddress == null ? bindAddress : clientAddress, 
                clientPort);
    }
    
    /**
     * it should be set by setBindAdress(). Null means the socket is bind to 
     * the default address.
     */
    private static InetAddress bindAddress;
}
