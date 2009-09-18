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



package org.apache.james.smtpserver.core.filter.fastfail;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.dsn.DSNStatus;
import org.apache.james.jspf.core.DNSService;
import org.apache.james.jspf.core.exceptions.SPFErrorConstants;
import org.apache.james.jspf.executor.SPFResult;
import org.apache.james.jspf.impl.DefaultSPF;
import org.apache.james.jspf.impl.SPF;
import org.apache.james.smtpserver.Configurable;
import org.apache.james.smtpserver.SMTPRetCode;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.smtpserver.hook.HookResult;
import org.apache.james.smtpserver.hook.HookReturnCode;
import org.apache.james.smtpserver.hook.MailHook;
import org.apache.james.smtpserver.hook.MessageHook;
import org.apache.james.smtpserver.hook.RcptHook;
import org.apache.james.socket.LogEnabled;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

/**
 * SPFHandler which could be used to reject mail based on spf. The Following attributes
 * 
 * org.apache.james.smtpserver.spf.header - Holds the header which can be attached later
 * 
 * Sample Configuration: <br>
 * <br>
 * &lt;handler class="org.apache.james.smtpserver.core.SPFHandler" command="MAIL,RCPT"&gt;
 * &lt;blockSoftFail&gt;false&lt;/blockSoftFail&gt;
 * &lt;checkAuthNetworks&gt;false&lt/checkAuthNetworks&gt; 
 * &lt;/handler&gt;
 */
public class SPFHandler implements LogEnabled, MailHook, RcptHook, MessageHook, Configurable {
    
    /** This log is the fall back shared by all instances */
    private static final Log FALLBACK_LOG = LogFactory.getLog(SPFHandler.class);
    
    /** Non context specific log should only be used when no context specific log is available */
    private Log serviceLog = FALLBACK_LOG;

    public static final String SPF_BLOCKLISTED = "SPF_BLOCKLISTED";

    public static final String SPF_DETAIL = "SPF_DETAIL";

    public static final String SPF_TEMPBLOCKLISTED = "SPF_TEMPBLOCKLISTED";

    public final static String SPF_HEADER = "SPF_HEADER";

    public final static String SPF_HEADER_MAIL_ATTRIBUTE_NAME = "org.apache.james.spf.header";

    /**
     * If set to true the mail will also be rejected on a softfail
     */
    private boolean blockSoftFail = false;
    
    private boolean blockPermError = true;

    private boolean checkAuthNetworks = false;

    private SPF spf = new DefaultSPF(new SPFLogger());


    /**
     * Sets the service log.
     * Where available, a context sensitive log should be used.
     * @param Log not null
     */
    public void setLog(Log log) {
        this.serviceLog = log;
    }

    /**
     * @see org.apache.james.smtpserver.Configurable#configure(org.apache.commons.configuration.Configuration)
     */
    public void configure(Configuration handlerConfiguration)
            throws ConfigurationException {
        setBlockSoftFail(handlerConfiguration.getBoolean( "blockSoftFail", false));
        setBlockPermError(handlerConfiguration.getBoolean("blockPermError", true));
        setCheckAuthNetworks(handlerConfiguration.getBoolean("checkAuthNetworks",false));
    }
    

    /**
     * block the email on a softfail
     * 
     * @param blockSoftFail
     *            true or false
     */
    public void setBlockSoftFail(boolean blockSoftFail) {
        this.blockSoftFail = blockSoftFail;
    }
    
    /**
     * block the email on a permerror
     * 
     * @param blockPermError
     *            true or false
     */
    public void setBlockPermError(boolean blockPermError) {
        this.blockPermError = blockPermError;
    }

    /**
     * DNSService to use
     * 
     * @param dnsService The DNSService
     */
    public void setDNSService(DNSService dnsService) {
        spf = new SPF(dnsService, new SPFLogger());
    }

    /**
     * Set to true if AuthNetworks should be included in the EHLO check
     * 
     * @param checkAuthNetworks
     *            Set to true to enable
     */
    public void setCheckAuthNetworks(boolean checkAuthNetworks) {
        this.checkAuthNetworks = checkAuthNetworks;
    }


    /**
     * Calls a SPF check
     * 
     * @param session
     *            SMTP session object
     */
    private void doSPFCheck(SMTPSession session, MailAddress sender) {
        String heloEhlo = (String) session.getState().get(
                SMTPSession.CURRENT_HELO_NAME);

        // We have no Sender or HELO/EHLO yet return false
        if (sender == null || heloEhlo == null) {
            session.getLogger().info("No Sender or HELO/EHLO present");
        } else {
            // No checks for authorized cliends
            if (session.isRelayingAllowed() && checkAuthNetworks == false) {
                session.getLogger().info(
                        "Ipaddress " + session.getRemoteIPAddress()
                                + " is allowed to relay. Don't check it");
            } else {

                String ip = session.getRemoteIPAddress();

                SPFResult result = spf
                        .checkSPF(ip, sender.toString(), heloEhlo);

                String spfResult = result.getResult();

                String explanation = "Blocked - see: "
                        + result.getExplanation();

                // Store the header
                session.getState().put(SPF_HEADER, result.getHeaderText());

                session.getLogger().info(
                        "Result for " + ip + " - " + sender + " - " + heloEhlo
                                + " = " + spfResult);

                // Check if we should block!
                if ((spfResult.equals(SPFErrorConstants.FAIL_CONV))
                        || (spfResult.equals(SPFErrorConstants.SOFTFAIL_CONV) && blockSoftFail)
                        || (spfResult.equals(SPFErrorConstants.PERM_ERROR_CONV) && blockPermError)) {

                    if (spfResult.equals(SPFErrorConstants.PERM_ERROR_CONV)) {
                        explanation = "Block caused by an invalid SPF record";
                    }
                    session.getState().put(SPF_DETAIL, explanation);
                    session.getState().put(SPF_BLOCKLISTED, "true");

                } else if (spfResult.equals(SPFErrorConstants.TEMP_ERROR_CONV)) {
                    session.getState().put(SPF_TEMPBLOCKLISTED, "true");
                }
            }
        }


    }

    /**
     * @see org.apache.james.smtpserver.hook.MessageHook#onMessage(org.apache.james.smtpserver.SMTPSession, org.apache.mailet.Mail)
     */
    public HookResult onMessage(SMTPSession session, Mail mail) {
        // Store the spf header as attribute for later using
        mail.setAttribute(SPF_HEADER_MAIL_ATTRIBUTE_NAME, (String) session.getState().get(SPF_HEADER));
    
        return null;
    }
    
    /**
     * @see org.apache.james.smtpserver.hook.RcptHook#doRcpt(org.apache.james.smtpserver.SMTPSession, org.apache.mailet.MailAddress, org.apache.mailet.MailAddress)
     */
    public HookResult doRcpt(SMTPSession session, MailAddress sender, MailAddress rcpt) {
        if (!session.isRelayingAllowed()) {
            // Check if session is blocklisted
            if (session.getState().get(SPF_BLOCKLISTED)!= null) {
                return new HookResult(HookReturnCode.DENY,DSNStatus.getStatus(DSNStatus.PERMANENT, DSNStatus.SECURITY_AUTH) + " "
                    + session.getState().get(SPF_TEMPBLOCKLISTED));
            } else if (session.getState().get(SPF_TEMPBLOCKLISTED) != null) {
                return new HookResult(HookReturnCode.DENYSOFT, SMTPRetCode.LOCAL_ERROR,DSNStatus.getStatus(DSNStatus.TRANSIENT, DSNStatus.NETWORK_DIR_SERVER) + " "
                    + "Temporarily rejected: Problem on SPF lookup");
            }
        }
        return new HookResult(HookReturnCode.DECLINED);      
    }

    /**
     * @see org.apache.james.smtpserver.hook.MailHook#doMail(org.apache.james.smtpserver.SMTPSession, org.apache.mailet.MailAddress)
     */
    public HookResult doMail(SMTPSession session, MailAddress sender) {
        doSPFCheck(session,sender);
        return new HookResult(HookReturnCode.DECLINED);
    }
    
    /**
     * Adapts service log.
     */
    private final class SPFLogger implements org.apache.james.jspf.core.Logger {
        
        /**
         * @see org.apache.james.jspf.core.Logger#debug(String)
         */
        public void debug(String message) {
            serviceLog.debug(message);
        }

        /**
         * @see org.apache.james.jspf.core.Logger#debug(String, Throwable)
         */
        public void debug(String message, Throwable t) {
            serviceLog.debug(message, t);
        }

        /**
         * @see org.apache.james.jspf.core.Logger#error(String)
         */
        public void error(String message) {
            serviceLog.error(message);
        }

        /**
         * @see org.apache.james.jspf.core.Logger#error(String, Throwable)
         */
        public void error(String message, Throwable t) {
            serviceLog.error(message, t);
        }

        /**
         * @see org.apache.james.jspf.core.Logger#fatalError(String)
         */
        public void fatalError(String message) {
            serviceLog.fatal(message);
        }

        /**
         * @see org.apache.james.jspf.core.Logger#fatalError(String, Throwable)
         */
        public void fatalError(String message, Throwable t) {
            serviceLog.fatal(message, t);
        }

        /**
         * @see org.apache.james.jspf.core.Logger#info(String)
         */
        public void info(String message) {
            serviceLog.info(message);
        }

        /**
         * @see org.apache.james.jspf.core.Logger#info(String, Throwable)
         */
        public void info(String message, Throwable t) {
            serviceLog.info(message, t);
        }

        /**
         * @see org.apache.james.jspf.core.Logger#isDebugEnabled()
         */
        public boolean isDebugEnabled() {
            return serviceLog.isDebugEnabled();
        }

        /**
         * @see org.apache.james.jspf.core.Logger#isErrorEnabled()
         */
        public boolean isErrorEnabled() {
            return serviceLog.isErrorEnabled();
        }

        /**
         * @see org.apache.james.jspf.core.Logger#isFatalErrorEnabled()
         */
        public boolean isFatalErrorEnabled() {
            return serviceLog.isErrorEnabled();
        }

        /**
         * @see org.apache.james.jspf.core.Logger#isInfoEnabled()
         */
        public boolean isInfoEnabled() {
            return serviceLog.isInfoEnabled();
        }

        /**
         * @see org.apache.james.jspf.core.Logger#isWarnEnabled()
         */
        public boolean isWarnEnabled() {
            return serviceLog.isWarnEnabled();
        }

        /**
         * @see org.apache.james.jspf.core.Logger#warn(String)
         */
        public void warn(String message) {
            serviceLog.warn(message);
        }

        /**
         * @see org.apache.james.jspf.core.Logger#warn(String, Throwable)
         */
        public void warn(String message, Throwable t) {
            serviceLog.warn(message, t);
        }

        /**
         * @see org.apache.james.jspf.core.Logger#getChildLogger(String)
         */
        public org.apache.james.jspf.core.Logger getChildLogger(String name) {
            return this;
        }
    }

}
