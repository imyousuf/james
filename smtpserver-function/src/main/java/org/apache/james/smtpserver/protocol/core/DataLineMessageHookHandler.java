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

import javax.annotation.Resource;
import javax.mail.MessagingException;

import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.api.protocol.ExtensibleHandler;
import org.apache.james.api.protocol.LogEnabled;
import org.apache.james.api.protocol.WiringException;
import org.apache.james.core.MailImpl;
import org.apache.james.core.MimeMessageCopyOnWriteProxy;
import org.apache.james.core.MimeMessageInputStreamSource;
import org.apache.james.dsn.DSNStatus;
import org.apache.james.services.MailServer;
import org.apache.james.smtpserver.protocol.LineHandler;
import org.apache.james.smtpserver.protocol.SMTPResponse;
import org.apache.james.smtpserver.protocol.SMTPRetCode;
import org.apache.james.smtpserver.protocol.SMTPSession;
import org.apache.james.smtpserver.protocol.hook.HookResult;
import org.apache.james.smtpserver.protocol.hook.HookResultHook;
import org.apache.james.smtpserver.protocol.hook.MessageHook;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

public final class DataLineMessageHookHandler implements DataLineFilter, ExtensibleHandler, LogEnabled {

    /** This log is the fall back shared by all instances */
    private static final Log FALLBACK_LOG = LogFactory.getLog(DataLineMessageHookHandler.class);
    
    /** Non context specific log should only be used when no context specific log is available */
    private Log serviceLog = FALLBACK_LOG;
    
    private List messageHandlers;
    
    private List rHooks;
    
    private MailServer mailServer;
    
    /**
     * Gets the mail server.
     * @return the mailServer
     */
    public final MailServer getMailServer() {
        return mailServer;
    }

    /**
     * Sets the mail server.
     * @param mailServer the mailServer to set
     */
    @Resource(name="James")
    public final void setMailServer(MailServer mailServer) {
        this.mailServer = mailServer;
    }
    
    /**
     * @see org.apache.james.smtpserver.protocol.core.DataLineFilter#onLine(org.apache.james.smtpserver.protocol.SMTPSession, byte[], org.apache.james.smtpserver.protocol.LineHandler)
     */
    public void onLine(SMTPSession session, byte[] line, LineHandler next) {
        MimeMessageInputStreamSource mmiss = (MimeMessageInputStreamSource) session.getState().get(DataCmdHandler.DATA_MIMEMESSAGE_STREAMSOURCE);
        OutputStream out = (OutputStream)  session.getState().get(DataCmdHandler.DATA_MIMEMESSAGE_OUTPUTSTREAM);
        try {
            // 46 is "."
            // Stream terminated
            if (line.length == 3 && line[0] == 46) {
                out.flush();
                out.close();
                
                List recipientCollection = (List) session.getState().get(SMTPSession.RCPT_LIST);
                MailImpl mail =
                    new MailImpl(mailServer.getId(),
                                 (MailAddress) session.getState().get(SMTPSession.SENDER),
                                 recipientCollection);
                MimeMessageCopyOnWriteProxy mimeMessageCopyOnWriteProxy = null;
                try {
                    mimeMessageCopyOnWriteProxy = new MimeMessageCopyOnWriteProxy(mmiss);
                    mail.setMessage(mimeMessageCopyOnWriteProxy);
                    
                    processExtensions(session, mail);
                    
                    session.popLineHandler();
                    //next.onLine(session, line);
    
                } catch (MessagingException e) {
                    // TODO probably return a temporary problem
                    session.getLogger().info("Unexpected error handling DATA stream",e);
                    session.writeSMTPResponse(new SMTPResponse(SMTPRetCode.LOCAL_ERROR, "Unexpected error handling DATA stream."));
                } finally {
                    ContainerUtil.dispose(mimeMessageCopyOnWriteProxy);
                    ContainerUtil.dispose(mmiss);
                    ContainerUtil.dispose(mail);
                }
    
                
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
            session.writeSMTPResponse(response);
            return;
        }
    }


    /**
     * @param session
     */
    private void processExtensions(SMTPSession session, Mail mail) {
        if(mail != null && mail instanceof Mail && messageHandlers != null) {
            try {
                int count = messageHandlers.size();
                for(int i =0; i < count; i++) {
                    Object rawHandler =  messageHandlers.get(i);
                    session.getLogger().debug("executing message handler " + rawHandler);
                    HookResult hRes = ((MessageHook)rawHandler).onMessage(session, (Mail) mail);
                    
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
                        session.writeSMTPResponse(response);
                        break;
                    }
                }
            } finally {
                // Dispose the mail object and remove it
                if(mail != null) {
                    ContainerUtil.dispose(mail);
                    mail = null;
                }
                //do the clean up
                session.resetState();
            }
        }
    }
    
    /**
     * @see org.apache.james.api.protocol.ExtensibleHandler#wireExtensions(java.lang.Class, java.util.List)
     */
    public void wireExtensions(Class interfaceName, List extension) throws WiringException {
        if (MessageHook.class.equals(interfaceName)) {
            this.messageHandlers = extension;
            if (messageHandlers.size() == 0) {
                if (serviceLog.isErrorEnabled()) {
                    serviceLog.error(
                                    "No messageHandler configured. Check that SendMailHandler is configured in the SMTPHandlerChain");
                }
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

    /**
     * Sets the service log.
     * Where available, a context sensitive log should be used.
     * @param Log not null
     */
    public void setLog(Log log) {
        this.serviceLog = log;
    }
}