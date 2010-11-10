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
 * JMX MBean for CommandHandler
 *
 */
public interface CommandHandlerStatsMBean {

    /**
     * Return the count of temporary errors returned by the handler
     * 
     * @return tempCount
     */
    public long getTemporaryError();
    
    /**
     * Return the count of permanent errors returned by the handler
     * 
     * @return permCount
     */
    public long getPermantError();

    /**
     * Return the count of successful handling returned by the handler
     * 
     * @return tempCount
     */
    public long getOk();
    
    /**
     * Return the count of all processed transactions by the handler
     * 
     * @return all
     */
    public long getAll();
    
    /**
     * Return the name of the handler
     * 
     * @return name
     */
    public String getName();
    
    /**
     * Return all implemented commands by this handler
     * 
     * @return commands
     */
    public String[] getCommands();
}
