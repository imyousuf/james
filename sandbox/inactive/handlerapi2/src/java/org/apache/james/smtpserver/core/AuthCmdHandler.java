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
import org.apache.james.smtpserver.IOState;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.util.mail.dsn.DSNStatus;
import org.apache.avalon.framework.logger.AbstractLogEnabled;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.StringTokenizer;
import org.apache.james.util.Base64;

/**
 * handles AUTH command
 */
public class AuthCmdHandler extends AbstractLogEnabled implements
    CommandHandler, IOState {

    /**
     * The text string for the SMTP AUTH type PLAIN.
     */
    private final static String AUTH_TYPE_PLAIN = "PLAIN";

    /**
     * The text string for the SMTP AUTH type LOGIN.
     */
    private final static String AUTH_TYPE_LOGIN = "LOGIN";

    private final static String AUTH_TYPE = "AUTH_TYPE";

    private final static String USER = "USER";

    private final static String PASS = "PASS";

    private final static String USER_PASS = "USER_PASS";

    /**
     * handles AUTH command
     *
     * @see org.apache.james.smtpserver.CommandHandler#onCommand(SMTPSession)
     */
    public void onCommand(SMTPSession session) {
        if (doAUTH(session, session.getCommandArgument()) == true) {
            session.pushIOState(this);
        }

    }

    /**
     * Handler method called upon receipt of a AUTH command.
     * Handles client authentication to the SMTP server.
     *
     * @param session SMTP session
     * @param argument the argument passed in with the command by the SMTP client
     */
    private boolean doAUTH(SMTPSession session, String argument) {
        String responseString = null;
        if (session.getUser() != null) {
            responseString = "503 "
                + DSNStatus.getStatus(DSNStatus.PERMANENT,
                    DSNStatus.DELIVERY_OTHER)
                + " User has previously authenticated. "
                + " Further authentication is not required!";
            session.getSMTPResponse().setRawSMTPResponse(responseString);
            return false;
        } else if (argument == null) {
            responseString = "501 "
                + DSNStatus.getStatus(DSNStatus.PERMANENT,
                    DSNStatus.DELIVERY_INVALID_ARG)
                + " Usage: AUTH (authentication type) <challenge>";
            session.getSMTPResponse().setRawSMTPResponse(responseString);
            return false;
         
        } else {
             
            String initialResponse = null;
            if ((argument != null) && (argument.indexOf(" ") > 0)) {
                initialResponse = argument.substring(argument.indexOf(" ") + 1);
                argument = argument.substring(0, argument.indexOf(" "));
            }
            String authType = argument.toUpperCase(Locale.US);

            session.getState().put(AUTH_TYPE, authType);

            if (authType.equals(AUTH_TYPE_PLAIN)) {
                if (initialResponse != null) {    
                   doPlainAuthUserPass(session,initialResponse);
                   session.getSMTPResponse().setRawSMTPResponse(doPlainAuth(session));
                   return false;
                } 
                session.getSMTPResponse().setRawSMTPResponse(getPlainAuthUserPassResponse());
        
                return true;

            } else if (authType.equals(AUTH_TYPE_LOGIN)) {
                if ((initialResponse) != null) {
                    doLoginAuthUser(session,initialResponse);
                }
                session.getSMTPResponse().setRawSMTPResponse(getLoginAuthUserResponse());
            
                return true;
            } else {
                doUnknownAuth(session, authType);
                return false;
            }
        }
    }

    private String getPlainAuthUserPassResponse() {
        String responseString = "334 OK. Continue authentication";
        return responseString;
    }
    
    private void doPlainAuthUserPass(SMTPSession session, String initialResponse) {
        session.getState().put(USER_PASS, initialResponse.trim());
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
     */
    private String doPlainAuth(SMTPSession session) {
        String user = null;
        String pass = null;
        String responseString = null;
        String userpass = (String) session.getState().get(USER_PASS);

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
                StringTokenizer authTokenizer = new StringTokenizer(userpass,"\0");
                String authorize_id = authTokenizer.nextToken(); // Authorization Identity
                user = authTokenizer.nextToken(); // Authentication Identity
                try {
                    pass = authTokenizer.nextToken(); // Password
                } catch (java.util.NoSuchElementException _) {
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
        } catch (Exception e) {
            // Ignored - this exception in parsing will be dealt
            // with in the if clause below
        }
  
        // Authenticate user
        if ((user == null) || (pass == null)) {
             responseString = "501 Could not decode parameters for AUTH PLAIN";
        } else if (session.getConfigurationData().getUsersRepository().test(user, pass)) {
            session.setUser(user);
            responseString = "235 Authentication Successful";
            getLogger().info("AUTH method PLAIN succeeded");
        } else {
            responseString = "535 Authentication Failed";
            getLogger().error("AUTH method PLAIN failed");
        }
        return responseString;
    }

    private void doLoginAuthUser(SMTPSession session, String initialResponse) {
        session.getState().put(USER, initialResponse.trim());
    }
    
    private String getLoginAuthUserResponse() {
        String responseString = "334 VXNlcm5hbWU6"; // base64 encoded "Username:"
        return responseString;
    }

    private void doLoginAuthPass(SMTPSession session, String pass) {
        session.getState().put(PASS, pass);
    }
    
    private String getLoginAuthPassResponse() {
        String responseString = "334 UGFzc3dvcmQ6"; // base64 encoded "Password:"
        return responseString;
    }

    /**
     * Carries out the Login AUTH SASL exchange.
     *
     * @param session SMTP session object
     */
    private String doLoginAuth(SMTPSession session) {
        String user = (String) session.getState().get(USER);
        String pass = (String) session.getState().get(PASS);

        String responseString = null;
        if (user != null) {
            try {
                user = Base64.decodeAsString(user);
            } catch (Exception e) {
                // Ignored - this parse error will be
               // addressed in the if clause below
                user = null;
            }
        }

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
            responseString = "501 Could not decode parameters for AUTH LOGIN";
        } else if (session.getConfigurationData().getUsersRepository().test(
            user, pass)) {
            session.setUser(user);
            responseString = "235 Authentication Successful";
            if (getLogger().isDebugEnabled()) {
                // TODO: Make this string a more useful debug message
                getLogger().debug("AUTH method LOGIN succeeded");
            }
        } else {
            responseString = "535 Authentication Failed";
            // TODO: Make this string a more useful error message
            getLogger().error("AUTH method LOGIN failed");
        }
        return responseString;
    }

    /**
     * Handles the case of an unrecognized auth type.
     *
     * @param session SMTP session object
     * @param authType the unknown auth type
     */
    private void doUnknownAuth(SMTPSession session, String authType) {
        String responseString = "504 Unrecognized Authentication Type";
        if (getLogger().isErrorEnabled()) {
            StringBuffer errorBuffer = new StringBuffer(128).append(
                "AUTH method ").append(authType).append(
                " is an unrecognized authentication type");
            getLogger().error(errorBuffer.toString());
        }
        
        session.getSMTPResponse().setRawSMTPResponse(responseString);
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
     * @see org.apache.james.smtpserver.IOState#readResponse(SMTPSession, String)
     */
    public void readResponse(SMTPSession session, String data) {
	// get auth type
        String authType = (String) session.getState().get(AUTH_TYPE);

        if (authType.equals(AUTH_TYPE_LOGIN)) {
            // get user
            String user = (String) session.getState().get(USER);
        
            if (user == null) {
                doLoginAuthUser(session, data);
                session.getSMTPResponse().setRawSMTPResponse(getLoginAuthPassResponse());
            } else {
                doLoginAuthPass(session, data);
                session.getSMTPResponse().setRawSMTPResponse(doLoginAuth(session));
            
                // cleanup stateMap and remove the temporary IOState
                cleanupStateMap(session);
                session.popIOState();
            }
        } else if (authType.equals(AUTH_TYPE_PLAIN)) {
            String userpass = (String) session.getState().get(USER_PASS);

            if (userpass == null) {
                doPlainAuthUserPass(session, data);
                session.getSMTPResponse().setRawSMTPResponse(getPlainAuthUserPassResponse());
            }
            session.getSMTPResponse().setRawSMTPResponse(doPlainAuth(session));
            
            // cleanup stateMap and remove the temporary IOState
            cleanupStateMap(session);
            session.popIOState();
            
        } else {
            // cleanup stateMap and remove the temporary IOState
            cleanupStateMap(session);
            session.popIOState();
        }
    }
    
    /**
     * Remove temporary entries from the state map
     * 
     * @param session The SMTPSession
     */
    private void cleanupStateMap(SMTPSession session) {
        session.getState().remove(AUTH_TYPE);
        session.getState().remove(USER);
        session.getState().remove(PASS);
        session.getState().remove(USER_PASS);
    }
}
