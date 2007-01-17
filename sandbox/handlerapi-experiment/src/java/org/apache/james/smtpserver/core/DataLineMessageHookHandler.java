/**
 * 
 */
package org.apache.james.smtpserver.core;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import javax.mail.MessagingException;

import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.core.MailImpl;
import org.apache.james.core.MimeMessageCopyOnWriteProxy;
import org.apache.james.core.MimeMessageInputStreamSource;
import org.apache.james.smtpserver.ExtensibleHandler;
import org.apache.james.smtpserver.LineHandler;
import org.apache.james.smtpserver.SMTPResponse;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.smtpserver.WiringException;
import org.apache.james.smtpserver.hook.HookResult;
import org.apache.james.smtpserver.hook.HookResultHook;
import org.apache.james.smtpserver.hook.MessageHook;
import org.apache.james.util.mail.SMTPRetCode;
import org.apache.james.util.mail.dsn.DSNStatus;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

public final class DataLineMessageHookHandler extends AbstractLogEnabled implements DataLineFilter, ExtensibleHandler {

    private List messageHandlers;
    
    private List rHooks;
    
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
                    new MailImpl(session.getConfigurationData().getMailServer().getId(),
                                 (MailAddress) session.getState().get(SMTPSession.SENDER),
                                 recipientCollection);
                MimeMessageCopyOnWriteProxy mimeMessageCopyOnWriteProxy = null;
                try {
                    mimeMessageCopyOnWriteProxy = new MimeMessageCopyOnWriteProxy(mmiss);
                    mail.setMessage(mimeMessageCopyOnWriteProxy);
                    
                    processExtensions(session, mail);
                    
                    session.popLineHandler();
                    
    
                } catch (MessagingException e) {
                    // TODO probably return a temporary problem
                    getLogger().info("Unexpected error handling DATA stream",e);
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
                // transormation before writing them to output.
                out.write(line);
            }
            out.flush();
        } catch (IOException e) {
            SMTPResponse response;
            response = new SMTPResponse(SMTPRetCode.LOCAL_ERROR,DSNStatus.getStatus(DSNStatus.TRANSIENT,
                            DSNStatus.UNDEFINED_STATUS) + " Error processing message: " + e.getMessage());
            
            getLogger().error(
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
                    getLogger().debug("executing message handler " + rawHandler);
                    HookResult hRes = ((MessageHook)rawHandler).onMessage(session, (Mail) mail);
                    
                    if (rHooks != null) {
                        for (int i2 = 0; i2 < rHooks.size(); i2++) {
                            Object rHook = rHooks.get(i2);
                            getLogger().debug("executing hook " + rHook);
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
     * @throws WiringException 
     * @see org.apache.james.smtpserver.ExtensibleHandler#wireExtensions(java.lang.Class, java.util.List)
     */
    public void wireExtensions(Class interfaceName, List extension) throws WiringException {
        if (MessageHook.class.equals(interfaceName)) {
            this.messageHandlers = extension;
            if (messageHandlers.size() == 0) {
                if (getLogger().isErrorEnabled()) {
                    getLogger().error(
                                    "No messageHandler configured. Check that SendMailHandler is configured in the SMTPHandlerChain");
                }
                throw new WiringException("No messageHandler configured");
            }
        } else if (HookResultHook.class.equals(interfaceName)) {
            this.rHooks = extension;
        }
    }

    /**
     * @see org.apache.james.smtpserver.ExtensibleHandler#getMarkerInterfaces()
     */
    public List getMarkerInterfaces() {
        List classes = new LinkedList();
        classes.add(MessageHook.class);
        classes.add(HookResultHook.class);
        return classes;
    }

}