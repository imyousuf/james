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

import java.util.Iterator;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.smtpserver.ConnectHandler;
import org.apache.james.smtpserver.MessageHandler;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.util.junkscore.ComposedJunkScore;
import org.apache.james.util.junkscore.JunkScore;
import org.apache.james.util.junkscore.JunkScoreImpl;
import org.apache.james.util.mail.dsn.DSNStatus;

public class JunkScoreHandler extends AbstractLogEnabled implements ConnectHandler, MessageHandler,Configurable {

    private double maxScore = 0;
    private String action;
    private static final String REJECT_ACTION = "reject";
    private static final String COMPOSE_ACTION = "compose";
    private static final String HEADER_ACTION = "header";

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(org.apache.avalon.framework.configuration.Configuration)
     */
    public void configure(Configuration arg0) throws ConfigurationException {
        Configuration maxScoreConfig = arg0.getChild("maxScore");

        if (maxScoreConfig != null) {
            setMaxScore(maxScoreConfig.getValueAsDouble(0));
        }
        
        Configuration actionConfig = arg0.getChild("action");
        if (actionConfig != null) {
            setAction(actionConfig.getValue().toLowerCase());
        } else {
            throw new ConfigurationException("Please configure the action");
        }
    }
    
    public void setMaxScore(double maxScore) {
        this.maxScore = maxScore;
    }
    
    public void setAction(String action) throws ConfigurationException {
        if (!action.equals(REJECT_ACTION) && !action.equals(COMPOSE_ACTION) && !action.equals(HEADER_ACTION)) 
            throw new ConfigurationException("Illegal action: " + action);
     
        this.action = action;  
    }
    
    /**
     * @see org.apache.james.smtpserver.MessageHandler#onMessage(org.apache.james.smtpserver.SMTPSession)
     */
    public void onMessage(SMTPSession session) {
        checkScore(session);
    }

    /**
     * 
     */
    private void checkScore(SMTPSession session) {
        JunkScore score1 = (JunkScore) session.getConnectionState().get(JunkScore.JUNK_SCORE_SESSION);
        JunkScore score2 = (JunkScore) session.getState().get(JunkScore.JUNK_SCORE);
        JunkScore composed = new ComposedJunkScore(score1,score2);
        
        if (action.equals(COMPOSE_ACTION)) {
            // Save the scores attribute to maybe use it later!
            session.getMail().setAttribute(JunkScore.JUNK_SCORE_SESSION_ATTR, String.valueOf(score1.getCompleteStoredScores()));
            session.getMail().setAttribute(JunkScore.JUNK_SCORE_ATTR, String.valueOf(score2.getCompleteStoredScores()));
            session.getMail().setAttribute(JunkScore.JUNK_SCORE_COMPOSED_ATTR, String.valueOf(composed.getCompleteStoredScores()));
        } else if (action.equals(REJECT_ACTION)) {
            if (maxScore <  composed.getCompleteStoredScores()) {

                
                String responseString = "554 "
                    + DSNStatus.getStatus(DSNStatus.PERMANENT,
                            DSNStatus.SECURITY_OTHER)
                    + " This message reach the spam hits treshold. Please contact the Postmaster if the email is not SPAM. Message rejected";
                StringBuffer buffer = new StringBuffer(256).append(
                    "Rejected message from ").append(
                    session.getState().get(SMTPSession.SENDER)
                            .toString()).append(" from host ")
                    .append(session.getRemoteHost()).append(" (")
                    .append(session.getRemoteIPAddress()).append(
                            ") " + responseString).append(
                            ". Required rejection hits: "
                                    + maxScore
                                    + " hits: " + composed.getCompleteStoredScores());
                getLogger().info(buffer.toString());
                
                session.writeResponse(responseString);
                session.setStopHandlerProcessing(true);
                session.abortMessage();
            }
        } else if (action.equals(HEADER_ACTION)) {
            try {
                MimeMessage message = session.getMail().getMessage();
                Map scores = composed.getStoredScores();
                Iterator itScores = scores.keySet().iterator();
        
                StringBuffer header = new StringBuffer();
                while (itScores.hasNext()) {
                    String key = itScores.next().toString();
                    header.append(key);
                    header.append("=");
                    header.append(scores.get(key));
                    header.append("; ");
                }
        
                message.setHeader("X-JUNKSCORE", header.toString());
                message.setHeader("X-JUNKSCORE-COMPOSED", String.valueOf(composed.getCompleteStoredScores()));
        
            } catch (MessagingException e) {
                getLogger().info("Unable to add Junkscore to header: " + e.getMessage());
            }
        }
    }
        
    /**
     * @see org.apache.james.smtpserver.ConnectHandler#onConnect(org.apache.james.smtpserver.SMTPSession)
     */
    public void onConnect(SMTPSession session) {
        session.getState().put(JunkScore.JUNK_SCORE, new JunkScoreImpl());
        session.getConnectionState().put(JunkScore.JUNK_SCORE_SESSION, new JunkScoreImpl());
    } 
}
