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
package org.apache.james.smtpserver.core;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.smtpserver.hook.HookResult;
import org.apache.james.smtpserver.hook.HookReturnCode;
import org.apache.james.smtpserver.hook.RcptHook;
import org.apache.james.util.mail.SMTPRetCode;
import org.apache.james.util.mail.dsn.DSNStatus;
import org.apache.mailet.MailAddress;

/**
 * Handler which check for authenticated users
 */
public class AuthRequiredToRelayRcptHook extends AbstractLogEnabled implements
        RcptHook {

    /**
     * @see org.apache.james.smtpserver.hook.RcptHook#doRcpt(org.apache.james.smtpserver.SMTPSession,
     *      org.apache.mailet.MailAddress, org.apache.mailet.MailAddress)
     */
    public HookResult doRcpt(SMTPSession session, MailAddress sender,
            MailAddress rcpt) {
        if (!session.isRelayingAllowed()) {
            String toDomain = rcpt.getHost();
            if (!session.getConfigurationData().getMailServer().isLocalServer(toDomain)) {
                if (session.isAuthSupported()) {
                    if (session.getUser() == null) {
                        return new HookResult(HookReturnCode.DENY,
                                SMTPRetCode.AUTH_REQUIRED, DSNStatus.getStatus(
                                        DSNStatus.PERMANENT,
                                        DSNStatus.SECURITY_AUTH)
                                        + " Authentication Required");
                    }
                } else {
                    return new HookResult(
                            HookReturnCode.DENY,
                            // sendmail returns 554 (SMTPRetCode.TRANSACTION_FAILED).
                            // it is not clear in RFC wether it is better to use 550 or 554.
                            SMTPRetCode.MAILBOX_PERM_UNAVAILABLE,
                            DSNStatus.getStatus(DSNStatus.PERMANENT,
                                    DSNStatus.SECURITY_AUTH)
                                    + " Requested action not taken: relaying denied");
                }
            }

        }
        return new HookResult(HookReturnCode.DECLINED);
    }

}
