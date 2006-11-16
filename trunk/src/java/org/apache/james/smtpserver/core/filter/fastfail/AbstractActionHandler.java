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
public abstract class AbstractActionHandler extends AbstractLogEnabled implements Configurable {
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
     * @see org.apache.james.smtpserver.CommandHandler#onCommand(SMTPSession)
     */
    public void onCommand(SMTPSession session) {
        if (check(session)) {
            if (getAction().equals(JunkScoreConfigUtil.JUNKSCORE)) {
                if (getLogger().isInfoEnabled()) {
                    getLogger().info(getJunkScoreLogString(session)+" Add Junkscore: " + getScore());
                }
                JunkScore junk = getJunkScore(session);
                junk.setStoredScore(getScoreName(), getScore());
                 
            } else {
                String response = getResponseString(session);
                getLogger().info(response);
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
     * Get the reponseString to return 
     * 
     * @param session the SMTPSession
     * @return responseString
     */
    protected abstract String getResponseString(SMTPSession session);
    
    /**
     * Return the LogString if a JunkScore action is used
     * 
     * @param session the SMTPSession
     * @return the LogString
     */
    protected abstract String getJunkScoreLogString(SMTPSession session);
    
    /**
     * Return the LogString if a Reject action is used
     * 
     * @param the SMTPSession
     * @return the LogString
     */
    protected abstract String getRejectLogString(SMTPSession session);
    
    /**
     * Return the Name which will used to store the JunkScore and get used in the headers
     * @return the name
     */
    protected abstract String getScoreName();
    
    /**
     * Return the JunkScore object
     * 
     * @return junkScore
     */
    protected JunkScore getJunkScore(SMTPSession session) {
        return (JunkScore) session.getState().get(JunkScore.JUNK_SCORE);
    }
}
