/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

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
