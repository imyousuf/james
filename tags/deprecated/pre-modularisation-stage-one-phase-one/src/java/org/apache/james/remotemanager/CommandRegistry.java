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

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;

/**
 * Registers remote manager commands.
 */
public class CommandRegistry extends AbstractLogEnabled {

    private final Command[] commands;
    
    public CommandRegistry(final Command[] commands) {
        this.commands = commands;
    }
    
    public boolean execute(final String commandName, final String args, final PrintWriter out) {
        boolean result = true;
        for (int i=0; i<commands.length;i++) {
            final Command command = commands[i];
            if (commandName.equalsIgnoreCase(command.getName())) {
                final Logger logger = getLogger();
                if (logger != null) logger.debug("Found matching command");
                result = command.execute(args, out);
            }
        }
        return result;
    }
    
    public void printHelp(final PrintWriter out) {
        for (int i=0; i<commands.length;i++) {
            final Command command = commands[i];
            final StringBuffer buffer = new StringBuffer(command.getName());
            while (buffer.length() <= 39) {
                buffer.append(' ');
            }
            buffer.append(command.help());
            out.println(buffer);
        }
    }
}
