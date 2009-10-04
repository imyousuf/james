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

/**
 * Provide help for a command
 *
 */
public class CommandHelp {

    private String syntax;
    private String desc;
    
    public CommandHelp(String syntax) {
        setSyntax(syntax);
    }
    
    public CommandHelp(String syntax, String desc) {
        this(syntax);
        setDescription(desc);
    }
    
    /**
     * Set the syntax
     * 
     * @param syntax
     */
    public void setSyntax(String syntax) {
        this.syntax = syntax;
    }
    
    /**
     * Return the syntax which should get used to call the command
     * 
     * @return syntax
     */
    public String getSyntax() {
        return syntax;
    }
    
    /**
     * Return the description for this command
     * 
     * @return
     */
    public String getDescription() {
        return desc;
    }
    
    /**
     * Set the description
     * 
     * @param desc
     */
    public void setDescription(String desc) {
        this.desc = desc;
    }
}
