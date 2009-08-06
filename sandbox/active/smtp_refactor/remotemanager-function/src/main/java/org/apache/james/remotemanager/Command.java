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

package org.apache.james.remotemanager;

import java.io.PrintWriter;

/**
 * Commands JAMES through the remote management console.
 */
public interface Command {

    /**
     * Gets the name of this command.
     * @return command name, not null
     */
    public String getName();
    
    /**
     * Outputs useful information for the user of this command.
     * @return user help, not null
     */
    public String help();

    /**
     * Executes this command.
     * @param args raw arguments, not null
     * @param out <code>PrintWriter</code> for user feedback
     * @return true additional commands are expected, false otherwise. Note 
     */
    public boolean execute(String args, final PrintWriter out);
}
