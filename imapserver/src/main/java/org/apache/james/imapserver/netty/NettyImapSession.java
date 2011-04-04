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
package org.apache.james.imapserver.netty;

import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.apache.james.imap.api.ImapSessionState;
import org.apache.james.imap.api.process.ImapLineHandler;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.api.process.SelectedMailbox;
import org.apache.james.protocols.impl.SessionLog;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.compression.ZlibDecoder;
import org.jboss.netty.handler.codec.compression.ZlibEncoder;
import org.jboss.netty.handler.codec.compression.ZlibWrapper;
import org.jboss.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;

public class NettyImapSession implements ImapSession, NettyConstants {

    private ImapSessionState state = ImapSessionState.NON_AUTHENTICATED;
    private SelectedMailbox selectedMailbox;
    private Map<String, Object> attributesByKey = new HashMap<String, Object>();
    private SSLContext sslContext;
    private String[] enabledCipherSuites;
    private boolean compress;
    private SessionLog log;
    private ChannelHandlerContext context;
    private int handlerCount;

    public NettyImapSession(ChannelHandlerContext context, Logger log, SSLContext sslContext, String[] enabledCipherSuites, boolean compress) {
        this.context = context;
        this.log = new SessionLog(context.getChannel().getId() + "", log);
        this.sslContext = sslContext;
        this.enabledCipherSuites = enabledCipherSuites;
        this.compress = compress;
    }

    /**
     * Return the wrapped {@link Channel} which this {@link ImapSession} is
     * bound to
     * 
     * @return channel
     */
    public Channel getChannel() {
        return context.getChannel();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.imap.api.process.ImapSession#logout()
     */
    public void logout() {
        closeMailbox();
        state = ImapSessionState.LOGOUT;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.imap.api.process.ImapSession#authenticated()
     */
    public void authenticated() {
        this.state = ImapSessionState.AUTHENTICATED;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.imap.api.process.ImapSession#deselect()
     */
    public void deselect() {
        this.state = ImapSessionState.AUTHENTICATED;
        closeMailbox();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.api.process.ImapSession#selected(org.apache.james
     * .imap.api.process.SelectedMailbox)
     */
    public void selected(SelectedMailbox mailbox) {
        this.state = ImapSessionState.SELECTED;
        closeMailbox();
        this.selectedMailbox = mailbox;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.imap.api.process.ImapSession#getSelected()
     */
    public SelectedMailbox getSelected() {
        return this.selectedMailbox;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.imap.api.process.ImapSession#getState()
     */
    public ImapSessionState getState() {
        return this.state;
    }

    private void closeMailbox() {
        if (selectedMailbox != null) {
            selectedMailbox.deselect();
            selectedMailbox = null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.api.process.ImapSession#getAttribute(java.lang.
     * String)
     */
    public Object getAttribute(String key) {
        final Object result = attributesByKey.get(key);
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.api.process.ImapSession#setAttribute(java.lang.
     * String, java.lang.Object)
     */
    public void setAttribute(String key, Object value) {
        if (value == null) {
            attributesByKey.remove(key);
        } else {
            attributesByKey.put(key, value);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.imap.api.process.ImapSession#startTLS()
     */
    public boolean startTLS() {
        if (supportStartTLS() == false)
            return false;
        context.getChannel().setReadable(false);

        SslHandler filter = new SslHandler(sslContext.createSSLEngine(), false);
        filter.getEngine().setUseClientMode(false);
        if (enabledCipherSuites != null && enabledCipherSuites.length > 0) {
            filter.getEngine().setEnabledCipherSuites(enabledCipherSuites);
        }
        context.getPipeline().addFirst(SSL_HANDLER, filter);

        context.getChannel().setReadable(true);

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.imap.api.process.ImapSession#supportStartTLS()
     */
    public boolean supportStartTLS() {
        return sslContext != null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.api.process.ImapSession#isCompressionSupported()
     */
    public boolean isCompressionSupported() {
        return compress;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.imap.api.process.ImapSession#startCompression()
     */
    public boolean startCompression() {
        if (isCompressionSupported() == false)
            return false;

        context.getChannel().setReadable(false);
        ZlibDecoder decoder = new ZlibDecoder(ZlibWrapper.NONE);
        ZlibEncoder encoder = new ZlibEncoder(ZlibWrapper.NONE, 5);

        // Check if we have the SslHandler in the pipeline already
        // if so we need to move the compress encoder and decoder
        // behind it in the chain
        // See JAMES-1186
        if (context.getPipeline().get(SSL_HANDLER) == null) {
            context.getPipeline().addFirst(ZLIB_DECODER, decoder);
            context.getPipeline().addFirst(ZLIB_ENCODER, encoder);
        } else {
            context.getPipeline().addAfter(SSL_HANDLER, ZLIB_DECODER, decoder);
            context.getPipeline().addAfter(SSL_HANDLER, ZLIB_ENCODER, encoder);
        }

        context.getChannel().setReadable(true);

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.imap.api.process.ImapSession#pushLineHandler(org.apache
     * .james.imap.api.process.ImapLineHandler)
     */
    public void pushLineHandler(ImapLineHandler lineHandler) {
        context.getPipeline().addBefore("requestDecoder", "lineHandler" + handlerCount++, new ImapLineHandlerAdapter(lineHandler));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.imap.api.process.ImapSession#popLineHandler()
     */
    public void popLineHandler() {
        context.getPipeline().remove("lineHandler" + --handlerCount);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.imap.api.process.ImapSession#getLog()
     */
    public Logger getLog() {
        return log;
    }

}
