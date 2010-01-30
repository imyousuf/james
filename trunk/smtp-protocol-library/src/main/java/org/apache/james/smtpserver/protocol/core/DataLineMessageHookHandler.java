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

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.james.api.protocol.ExtensibleHandler;
import org.apache.james.api.protocol.LineHandler;
import org.apache.james.api.protocol.WiringException;
import org.apache.james.dsn.DSNStatus;
import org.apache.james.smtpserver.protocol.MailEnvelopeImpl;
import org.apache.james.smtpserver.protocol.SMTPResponse;
import org.apache.james.smtpserver.protocol.SMTPRetCode;
import org.apache.james.smtpserver.protocol.SMTPSession;
import org.apache.james.smtpserver.protocol.hook.HookResult;
import org.apache.james.smtpserver.protocol.hook.HookResultHook;
import org.apache.james.smtpserver.protocol.hook.MessageHook;
import org.apache.mailet.Mail;

public final class DataLineMessageHookHandler implements DataLineFilter, ExtensibleHandler {

    
    private List messageHandlers;
    
    private List rHooks;
    

    /*
     * (non-Javadoc)
     * @see org.apache.james.smtpserver.protocol.core.DataLineFilter#onLine(org.apache.james.smtpserver.protocol.SMTPSession, byte[], org.apache.james.api.protocol.LineHandler)
     */
    public void onLine(SMTPSession session, byte[] line, LineHandler<SMTPSession> next) {
        MailEnvelopeImpl env = (MailEnvelopeImpl) session.getState().get(DataCmdHandler.MAILENV);
        OutputStream out = env.getMessageOutputStream();
        try {
            // 46 is "."
            // Stream terminated
            if (line.length == 3 && line[0] == 46) {
                out.flush();
                out.close();
                
                processExtensions(session, env);
                session.popLineHandler();

            // DotStuffing.
            } else if (line[0] == 46 && line[1] == 46) {
                out.write(line,1,line.length-1);
            // Standard write
            } else {
                // TODO: maybe we should handle the Header/Body recognition here
                // and if needed let a filter to cache the headers to apply some
                // transformation before writing them to output.
                out.write(line);
            }
            out.flush();
        } catch (IOException e) {
            SMTPResponse response;
            response = new SMTPResponse(SMTPRetCode.LOCAL_ERROR,DSNStatus.getStatus(DSNStatus.TRANSIENT,
                            DSNStatus.UNDEFINED_STATUS) + " Error processing message: " + e.getMessage());
            
            session.getLogger().error(
                    "Unknown error occurred while processing DATA.", e);
            session.writeResponse(response);
            return;
        }
    }


    /**
     * @param session
     */
    private void processExtensions(SMTPSession session, MailEnvelopeImpl mail) {
        if(mail != null && mail instanceof Mail && messageHandlers != null) {
            try {
                int count = messageHandlers.size();
                for(int i =0; i < count; i++) {
                    Object rawHandler =  messageHandlers.get(i);
                    session.getLogger().debug("executing message handler " + rawHandler);
                    HookResult hRes = ((MessageHook)rawHandler).onMessage(session, mail);
                    
                    if (rHooks != null) {
                        for (int i2 = 0; i2 < rHooks.size(); i2++) {
                            Object rHook = rHooks.get(i2);
                            session.getLogger().debug("executing hook " + rHook);
                            hRes = ((HookResultHook) rHook).onHookResult(session, hRes, rawHandler);
                        }
                    }
                    
                    SMTPResponse response = AbstractHookableCmdHandler.calcDefaultSMTPResponse(hRes);
                    
                    //if the response is received, stop processing of command handlers
                    if(response != null) {
                        session.writeResponse(response);
                        break;
                    }
                }
            } finally {
               
                //do the clean up
                session.resetState();
            }
        }
    }
    
    /**
     * @see org.apache.james.api.protocol.ExtensibleHandler#wireExtensions(java.lang.Class, java.util.List)
     */
    @SuppressWarnings("unchecked")
    public void wireExtensions(Class interfaceName, List extension) throws WiringException {
        if (MessageHook.class.equals(interfaceName)) {
            this.messageHandlers = extension;
            if (messageHandlers.size() == 0) {
                throw new WiringException("No messageHandler configured");
            }
        } else if (HookResultHook.class.equals(interfaceName)) {
            this.rHooks = extension;
        }
    }

    /**
     * @see org.apache.james.api.protocol.ExtensibleHandler#getMarkerInterfaces()
     */
    public List<Class<?>> getMarkerInterfaces() {
        List<Class<?>> classes = new LinkedList<Class<?>>();
        classes.add(MessageHook.class);
        classes.add(HookResultHook.class);
        return classes;
    }

}