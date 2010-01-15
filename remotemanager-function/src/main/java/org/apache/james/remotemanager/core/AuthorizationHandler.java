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

package org.apache.james.remotemanager.core;

import org.apache.james.remotemanager.ConnectHandler;
import org.apache.james.remotemanager.LineHandler;
import org.apache.james.remotemanager.RemoteManagerResponse;
import org.apache.james.remotemanager.RemoteManagerSession;

public class AuthorizationHandler implements ConnectHandler, LineHandler {

    private final static String AUTHORIZATION_STATE = "AUTHORIZATION_STATE";
    private final static int LOGIN_SUPPLIED = 1;
    private final static int PASSWORD_SUPPLIED = 2;

    private final static String USERNAME = "USERNAME";

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.remotemanager.ConnectHandler#onConnect(org.apache.james
     * .remotemanager.RemoteManagerSession)
     */
    public void onConnect(RemoteManagerSession session) {
        RemoteManagerResponse response = new RemoteManagerResponse("JAMES Remote Administration Tool ");// +
        // Constants.SOFTWARE_VERSION)
        response.appendLine("Please enter your login and password");
        response.appendLine("Login id:");

        session.writeRemoteManagerResponse(response);

        session.getState().put(AUTHORIZATION_STATE, LOGIN_SUPPLIED);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.remotemanager.LineHandler#onLine(org.apache.james
     * .remotemanager.RemoteManagerSession, java.lang.String)
     */
    public void onLine(RemoteManagerSession session, String line) {
        int state = (Integer) session.getState().get(AUTHORIZATION_STATE);

        if (state == LOGIN_SUPPLIED) {
            session.getState().put(USERNAME, line);
            session.getState().put(AUTHORIZATION_STATE, PASSWORD_SUPPLIED);

            session.writeRemoteManagerResponse(new RemoteManagerResponse("Password:"));
        } else if (state == PASSWORD_SUPPLIED) {
            String password = line;
            String username = (String) session.getState().get(USERNAME);

            if (!password.equals(session.getAdministrativeAccountData().get(username)) || password.length() == 0) {
                final String message = "Login failed for " + username;
                session.writeRemoteManagerResponse(new RemoteManagerResponse(message));
                session.writeRemoteManagerResponse(new RemoteManagerResponse("Login id:"));
            } else {
                StringBuilder messageBuffer = new StringBuilder(64).append("Welcome ").append(username).append(". HELP for a list of commands");
                session.writeRemoteManagerResponse(new RemoteManagerResponse(messageBuffer.toString()));
                if (session.getLogger().isInfoEnabled()) {
                    StringBuilder infoBuffer = new StringBuilder(128).append("Login for ").append(username).append(" successful");
                    session.getLogger().info(infoBuffer.toString());
                }
                session.popLineHandler();
            }
            session.getState().remove(USERNAME);
        }
    }

}
