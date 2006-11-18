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



package org.apache.james.smtpserver.core.filter.fastfail;

import org.apache.james.util.mail.dsn.DSNStatus;

public class JunkHandlerData {
    
    private String rejectLogString = "Bad email. Reject email";
    private String rejectResponseString = "554 " + DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.SECURITY_OTHER) + " " + rejectLogString;
    private String junkScoreLogString = "Bad email. Add JunkScore";
    private String scoreName = "JunkHandler";
    
    /**
     * The ResponseString which get returned in SMTP transaction if the email get rejected
     * 
     * @param rejectResponseString the responseString
     */
    public void setRejectResponseString(String rejectResponseString) {
        this.rejectResponseString = rejectResponseString;
    }
    
    /**
     * Set the logString which will be used on junkScore action
     * 
     * @param junkScoreLogString the logString
     */
    public void setJunkScoreLogString(String junkScoreLogString) {
        this.junkScoreLogString = junkScoreLogString;
    }
    
    /**
     * Set the logString which will be used on reject action
     * 
     * @param rejectLogString the logString
     */
    public void setRejectLogString(String rejectLogString) {
        this.rejectLogString = rejectLogString;
    }
    
    /**
     * The the keyname which will be used to store the junkScore
     * 
     * @param scoreName the name
     */
    public void setScoreName(String scoreName) {
        this.scoreName = scoreName;
    }
    
    /**
     * Get the reponseString to return 
     * 
     * @param session the SMTPSession
     * @return rejectResponseString
     */
    public String getRejectResponseString() {
        return rejectResponseString;
    }
    
    /**
     * Return the LogString if a JunkScore action is used
     * 
     * @param session the SMTPSession
     * @return the LogString
     */
    public String getJunkScoreLogString() {
        return junkScoreLogString;
    }
    
    /**
     * Return the LogString if a Reject action is used
     * 
     * @param the SMTPSession
     * @return the LogString
     */
    public String getRejectLogString() {
        return rejectLogString;
    }
    
    /**
     * Return the Name which will used to store the JunkScore and get used in the headers
     * 
     * @return the name
     */
    protected String getScoreName() {
        return scoreName;
    }

}
