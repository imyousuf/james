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

package org.apache.mailet;

/* A specialized subclass of javax.mail.URLName, which provides location
 * information for servers.
 * 
 * @since Mailet API v2.2.0a16-unstable
 */
public class HostAddress extends javax.mail.URLName
{
    private String hostname;

    public HostAddress(String hostname, String url)
    {
        super(url);
        this.hostname = hostname;
    }

    public String getHostName()
    {
        return hostname;
    }

/*
    public static void main(String[] args) throws Exception
    {
        HostAddress url;
        try
        {
            url = new HostAddress("mail.devtech.com", "smtp://" + "66.112.202.2" + ":25");
            System.out.println("Hostname: " + url.getHostName());
            System.out.println("The protocol is: " + url.getProtocol());
            System.out.println("The host is: " + url.getHost());
            System.out.println("The port is: " + url.getPort());
            System.out.println("The user is: " + url.getUsername());
            System.out.println("The password is: " + url.getPassword());
            System.out.println("The file is: " + url.getFile());
            System.out.println("The ref is: " + url.getRef());
        }
        catch (Exception e)
        {
            System.err.println(e);
        };
    }
*/
}
