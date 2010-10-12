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
package org.apache.james.lmtpserver.netty;

import javax.annotation.Resource;
import javax.net.ssl.SSLContext;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.protocols.api.ProtocolHandlerChain;
import org.apache.james.protocols.impl.AbstractSSLAwareChannelPipelineFactory;
import org.apache.james.protocols.smtp.SMTPConfiguration;
import org.apache.james.services.MailServer;
import org.apache.james.smtpserver.SMTPServerMBean;
import org.apache.james.smtpserver.netty.SMTPChannelUpstreamHandler;
import org.apache.james.smtpserver.netty.SMTPResponseEncoder;
import org.apache.james.socket.netty.AbstractConfigurableAsyncServer;
import org.apache.james.socket.netty.ConnectionCountHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

public class NioLMTPServer extends AbstractConfigurableAsyncServer implements SMTPServerMBean{

    /**
     * The maximum message size allowed by this SMTP server.  The default
     * value, 0, means no limit.
     */
    private long maxMessageSize = 0;
    private MailServer mailServer;
    private ProtocolHandlerChain handlerChain;
    private LMTPConfiguration lmtpConfig = new LMTPConfiguration();
    private String lmtpGreeting;
    private final ConnectionCountHandler countHandler = new ConnectionCountHandler();
    

    @Resource(name="mailserver")
    public final void setMailServer(MailServer mailServer) {
        this.mailServer = mailServer;
    }
    

    public void setProtocolHandlerChain(ProtocolHandlerChain handlerChain) {
        this.handlerChain = handlerChain;
    }

    
    @Override
    public int getDefaultPort() {
        return 24;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.socket.ServerMBean#getServiceType()
     */
    public String getServiceType() {
        return "LMTP Service";
    }
    
    public void doConfigure(final HierarchicalConfiguration configuration) throws ConfigurationException {
        if (isEnabled()) {
            HierarchicalConfiguration handlerConfiguration = configuration.configurationAt("handler");
           

            // get the message size limit from the conf file and multiply
            // by 1024, to put it in bytes
            maxMessageSize = handlerConfiguration.getLong( "maxmessagesize",maxMessageSize ) * 1024;
            if (maxMessageSize > 0) {
                getLogger().info("The maximum allowed message size is " + maxMessageSize + " bytes.");
            } else {
                getLogger().info("No maximum message size is enforced for this server.");
            }
            
            // get the lmtpGreeting
            lmtpGreeting = handlerConfiguration.getString("lmtpGreeting",null);


        }
    }
    
    @Override
    protected ChannelPipelineFactory createPipelineFactory(ChannelGroup group) {
        return new LMTPChannelPipelineFactory(getTimeout(), connectionLimit, connPerIP, group);
    }

    /**
     * A class to provide SMTP handler configuration to the handlers
     */
    public class LMTPConfiguration implements SMTPConfiguration {

        /**
         * @see org.apache.james.protocols.smtp.SMTPConfiguration#getHelloName()
         */
        public String getHelloName() {
            if (NioLMTPServer.this.getHelloName() == null) {
                return NioLMTPServer.this.mailServer.getHelloName();
            } else {
                return NioLMTPServer.this.getHelloName();
            }
        }

        /**
         * @see org.apache.james.protocols.smtp.SMTPConfiguration#getResetLength()
         */
        public int getResetLength() {
            return -1;
        }

        /**
         * @see org.apache.james.protocols.smtp.SMTPConfiguration#getMaxMessageSize()
         */
        public long getMaxMessageSize() {
            return NioLMTPServer.this.maxMessageSize;
        }

        /**
         * Relaying not allowed with LMTP
         */
        public boolean isRelayingAllowed(String remoteIP) {
            return false;
        }

        /**
         * No enforcement
         */
        public boolean useHeloEhloEnforcement() {
            return false;
        }


        /**
         * @see org.apache.james.protocols.smtp.SMTPConfiguration#getSMTPGreeting()
         */
        public String getSMTPGreeting() {
            return NioLMTPServer.this.lmtpGreeting;
        }

        /**
         * @see org.apache.james.protocols.smtp.SMTPConfiguration#useAddressBracketsEnforcement()
         */
        public boolean useAddressBracketsEnforcement() {
            return true;
        }

        /**
         * @see org.apache.james.protocols.smtp.SMTPConfiguration#isAuthRequired(java.lang.String)
         */
        public boolean isAuthRequired(String remoteIP) {
            return true;
        }

        /**
         * @see org.apache.james.protocols.smtp.SMTPConfiguration#isStartTLSSupported()
         */
        public boolean isStartTLSSupported() {
            return false;
        }
    }
    
    private final class LMTPChannelPipelineFactory extends AbstractSSLAwareChannelPipelineFactory {

        public LMTPChannelPipelineFactory(int timeout, int maxConnections,
                int maxConnectsPerIp, ChannelGroup group) {
            super(timeout, maxConnections, maxConnectsPerIp, group);
        }

        @Override
		public ChannelPipeline getPipeline() throws Exception {
			ChannelPipeline pipeLine = super.getPipeline();
			pipeLine.addBefore("coreHandler", "countHandler", countHandler);
			return pipeLine;
		}

		@Override
        protected SSLContext getSSLContext() {
            return null;
        }

        @Override
        protected boolean isSSLSocket() {
            return  false;
        }

        @Override
        protected OneToOneEncoder createEncoder() {
            return new SMTPResponseEncoder();
        }

        @Override
        protected ChannelUpstreamHandler createHandler() {
            return new SMTPChannelUpstreamHandler(handlerChain, lmtpConfig, getLogger(), getSSLContext());
        }
        
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.smtpserver.SMTPServerMBean#getAddressBracketsEnforcement()
     */
	public boolean getAddressBracketsEnforcement() {
		return lmtpConfig.useAddressBracketsEnforcement();
	}


	/*
	 * (non-Javadoc)
	 * @see org.apache.james.smtpserver.SMTPServerMBean#getHeloEhloEnforcement()
	 */
	public boolean getHeloEhloEnforcement() {
		return lmtpConfig.useHeloEhloEnforcement();
	}


	/*
	 * (non-Javadoc)
	 * @see org.apache.james.smtpserver.SMTPServerMBean#getMaximalMessageSize()
	 */
	public long getMaximalMessageSize() {
		return lmtpConfig.getMaxMessageSize();
	}


	/*
	 * (non-Javadoc)
	 * @see org.apache.james.socket.ServerMBean#getCurrentConnections()
	 */
	public int getCurrentConnections() {
		return countHandler.getCurrentConnectionCount();
	}


	/*
	 * (non-Javadoc)
	 * @see org.apache.james.protocols.smtp.SMTPServerMBean#getNetworkInterface()
	 */
	public String getNetworkInterface() {
		return "unknown";
	}

}
