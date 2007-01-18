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



package org.apache.james.smtpserver.core.esmtp;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.smtpserver.CommandHandler;
import org.apache.james.smtpserver.ExtensibleHandler;
import org.apache.james.smtpserver.LineHandler;
import org.apache.james.smtpserver.SMTPResponse;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.smtpserver.WiringException;
import org.apache.james.smtpserver.hook.AuthHook;
import org.apache.james.smtpserver.hook.HookResult;
import org.apache.james.smtpserver.hook.HookResultHook;
import org.apache.james.smtpserver.hook.HookReturnCode;
import org.apache.james.smtpserver.hook.MailParametersHook;
import org.apache.james.util.Base64;
import org.apache.james.util.mail.SMTPRetCode;
import org.apache.james.util.mail.dsn.DSNStatus;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;


/**
 * handles AUTH command
 * 
 * Note: we could extend this to use java5 sasl standard libraries and provide client
 * support against a server implemented via non-james specific hooks.
 * This would allow us to reuse hooks between imap4/pop3/smtp and eventually different
 * system (simple pluggabilty against external authentication services).
 */
public class AuthCmdHandler
    extends AbstractLogEnabled
    implements CommandHandler, EhloExtension, ExtensibleHandler, MailParametersHook {

    private abstract class AbstractSMTPLineHandler implements LineHandler {
        
        public void onLine(SMTPSession session, byte[] line) {
            try {
                String l = new String(line, "US-ASCII");
                SMTPResponse res = onCommand(session,l);
                session.popLineHandler();
                session.writeSMTPResponse(res);
            } catch (UnsupportedEncodingException e) {
                // TODO should never happen
                e.printStackTrace();
            }
        }

        protected abstract SMTPResponse onCommand(SMTPSession session, String l);
    }



    /**
     * The text string for the SMTP AUTH type PLAIN.
     */
    private final static String AUTH_TYPE_PLAIN = "PLAIN";

    /**
     * The text string for the SMTP AUTH type LOGIN.
     */
    private final static String AUTH_TYPE_LOGIN = "LOGIN";

    /**
     * The AuthHooks
     */
    private List hooks;
    
    private List rHooks;
    
    /**
     * handles AUTH command
     *
     * @see org.apache.james.smtpserver.CommandHandler#onCommand(SMTPSession)
     */
    public SMTPResponse onCommand(SMTPSession session, String command, String argument) {
        return doAUTH(session, argument);
    }



    /**
     * Handler method called upon receipt of a AUTH command.
     * Handles client authentication to the SMTP server.
     *
     * @param session SMTP session
     * @param argument the argument passed in with the command by the SMTP client
     */
    private SMTPResponse doAUTH(SMTPSession session, String argument) {
        if (session.getUser() != null) {
            return new SMTPResponse(SMTPRetCode.BAD_SEQUENCE, DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.DELIVERY_OTHER)+" User has previously authenticated. "
                    + " Further authentication is not required!");
        } else if (argument == null) {
            return new SMTPResponse(SMTPRetCode.SYNTAX_ERROR_ARGUMENTS, DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.DELIVERY_INVALID_ARG)+" Usage: AUTH (authentication type) <challenge>");
        } else {
            String initialResponse = null;
            if ((argument != null) && (argument.indexOf(" ") > 0)) {
                initialResponse = argument.substring(argument.indexOf(" ") + 1);
                argument = argument.substring(0,argument.indexOf(" "));
            }
            String authType = argument.toUpperCase(Locale.US);
            if (authType.equals(AUTH_TYPE_PLAIN)) {
                String userpass;
                if (initialResponse == null) {
                    session.pushLineHandler(new AbstractSMTPLineHandler() {
                        protected SMTPResponse onCommand(SMTPSession session, String l) {
                            return doPlainAuthPass(session, l);
                        }
                    });
                    return new SMTPResponse(SMTPRetCode.AUTH_READY, "OK. Continue authentication");
                } else {
                    userpass = initialResponse.trim();
                    return doPlainAuthPass(session, userpass);
                }
            } else if (authType.equals(AUTH_TYPE_LOGIN)) {
                
                if (initialResponse == null) {
                    session.pushLineHandler(new AbstractSMTPLineHandler() {
                        protected SMTPResponse onCommand(SMTPSession session, String l) {
                            return doLoginAuthPass(session, l);
                        }
                    });
                    return new SMTPResponse(SMTPRetCode.AUTH_READY, "VXNlcm5hbWU6"); // base64 encoded "Username:"
                } else {
                    String user = initialResponse.trim();
                    return doLoginAuthPass(session, user);
                }
            } else {
                return doUnknownAuth(session, authType, initialResponse);
            }
        }
    }

    /**
     * Carries out the Plain AUTH SASL exchange.
     *
     * According to RFC 2595 the client must send: [authorize-id] \0 authenticate-id \0 password.
     *
     * >>> AUTH PLAIN dGVzdAB0ZXN0QHdpei5leGFtcGxlLmNvbQB0RXN0NDI=
     * Decoded: test\000test@wiz.example.com\000tEst42
     *
     * >>> AUTH PLAIN dGVzdAB0ZXN0AHRFc3Q0Mg==
     * Decoded: test\000test\000tEst42
     *
     * @param session SMTP session object
     * @param initialResponse the initial response line passed in with the AUTH command
     */
    private SMTPResponse doPlainAuthPass(SMTPSession session, String userpass) {
        String user = null, pass = null;
        try {
            if (userpass != null) {
                userpass = Base64.decodeAsString(userpass);
            }
            if (userpass != null) {
                /*  See: RFC 2595, Section 6
                    The mechanism consists of a single message from the client to the
                    server.  The client sends the authorization identity (identity to
                    login as), followed by a US-ASCII NUL character, followed by the
                    authentication identity (identity whose password will be used),
                    followed by a US-ASCII NUL character, followed by the clear-text
                    password.  The client may leave the authorization identity empty to
                    indicate that it is the same as the authentication identity.

                    The server will verify the authentication identity and password with
                    the system authentication database and verify that the authentication
                    credentials permit the client to login as the authorization identity.
                    If both steps succeed, the user is logged in.
                */
                StringTokenizer authTokenizer = new StringTokenizer(userpass, "\0");
                String authorize_id = authTokenizer.nextToken();  // Authorization Identity
                user = authTokenizer.nextToken();                 // Authentication Identity
                try {
                    pass = authTokenizer.nextToken();             // Password
                }
                catch (java.util.NoSuchElementException _) {
                    // If we got here, this is what happened.  RFC 2595
                    // says that "the client may leave the authorization
                    // identity empty to indicate that it is the same as
                    // the authentication identity."  As noted above,
                    // that would be represented as a decoded string of
                    // the form: "\0authenticate-id\0password".  The
                    // first call to nextToken will skip the empty
                    // authorize-id, and give us the authenticate-id,
                    // which we would store as the authorize-id.  The
                    // second call will give us the password, which we
                    // think is the authenticate-id (user).  Then when
                    // we ask for the password, there are no more
                    // elements, leading to the exception we just
                    // caught.  So we need to move the user to the
                    // password, and the authorize_id to the user.
                    pass = user;
                    user = authorize_id;
                }

                authTokenizer = null;
            }
        }
        catch (Exception e) {
            // Ignored - this exception in parsing will be dealt
            // with in the if clause below
        }
        // Authenticate user
        return doAuthTest(session, user, pass, "PLAIN");
    }

    /**
     * Carries out the Login AUTH SASL exchange.
     *
     * @param session SMTP session object
     * @param initialResponse the initial response line passed in with the AUTH command
     */
    private SMTPResponse doLoginAuthPass(SMTPSession session, String user) {
        if (user != null) {
            try {
                user = Base64.decodeAsString(user);
            } catch (Exception e) {
                // Ignored - this parse error will be
                // addressed in the if clause below
                user = null;
            }
        }
        session.pushLineHandler(new AbstractSMTPLineHandler() {

            private String user;

            public LineHandler setUser(String user) {
                this.user = user;
                return this;
            }

            protected SMTPResponse onCommand(SMTPSession session, String l) {
                return doLoginAuthPassCheck(session, user, l);
            }
            
        }.setUser(user));
        return new SMTPResponse(SMTPRetCode.AUTH_READY, "UGFzc3dvcmQ6"); // base64 encoded "Password:"
    }
    
    private SMTPResponse doLoginAuthPassCheck(SMTPSession session, String user, String pass) {
        if (pass != null) {
            try {
                pass = Base64.decodeAsString(pass);
            } catch (Exception e) {
                // Ignored - this parse error will be
                // addressed in the if clause below
                pass = null;
            }
        }
        // Authenticate user
        return doAuthTest(session, user, pass, "LOGIN");
    }



    /**
     * @param session
     * @param user
     * @param pass
     * @param authType
     * @return
     */
    private SMTPResponse doAuthTest(SMTPSession session, String user, String pass, String authType) {
        if ((user == null) || (pass == null)) {
            return new SMTPResponse(SMTPRetCode.SYNTAX_ERROR_ARGUMENTS,"Could not decode parameters for AUTH "+authType);
        }

        SMTPResponse res = null;
        
        List hooks = getHooks();
        
        if (hooks != null) {
            int count = hooks.size();
            for (int i = 0; i < count; i++) {
                Object rawHook = hooks.get(i);
                getLogger().debug("executing  hook " + rawHook);
                
                HookResult hRes = ((AuthHook) rawHook).doAuth(session, user, pass);
                
                if (rHooks != null) {
                    for (int i2 = 0; i2 < rHooks.size(); i2++) {
                        Object rHook = rHooks.get(i2);
                        getLogger().debug("executing  hook " + rHook);
                    
                        hRes = ((HookResultHook) rHook).onHookResult(session, hRes, rHook);
                    }
                }
                
                res = calcDefaultSMTPResponse(hRes);
                
                if (res != null) {
                    if (SMTPRetCode.AUTH_FAILED.equals(res.getRetCode())) {
                        getLogger().error("AUTH method "+authType+" failed");
                    } else if (SMTPRetCode.AUTH_OK.equals(res.getRetCode())) {
                        if (getLogger().isDebugEnabled()) {
                            // TODO: Make this string a more useful debug message
                            getLogger().debug("AUTH method "+authType+" succeeded");
                        }
                    }
                    return res;
                }
            }
        }

        res = new SMTPResponse(SMTPRetCode.AUTH_FAILED, "Authentication Failed");
        // TODO: Make this string a more useful error message
        getLogger().error("AUTH method "+authType+" failed");
        return res;
    }


    /**
     * Calculate the SMTPResponse for the given result
     * 
     * @param result the HookResult which should converted to SMTPResponse
     * @return the calculated SMTPResponse for the given HookReslut
     */
    protected SMTPResponse calcDefaultSMTPResponse(HookResult result) {
        if (result != null) {
            int rCode = result.getResult();
            String smtpRetCode = result.getSmtpRetCode();
            String smtpDesc = result.getSmtpDescription();
    
            if (rCode == HookReturnCode.DENY) {
                if (smtpRetCode == null)
                    smtpRetCode = SMTPRetCode.AUTH_FAILED;
                if (smtpDesc == null)
                    smtpDesc = "Authentication Failed";
    
                return new SMTPResponse(smtpRetCode, smtpDesc);
            } else if (rCode == HookReturnCode.DENYSOFT) {
                if (smtpRetCode == null)
                    smtpRetCode = SMTPRetCode.LOCAL_ERROR;
                if (smtpDesc == null)
                    smtpDesc = "Temporary problem. Please try again later";
    
                return new SMTPResponse(smtpRetCode, smtpDesc);
            } else if (rCode == HookReturnCode.OK) {
                if (smtpRetCode == null)
                    smtpRetCode = SMTPRetCode.AUTH_OK;
                if (smtpDesc == null)
                    smtpDesc = "Authentication Succesfull";
    
                return new SMTPResponse(smtpRetCode, smtpDesc);
            } else {
                // Return null as default
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Handles the case of an unrecognized auth type.
     *
     * @param session SMTP session object
     * @param authType the unknown auth type
     * @param initialResponse the initial response line passed in with the AUTH command
     */
    private SMTPResponse doUnknownAuth(SMTPSession session, String authType, String initialResponse) {
        if (getLogger().isErrorEnabled()) {
            StringBuffer errorBuffer =
                new StringBuffer(128)
                    .append("AUTH method ")
                        .append(authType)
                        .append(" is an unrecognized authentication type");
            getLogger().error(errorBuffer.toString());
        }
        return new SMTPResponse(SMTPRetCode.PARAMETER_NOT_IMPLEMENTED, "Unrecognized Authentication Type");
    }



    /**
     * @see org.apache.james.smtpserver.CommandHandler#getImplCommands()
     */
    public Collection getImplCommands() {
        Collection implCommands = new ArrayList();
        implCommands.add("AUTH");
        
        return implCommands;
    }

    /**
     * @see org.apache.james.smtpserver.core.esmtp.EhloExtension#getImplementedEsmtpFeatures(org.apache.james.smtpserver.SMTPSession)
     */
    public List getImplementedEsmtpFeatures(SMTPSession session) {
        if (session.isAuthSupported()) {
            List resp = new LinkedList();
            resp.add("AUTH LOGIN PLAIN");
            resp.add("AUTH=LOGIN PLAIN");
            return resp;
        } else {
            return null;
        }
    }

    /**
     * @see org.apache.james.smtpserver.ExtensibleHandler#getMarkerInterfaces()
     */
    public List getMarkerInterfaces() {
        List classes = new ArrayList(1);
        classes.add(AuthHook.class);
        return classes;
    }


    /**
     * @see org.apache.james.smtpserver.ExtensibleHandler#wireExtensions(java.lang.Class, java.util.List)
     */
    public void wireExtensions(Class interfaceName, List extension) throws WiringException {
        if (AuthHook.class.equals(interfaceName)) {
            this.hooks = extension;
            // If no AuthHook is configured then we revert to the default LocalUsersRespository check
            if (hooks == null || hooks.size() == 0) {
                throw new WiringException("AuthCmdHandler used without AuthHooks");
            }
        } else if (HookResultHook.class.equals(interfaceName)) {
            this.rHooks = extension;
        }
    }
    

    /**
     * Return a list which holds all hooks for the cmdHandler
     * 
     * @return
     */
    protected List getHooks() {
        return hooks;
    }

    /**
     * @see org.apache.james.smtpserver.hook.MailParametersHook#doMailParameter(org.apache.james.smtpserver.SMTPSession, java.lang.String, java.lang.String)
     */
    public HookResult doMailParameter(SMTPSession session, String paramName, String paramValue) {
        // Ignore the AUTH command.
        // TODO we should at least check for correct syntax and put the result in session
        return new HookResult(HookReturnCode.DECLINED);
    }

    /**
     * @see org.apache.james.smtpserver.hook.MailParametersHook#getMailParamNames()
     */
    public String[] getMailParamNames() {
        return new String[] { "AUTH" };
    }

}
