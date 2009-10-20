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
package org.apache.james.smtpserver.protocol.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.james.api.protocol.ExtensibleHandler;
import org.apache.james.api.protocol.WiringException;
import org.apache.james.dsn.DSNStatus;
import org.apache.james.smtpserver.protocol.CommandHandler;
import org.apache.james.smtpserver.protocol.LineHandler;
import org.apache.james.smtpserver.protocol.MailEnvelopeImpl;
import org.apache.james.smtpserver.protocol.SMTPRequest;
import org.apache.james.smtpserver.protocol.SMTPResponse;
import org.apache.james.smtpserver.protocol.SMTPRetCode;
import org.apache.james.smtpserver.protocol.SMTPSession;
import org.apache.mailet.MailAddress;


/**
  * handles DATA command
 */
public class DataCmdHandler implements CommandHandler, ExtensibleHandler {

    public final class DataConsumerLineHandler implements LineHandler {
        /**
         * @see org.apache.james.smtpserver.protocol.LineHandler#onLine(org.apache.james.smtpserver.protocol.SMTPSession, byte[])
         */
        public void onLine(SMTPSession session, byte[] line) {
            // Discard everything until the end of DATA session
            if (line.length == 3 && line[0] == 46) {
                session.popLineHandler();
            }
        }
    }

    public final class DataLineFilterWrapper implements LineHandler {

        private DataLineFilter filter;
        private LineHandler next;
        
        public DataLineFilterWrapper(DataLineFilter filter, LineHandler next) {
            this.filter = filter;
            this.next = next;
        }
        public void onLine(SMTPSession session, byte[] line) {
            filter.onLine(session, line, next);
        }
                
    }
   
    public final static String MAILENV = "MAILENV";
    
    private LineHandler lineHandler;
    
    /**
     * process DATA command
     *
     * @see org.apache.james.smtpserver.protocol.CommandHandler#onCommand(SMTPSession)
     */
    public SMTPResponse onCommand(SMTPSession session, SMTPRequest request) {
        String parameters = request.getArgument();
        SMTPResponse response = doDATAFilter(session,parameters);
        
        if (response == null) {
            return doDATA(session, parameters);
        } else {
            return response;
        }
    }


    /**
     * Handler method called upon receipt of a DATA command.
     * Reads in message data, creates header, and delivers to
     * mail server service for delivery.
     *
     * @param session SMTP session object
     * @param argument the argument passed in with the command by the SMTP client
     */
    @SuppressWarnings("unchecked")
    protected SMTPResponse doDATA(SMTPSession session, String argument) {
        MailEnvelopeImpl env = new MailEnvelopeImpl();
        env.setRecipients(new ArrayList<MailAddress>((Collection)session.getState().get(SMTPSession.RCPT_LIST)));
        env.setSender((MailAddress) session.getState().get(SMTPSession.SENDER));
        session.getState().put(MAILENV, env);
        session.pushLineHandler(lineHandler);
        
        return new SMTPResponse(SMTPRetCode.DATA_READY, "Ok Send data ending with <CRLF>.<CRLF>");
    }
    
    /**
     * @see org.apache.james.smtpserver.protocol.CommandHandler#getImplCommands()
     */
    public Collection<String> getImplCommands() {
        Collection<String> implCommands = new ArrayList<String>();
        implCommands.add("DATA");
        
        return implCommands;
    }


    /**
     * @see org.apache.james.api.protocol.ExtensibleHandler#getMarkerInterfaces()
     */
    public List getMarkerInterfaces() {
        List classes = new LinkedList();
        classes.add(DataLineFilter.class);
        return classes;
    }


    /**
     * @see org.apache.james.api.protocol.ExtensibleHandler#wireExtensions(java.lang.Class, java.util.List)
     */
    public void wireExtensions(Class interfaceName, List extension) throws WiringException {
        if (DataLineFilter.class.equals(interfaceName)) {

            LineHandler lineHandler = new DataConsumerLineHandler();
            for (int i = extension.size() - 1; i >= 0; i--) {
                lineHandler = new DataLineFilterWrapper((DataLineFilter) extension.get(i), lineHandler);
            }

            this.lineHandler = lineHandler;
        }
    }

    protected SMTPResponse doDATAFilter(SMTPSession session, String argument) {
        if ((argument != null) && (argument.length() > 0)) {
            return new SMTPResponse(SMTPRetCode.SYNTAX_ERROR_COMMAND_UNRECOGNIZED, DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.DELIVERY_INVALID_ARG)+" Unexpected argument provided with DATA command");
        }
        if (!session.getState().containsKey(SMTPSession.SENDER)) {
            return new SMTPResponse(SMTPRetCode.BAD_SEQUENCE, DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.DELIVERY_OTHER)+" No sender specified");
        } else if (!session.getState().containsKey(SMTPSession.RCPT_LIST)) {
            return new SMTPResponse(SMTPRetCode.BAD_SEQUENCE, DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.DELIVERY_OTHER)+" No recipients specified");
        }
        return null;
    }
    
    protected LineHandler getLineHandler() {
    	return lineHandler;
    }

}
