package org.apache.james.smtpserver.mina;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.james.smtpserver.CommandHandler;
import org.apache.james.smtpserver.ConnectHandler;
import org.apache.james.smtpserver.SMTPConfiguration;
import org.apache.james.smtpserver.SMTPHandlerChain;
import org.apache.james.smtpserver.SMTPRequest;
import org.apache.james.smtpserver.SMTPResponse;
import org.apache.james.smtpserver.SMTPRetCode;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.smtpserver.core.UnknownCmdHandler;
import org.apache.james.socket.shared.AbstractCommandDispatcher;
import org.apache.james.socket.shared.ExtensibleHandler;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

public class SMTPCommandDispatcherIoHandler extends
        AbstractCommandDispatcher<CommandHandler> implements ExtensibleHandler,
        IoHandler {
    private final static String SMTP_SESSION = "com.googlecode.asyncmail.smtpserver.SMTPCommandDispatcherIoHandler.SMTP_SESSION";
    private final UnknownCmdHandler unknownCmdHandler = new UnknownCmdHandler();
    private final static String[] mandatoryCommands = { "MAIL", "RCPT", "QUIT" };
    private Log logger;
    private SMTPHandlerChain chain;
    private SMTPConfiguration conf;

    public SMTPCommandDispatcherIoHandler(SMTPHandlerChain chain,
            SMTPConfiguration conf) {
        this.chain = chain;
        this.conf = conf;

    }

    /**
     * @see org.apache.james.socket.shared.AbstractCommandDispatcher#getLog()
     */
    protected Log getLog() {
        return logger;
    }

    @Override
    protected List<String> getMandatoryCommands() {
        return Arrays.asList(mandatoryCommands);
    }

    /**
     * @see org.apache.james.socket.shared.AbstractCommandDispatcher#getUnknownCommandHandler()
     */
    protected CommandHandler getUnknownCommandHandler() {
        return unknownCmdHandler;
    }

    /**
     * @see org.apache.james.socket.shared.AbstractCommandDispatcher#getUnknownCommandHandlerIdentifier()
     */
    protected String getUnknownCommandHandlerIdentifier() {
        return UnknownCmdHandler.UNKNOWN_COMMAND;
    }

    /**
     * @see org.apache.james.socket.shared.ExtensibleHandler#getMarkerInterfaces()
     */
    public List<Class<?>> getMarkerInterfaces() {
        List<Class<?>> res = new LinkedList<Class<?>>();
        res.add(CommandHandler.class);
        res.add(ConnectHandler.class);
        return res;
    }

    /**
     * @see org.apache.mina.core.service.IoHandler#exceptionCaught(org.apache.mina.core.session.IoSession,
     *      java.lang.Throwable)
     */
    public void exceptionCaught(IoSession session, Throwable exception)
            throws Exception {
        logger.error("Caught exception: " + session.getCurrentWriteMessage(),
                exception);
        // just close session
        session.close(true);
    }

    /**
     * @see org.apache.mina.core.service.IoHandler#messageReceived(org.apache.mina.core.session.IoSession,
     *      java.lang.Object)
     */
    public void messageReceived(IoSession session, Object message)
            throws Exception {
        if (message instanceof SMTPRequest) {
            SMTPRequest request = (SMTPRequest) message;
            SMTPSession smtpSession = (SMTPSession) session
                    .getAttribute(SMTP_SESSION);
            List<CommandHandler> commandHandlers = getCommandHandlers(request
                    .getCommand(), smtpSession);
            // fetch the command handlers registered to the command
            if (commandHandlers == null) {
                // end the session
                SMTPResponse resp = new SMTPResponse(SMTPRetCode.LOCAL_ERROR,
                        "Local configuration error: unable to find a command handler.");
                resp.setEndSession(true);
                session.write(resp);
            } else {
                int count = commandHandlers.size();
                for (int i = 0; i < count; i++) {
                    SMTPResponse response = commandHandlers.get(i).onCommand(
                            smtpSession, request);

                    // if the response is received, stop processing of command
                    // handlers
                    if (response != null) {
                        session.write(response);
                        break;
                    }

                    // NOTE we should never hit this line, otherwise we ended
                    // the
                    // CommandHandlers with
                    // no responses.
                    // (The note is valid for i == count-1)
                }

            }

        } else {
            logger.error("Invalid message object");
        }

    }

    /**
     * Not implemented
     */
    public void messageSent(IoSession session, Object message) throws Exception {
        // Nothing todo here

    }

    /**
     * Not implemented
     */
    public void sessionClosed(IoSession session) throws Exception {
        // Nothing todo here

    }

    /**
     * @see org.apache.mina.core.service.IoHandler#sessionCreated(org.apache.mina.core.session.IoSession)
     */
    public void sessionCreated(IoSession session) throws Exception {
        // Add attributes
        SMTPSession smtpSession = new SMTPSessionImpl(conf, logger, session);
        session.setAttribute(SMTP_SESSION,smtpSession);
    }

    /**
     * @see org.apache.mina.core.service.IoHandler#sessionIdle(org.apache.mina.core.session.IoSession,
     *      org.apache.mina.core.session.IdleStatus)
     */
    public void sessionIdle(IoSession session, IdleStatus status)
            throws Exception {
        logger.debug("Connection timed out");
        session.write("Connection timeout");
    }

    /**
     * @see org.apache.mina.core.service.IoHandler#sessionOpened(org.apache.mina.core.session.IoSession)
     */
    public void sessionOpened(IoSession session) throws Exception {
        List<ConnectHandler> connectHandlers = chain
                .getHandlers(ConnectHandler.class);
      
        if (connectHandlers != null) {
            for (int i = 0; i < connectHandlers.size(); i++) {
                connectHandlers.get(i).onConnect(
                        (SMTPSession) session.getAttribute(SMTP_SESSION));
            }
        }
    
    }
}
