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

package org.apache.james.smtpserver.mina;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.net.ssl.SSLContext;

import org.apache.commons.logging.Log;
import org.apache.james.smtpserver.LineHandler;
import org.apache.james.smtpserver.SMTPConfiguration;
import org.apache.james.smtpserver.SMTPResponse;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.smtpserver.mina.filter.FilterLineHandlerAdapter;
import org.apache.james.smtpserver.mina.filter.TarpitFilter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.ssl.SslFilter;

public class SMTPSessionImpl implements SMTPSession {

        private static Random random = new Random();

        private boolean relayingAllowed;

        private String smtpID;

        private Map<String, Object> connectionState;

        private SMTPConfiguration theConfigData;

        private InetSocketAddress socketAddress;

        private String user;

        private IoSession session;

        private int lineHandlerCount = 0;

        private Log logger;
        
        private SSLContext context;

        public SMTPSessionImpl(SMTPConfiguration theConfigData,
                Log logger, IoSession session, SSLContext context) {
            this.theConfigData = theConfigData;
            this.session = session;
            connectionState = new HashMap<String, Object>();
            smtpID = random.nextInt(1024) + "";

            this.socketAddress = (InetSocketAddress) session.getRemoteAddress();
            relayingAllowed = theConfigData.isRelayingAllowed(getRemoteIPAddress());
            session.setAttribute(FilterLineHandlerAdapter.SMTP_SESSION, this);
            this.logger = logger;
            this.context = context;
        }

        public SMTPSessionImpl(SMTPConfiguration theConfigData,
                Log logger, IoSession session) {
            this(theConfigData,logger,session,null);
        }
        /**
         * @see org.apache.james.smtpserver.SMTPSession#getConnectionState()
         */
        public Map<String, Object> getConnectionState() {
            return connectionState;
        }

        /**
         * @see org.apache.james.socket.shared.TLSSupportedSession#getRemoteHost()
         */
        public String getRemoteHost() {
            return socketAddress.getHostName();
        }

        /**
         * @see org.apache.james.socket.shared.TLSSupportedSession#getRemoteIPAddress()
         */
        public String getRemoteIPAddress() {
            return socketAddress.getAddress().getHostAddress();
        }
        
        /**
         * @see org.apache.james.smtpserver.SMTPSession#getSessionID()
         */
        public String getSessionID() {
            return smtpID;
        }

        /**
         * @see org.apache.james.smtpserver.SMTPSession#getState()
         */
        @SuppressWarnings("unchecked")
        public Map<String, Object> getState() {
            Map<String, Object> res = (Map<String, Object>) getConnectionState()
                    .get(SMTPSession.SESSION_STATE_MAP);
            if (res == null) {
                res = new HashMap<String, Object>();
                getConnectionState().put(SMTPSession.SESSION_STATE_MAP, res);
            }
            return res;
        }

        /**
         * @see org.apache.james.socket.shared.TLSSupportedSession#getUser()
         */
        public String getUser() {
            return user;
        }

        /**
         * @see org.apache.james.smtpserver.SMTPSession#isRelayingAllowed()
         */
        public boolean isRelayingAllowed() {
            return relayingAllowed;
        }

        /**
         * @see org.apache.james.smtpserver.SMTPSession#resetState()
         */
        public void resetState() {
            // remember the ehlo mode between resets
            Object currentHeloMode = getState().get(CURRENT_HELO_MODE);

            getState().clear();

            // start again with the old helo mode
            if (currentHeloMode != null) {
                getState().put(CURRENT_HELO_MODE, currentHeloMode);
            }
        }

        /**
         * @see org.apache.james.socket.shared.TLSSupportedSession#setUser(java.lang.String)
         */
        public void setUser(String user) {
            this.user = user;
        }

        public IoSession getIoSession() {
            return session;
        }

        /**
         * @see org.apache.james.smtpserver.SMTPSession#popLineHandler()
         */
        public void popLineHandler() {
            getIoSession().getFilterChain()
                    .remove("lineHandler" + lineHandlerCount);
            lineHandlerCount--;
        }

        /**
         * @see org.apache.james.smtpserver.SMTPSession#pushLineHandler(org.apache.james.smtpserver.LineHandler)
         */
        public void pushLineHandler(LineHandler overrideCommandHandler) {
            lineHandlerCount++;
            getIoSession().getFilterChain().addAfter("protocolCodecFactory",
                    "lineHandler" + lineHandlerCount,
                    new FilterLineHandlerAdapter(overrideCommandHandler));
        }

        /**
         * @see org.apache.james.smtpserver.SMTPSession#writeSMTPResponse(org.apache.james.smtpserver.SMTPResponse)
         */
        public void writeSMTPResponse(SMTPResponse response) {
            getIoSession().write(response);
        }


        /**
         * @see org.apache.james.smtpserver.SMTPSession#getHelloName()
         */
        public String getHelloName() {
            return theConfigData.getHelloName();
        }


        /**
         * @see org.apache.james.smtpserver.SMTPSession#getMaxMessageSize()
         */
        public long getMaxMessageSize() {
            return theConfigData.getMaxMessageSize();
        }


        /**
         * @see org.apache.james.smtpserver.SMTPSession#getRcptCount()
         */
        @SuppressWarnings("unchecked")
        public int getRcptCount() {
            int count = 0;

            // check if the key exists
            if (getState().get(SMTPSession.RCPT_LIST) != null) {
                count = ((Collection) getState().get(SMTPSession.RCPT_LIST)).size();
            }

            return count;
        }

        /**
         * @see org.apache.james.smtpserver.SMTPSession#getSMTPGreeting()
         */
        public String getSMTPGreeting() {
            return theConfigData.getSMTPGreeting();
        }


        /**
         * @see org.apache.james.smtpserver.SMTPSession#isAuthSupported()
         */
        public boolean isAuthSupported() {
            return theConfigData.isAuthRequired(socketAddress.getAddress().getHostAddress());
        }


        /**
         * @see org.apache.james.smtpserver.SMTPSession#setRelayingAllowed(boolean)
         */
        public void setRelayingAllowed(boolean relayingAllowed) {
            this.relayingAllowed = relayingAllowed;
        }


        /**
         * @see org.apache.james.smtpserver.SMTPSession#sleep(long)
         */
        public void sleep(long ms) {
            session.getFilterChain().addAfter("connectionFilter", "tarpitFilter",new TarpitFilter(ms));
        }


        /**
         * @see org.apache.james.smtpserver.SMTPSession#useAddressBracketsEnforcement()
         */
        public boolean useAddressBracketsEnforcement() {
            return theConfigData.useAddressBracketsEnforcement();
        }

        /**
         * @see org.apache.james.smtpserver.SMTPSession#useHeloEhloEnforcement()
         */
        public boolean useHeloEhloEnforcement() {
            return theConfigData.useHeloEhloEnforcement();
        }


        /**
         * @see org.apache.james.socket.shared.TLSSupportedSession#isStartTLSSupported()
         */
        public boolean isStartTLSSupported() {
            return context != null;
        }

        /**
         * @see org.apache.james.socket.shared.TLSSupportedSession#isTLSStarted()
         */
        public boolean isTLSStarted() {
            return session.getFilterChain().contains("sslFilter");
        }

        /**
         * @see org.apache.james.socket.shared.TLSSupportedSession#startTLS()
         */
        public void startTLS() throws IOException {
            session.suspendRead();
            SslFilter filter = new SslFilter(context);
            resetState();
            session.getFilterChain().addFirst("sslFilter", filter);
            session.resumeRead();
        }


        /**
         * @see org.apache.james.socket.shared.LogEnabledSession#getLogger()
         */
        public Log getLogger() {
            return logger;
        }


}
