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

package org.apache.james.smtpserver.protocol.core.fastfail;

import java.net.UnknownHostException;


import org.apache.james.dsn.DSNStatus;
import org.apache.james.smtpserver.protocol.DNSService;
import org.apache.james.smtpserver.protocol.SMTPRetCode;
import org.apache.james.smtpserver.protocol.SMTPSession;
import org.apache.james.smtpserver.protocol.hook.HeloHook;
import org.apache.james.smtpserver.protocol.hook.HookResult;
import org.apache.james.smtpserver.protocol.hook.HookReturnCode;
import org.apache.james.smtpserver.protocol.hook.RcptHook;
import org.apache.mailet.MailAddress;


/**
 * This CommandHandler can be used to reject not resolvable EHLO/HELO
 */
public class ResolvableEhloHeloHandler implements RcptHook, HeloHook {

    public final static String BAD_EHLO_HELO = "BAD_EHLO_HELO";

    protected DNSService dnsService = null;

    /**
     * Gets the DNS service.
     * @return the dnsService
     */
    public final DNSService getDNSService() {
        return dnsService;
    }

    /**
     * Sets the DNS service.
     * @param dnsService the dnsService to set
     */
    public final void setDNSService(DNSService dnsService) {
        this.dnsService = dnsService;
    }


    /**
     * Check if EHLO/HELO is resolvable
     * 
     * @param session
     *            The SMTPSession
     * @param argument
     *            The argument
     */
    protected void checkEhloHelo(SMTPSession session, String argument) {
        
        if (isBadHelo(session, argument)) {
            session.getState().put(BAD_EHLO_HELO, "true");
        }
    }
    
    /**
     * @param session the SMTPSession
     * @param argument the argument
     * @return true if the helo is bad.
     */
    protected boolean isBadHelo(SMTPSession session, String argument) {
        // try to resolv the provided helo. If it can not resolved do not
        // accept it.
        try {
        	dnsService.getByName(argument);
        } catch (UnknownHostException e) {
            return true;
        }
        return false;
        
    }

    /**
     * @see org.apache.james.smtpserver.core.filter.fastfail.AbstractJunkHandler#check(org.apache.james.smtpserver.protocol.SMTPSession)
     */
    protected boolean check(SMTPSession session,MailAddress rcpt) {
        // not reject it
        if (session.getState().get(BAD_EHLO_HELO) == null) {
            return false;
        }

        return true;
    }

    /**
     * @see org.apache.james.smtpserver.protocol.hook.RcptHook#doRcpt(org.apache.james.smtpserver.protocol.SMTPSession, org.apache.mailet.MailAddress, org.apache.mailet.MailAddress)
     */
    public HookResult doRcpt(SMTPSession session, MailAddress sender, MailAddress rcpt) {
        if (check(session,rcpt)) {
            return new HookResult(HookReturnCode.DENY,SMTPRetCode.SYNTAX_ERROR_ARGUMENTS,DSNStatus.getStatus(DSNStatus.PERMANENT, DSNStatus.DELIVERY_INVALID_ARG)
                    + " Provided EHLO/HELO " + session.getState().get(SMTPSession.CURRENT_HELO_NAME) + " can not resolved.");
        } else {
            return new HookResult(HookReturnCode.DECLINED);
        }
    }

    /**
     * @see org.apache.james.smtpserver.protocol.hook.HeloHook#doHelo(org.apache.james.smtpserver.protocol.SMTPSession, java.lang.String)
     */
    public HookResult doHelo(SMTPSession session, String helo) {
        checkEhloHelo(session, helo);
        return new HookResult(HookReturnCode.DECLINED);
    }

}
