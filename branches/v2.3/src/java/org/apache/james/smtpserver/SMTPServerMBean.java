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

package org.apache.james.smtpserver;

/**
 * An interface to expose James management functionality through JMX.
 * 
 * @phoenix:mx-topic name="SMTPServer"
 */
public interface SMTPServerMBean {
    /**
    * @phoenix:mx-attribute
    * @phoenix:mx-description Returns flag indicating it this service is enabled 
    * @phoenix:mx-isWriteable no
    * 
    * @return boolean The enabled flag     
    */  
    public boolean isEnabled();

    /**
    * @phoenix:mx-attribute
    * @phoenix:mx-description Returns the port that the service is bound to 
    * @phoenix:mx-isWriteable no
    * 
    * @return int The port number     
    */  
    public int  getPort();
    
    /**
    * @phoenix:mx-attribute
    * @phoenix:mx-description Returns the address if the network interface the socket is bound to 
    * @phoenix:mx-isWriteable no
    * 
    * @return String The network interface name     
    */  
    public String  getNetworkInterface();
    
    /**
    * @phoenix:mx-attribute
    * @phoenix:mx-description Returns the server socket type, plain or SSL 
    * @phoenix:mx-isWriteable no
    * 
    * @return String The scoekt type, plain or SSL     
    */  
    public String  getSocketType();
}
