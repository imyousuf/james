package org.apache.james.smtpserver.netty;

import org.apache.commons.logging.Log;
import org.apache.james.lifecycle.LifecycleUtil;
import org.apache.james.protocols.api.ProtocolHandlerChain;
import org.apache.james.protocols.api.ProtocolSession;
import org.apache.james.protocols.smtp.SMTPConfiguration;
import org.apache.james.protocols.smtp.SMTPResponse;
import org.apache.james.protocols.smtp.SMTPRetCode;
import org.apache.james.protocols.smtp.SMTPSession;
import org.apache.james.smtpserver.SMTPConstants;
import org.apache.james.socket.netty.AbstractChannelUpstreamHandler;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;

public class SMTPChannelUpstreamHandler extends AbstractChannelUpstreamHandler{
    private Log logger;
    private SMTPConfiguration conf;

    public SMTPChannelUpstreamHandler(ProtocolHandlerChain chain,
            SMTPConfiguration conf, Log logger) {
        super(chain);
        this.conf = conf;
        this.logger = logger;
    }
    
    @Override
    protected ProtocolSession createSession(ChannelHandlerContext ctx) throws Exception {
        SMTPSession smtpSession= new SMTPNettySession(conf, logger, ctx);
           
        return smtpSession;
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        logger.info("Dispose objects while closing channel " + ctx.getChannel().getId());
        cleanup(ctx.getChannel());
        super.channelDisconnected(ctx, e);
    }

    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {        
        Channel channel = ctx.getChannel();
        if (channel.isConnected()) {
            ctx.getChannel().write(new SMTPResponse(SMTPRetCode.LOCAL_ERROR, "Unable to process smtp request"));
        }
        cleanup(channel);
        channel.close();
        super.exceptionCaught(ctx, e);
    }

    private void cleanup(Channel channel) {
        // Make sure we dispose everything on exit on session close
        SMTPSession smtpSession = (SMTPSession) attributes.get(channel);
        
        if (smtpSession != null) {
            LifecycleUtil.dispose(smtpSession.getState().get(SMTPConstants.MAIL));
            LifecycleUtil.dispose(smtpSession.getState().get(SMTPConstants.DATA_MIMEMESSAGE_STREAMSOURCE));
            LifecycleUtil.dispose(smtpSession.getState().get(SMTPConstants.DATA_MIMEMESSAGE_OUTPUTSTREAM));
        }
    }
    
}
