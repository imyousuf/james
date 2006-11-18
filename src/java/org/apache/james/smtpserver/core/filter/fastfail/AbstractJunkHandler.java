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

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.util.junkscore.JunkScore;
import org.apache.james.util.junkscore.JunkScoreConfigUtil;

/**
 * TODO: Should we split this class  ?
 *       Or maybe add a Handler which loads other handlers ?
 *
 */
public abstract class AbstractJunkHandler extends AbstractLogEnabled implements Configurable {
    private String action = "reject";
    private double score = 0;

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration handlerConfiguration) throws ConfigurationException {
        
        Configuration configAction = handlerConfiguration.getChild("action",false);
        if(configAction != null) {
            String configString = configAction.getValue();
            if (configString.startsWith(JunkScoreConfigUtil.JUNKSCORE)) {
                setAction(JunkScoreConfigUtil.JUNKSCORE);
                setScore(JunkScoreConfigUtil.getJunkScore(configAction.getValue()));
            }
        }
        
    }
    
    /**
     * Set the Action which should be taken if the mail from has no valid domain.
     * Supported are: junkScore and reject
     * 
     * @param action the action
     */
    public void setAction(String action) {
        this.action = action.toLowerCase();
    }
    
    /**
     * Set the score which will get added to the JunkScore object if the action is junkScore andt the sender has no valid domain
     * 
     * @param score the score
     */
    public void setScore(double score) {
        this.score = score;
    }
    
    /**
     * Return the configured score
     * 
     * @return score
     */
    protected double getScore() {
        return score;
    }
    
    
    /**
     * Return the action
     * 
     * @return action
     */
    protected String getAction() {
        return action;
    }
    
    /**
     * Process the checking
     * 
     * @param session the SMTPSession
     */
    protected void doProcessing(SMTPSession session) {
        if (check(session)) {
            if (getAction().equals(JunkScoreConfigUtil.JUNKSCORE)) {
                getLogger().info(getJunkHandlerData(session).getJunkScoreLogString());
                JunkScore junk = getJunkScore(session);
                junk.setStoredScore(getJunkHandlerData(session).getScoreName(), getScore());
                 
            } else {
                String response = getJunkHandlerData(session).getRejectResponseString();
                
                if (getJunkHandlerData(session).getRejectLogString() != null) getLogger().info(getJunkHandlerData(session).getRejectLogString());
                
                session.writeResponse(response);
                // After this filter match we should not call any other handler!
                session.setStopHandlerProcessing(true);
            }
        } 
    }
    
    /**
     * All checks must be done in this method
     * 
     * @param session the SMTPSession
     * @return true if the check match
     */
    protected abstract boolean check(SMTPSession session);
    
    /**
     * Get the JunkHandlerData to work with
     * 
     * @param session the SMTPSession
     * @return junkHandlerData
     */
    public abstract JunkHandlerData getJunkHandlerData(SMTPSession session);
   
    /**
     * Return the JunkScore object.
     * 
     * @return junkScore
     */
    protected JunkScore getJunkScore(SMTPSession session) {
        return (JunkScore) session.getState().get(JunkScore.JUNK_SCORE);
    }
}
