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

package org.apache.james.smtpserver.protocol.hook;

/**
 * Result which get used for hooks
 * 
 */
public class HookResult {

    private int result;
    private String smtpRetCode;
    private String smtpDescription;
    
    /**
     * Construct new HookResult
     * 
     * @param result
     * @param smtpRetCode 
     * @param smtpDescription
     */
    public HookResult(int result, String smtpRetCode, CharSequence smtpDescription) {
        this.result = result;
        this.smtpRetCode = smtpRetCode;
        this.smtpDescription = (smtpDescription == null) ? null : smtpDescription.toString();
    }
    
    /**
     * Construct new HookResult
     * 
     * @param result
     * @param smtpDescription
     */
    public HookResult(int result, String smtpDescription) {
        this(result,null,smtpDescription);
    }
    
    /**
     * Construct new HookResult
     * 
     * @param result
     */
    public HookResult(int result) {
        this(result,null,null);
    }
    
    /**
     * Return the result
     * 
     * @return result
     */
    public int getResult() {
        return result;
    }
    
    /**
     * Return the SMTPRetCode which should used. If not set return null. 
     * 
     * @return smtpRetCode
     */
    public String getSmtpRetCode() {
        return smtpRetCode;
    }
    
    /**
     * Return the SMTPDescription which should used. If not set return null
     *  
     * @return smtpDescription
     */
    public String getSmtpDescription() {
        return smtpDescription;
    }
}
