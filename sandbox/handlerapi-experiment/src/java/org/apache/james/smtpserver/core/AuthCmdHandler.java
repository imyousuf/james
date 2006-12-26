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

import org.apache.james.smtpserver.CommandHandler;
import org.apache.james.smtpserver.LineHandler;
import org.apache.james.smtpserver.SMTPResponse;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.util.mail.SMTPRetCode;
import org.apache.james.util.mail.dsn.DSNStatus;
import org.apache.avalon.framework.logger.AbstractLogEnabled;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.StringTokenizer;
import org.apache.james.util.Base64;
import java.io.UnsupportedEncodingException;


/**
  * handles AUTH command
  */
public class AuthCmdHandler
    extends AbstractLogEnabled
    implements CommandHandler {

    /**
     * The text string for the SMTP AUTH type PLAIN.
     */
    private final static String AUTH_TYPE_PLAIN = "PLAIN";

    /**
     * The text string for the SMTP AUTH type LOGIN.
     */
    private final static String AUTH_TYPE_LOGIN = "LOGIN";
    
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
                    session.pushLineHandler(new LineHandler() {

                        public void onLine(SMTPSession session, byte[] line) {
                            try {
                                String l = new String(line, "US-ASCII");
                                //System.err.println("((("+line+")))");
                                SMTPResponse res = doPlainAuthPass(session, l);
                                session.popLineHandler();
                                session.writeSMTPResponse(res);
                            } catch (UnsupportedEncodingException e) {
                                // TODO should never happen
                                e.printStackTrace();
                            }
                        }
                        
                    });
                    return new SMTPResponse("334", "OK. Continue authentication");
                } else {
                    userpass = initialResponse.trim();
                    return doPlainAuthPass(session, userpass);
                }
            } else if (authType.equals(AUTH_TYPE_LOGIN)) {
                
                if (initialResponse == null) {
                    session.pushLineHandler(new LineHandler() {

                        public void onLine(SMTPSession session, byte[] line) {
                            try {
                                SMTPResponse res = doLoginAuthPass(session, new String(line, "US-ASCII"));
                                session.popLineHandler();
                                session.writeSMTPResponse(res);
                            } catch (UnsupportedEncodingException e) {
                                // TODO should never happen
                                e.printStackTrace();
                            }
                        }
                        
                    });
                    return new SMTPResponse("334", "VXNlcm5hbWU6"); // base64 encoded "Username:"
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
        if ((user == null) || (pass == null)) {
            return new SMTPResponse(SMTPRetCode.SYNTAX_ERROR_ARGUMENTS, "Could not decode parameters for AUTH PLAIN");
        } else if (session.getConfigurationData().getUsersRepository().test(user, pass)) {
            session.setUser(user);
            getLogger().info("AUTH method PLAIN succeeded");
            return new SMTPResponse(SMTPRetCode.AUTH_OK, "Authentication Successful");
        } else {
            getLogger().error("AUTH method PLAIN failed");
            return new SMTPResponse(SMTPRetCode.AUTH_FAILED, "Authentication Failed");
        }
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
        session.pushLineHandler(new LineHandler() {

            private String user;

            public void onLine(SMTPSession session, byte[] line) {
                try {
                    SMTPResponse res = doLoginAuthPassCheck(session, user, new String(line, "US-ASCII"));
                    session.popLineHandler();
                    session.writeSMTPResponse(res);
                } catch (UnsupportedEncodingException e) {
                    // TODO should never happen
                    e.printStackTrace();
                }
            }

            public LineHandler setUser(String user) {
                this.user = user;
                return this;
            }
            
        }.setUser(user));
        return new SMTPResponse("334", "UGFzc3dvcmQ6"); // base64 encoded "Password:"
    }
    
    private SMTPResponse doLoginAuthPassCheck(SMTPSession session, String user, String pass) {
        SMTPResponse response = null;
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
        if ((user == null) || (pass == null)) {
            response = new SMTPResponse(SMTPRetCode.SYNTAX_ERROR_ARGUMENTS,"Could not decode parameters for AUTH LOGIN");
        } else if (session.getConfigurationData().getUsersRepository().test(user, pass)) {
            session.setUser(user);
            response = new SMTPResponse(SMTPRetCode.AUTH_OK, "Authentication Successful");
            if (getLogger().isDebugEnabled()) {
                // TODO: Make this string a more useful debug message
                getLogger().debug("AUTH method LOGIN succeeded");
            }
        } else {
            response = new SMTPResponse(SMTPRetCode.AUTH_FAILED, "Authentication Failed");
            // TODO: Make this string a more useful error message
            getLogger().error("AUTH method LOGIN failed");
        }
        return response;
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
}
