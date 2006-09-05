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



package org.apache.james.remotemanager;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.mail.internet.ParseException;

import org.apache.james.Constants;
import org.apache.james.core.AbstractJamesHandler;
import org.apache.james.management.BayesianAnalyzerManagementException;
import org.apache.james.management.SpoolFilter;
import org.apache.james.services.JamesUser;
import org.apache.james.services.User;
import org.apache.james.services.UsersRepository;
import org.apache.mailet.MailAddress;


/**
 * Provides a really rude network interface to administer James.
 * - Allow to add accounts.
 * - Allow to manage the spool
 * - Allow to feed BayesianAnalysis
 * 
 * TODO: -improve protocol
 *       -much more...
 *
 * @version $Revision$
 *
 */
public class RemoteManagerHandler
    extends AbstractJamesHandler {

    /**
     * The text string for the MEMSTAT command
     */
    private static final String COMMAND_MEMSTAT = "MEMSTAT";

    /**
     * The text string for the ADDUSER command
     */
    private static final String COMMAND_ADDUSER = "ADDUSER";

    /**
     * The text string for the SETPASSWORD command
     */
    private static final String COMMAND_SETPASSWORD = "SETPASSWORD";

    /**
     * The text string for the DELUSER command
     */
    private static final String COMMAND_DELUSER = "DELUSER";

    /**
     * The text string for the LISTUSERS command
     */
    private static final String COMMAND_LISTUSERS = "LISTUSERS";

    /**
     * The text string for the COUNTUSERS command
     */
    private static final String COMMAND_COUNTUSERS = "COUNTUSERS";

    /**
     * The text string for the VERIFY command
     */
    private static final String COMMAND_VERIFY = "VERIFY";

    /**
     * The text string for the HELP command
     */
    private static final String COMMAND_HELP = "HELP";

    /**
     * The text string for the SETFORWARDING command
     */
    private static final String COMMAND_SETFORWARDING = "SETFORWARDING";

    /**
     * The text string for the SHOWFORWARDING command
     */
    private static final String COMMAND_SHOWFORWARDING = "SHOWFORWARDING";

    /**
     * The text string for the UNSETFORWARDING command
     */
    private static final String COMMAND_UNSETFORWARDING = "UNSETFORWARDING";

    /**
     * The text string for the SETALIAS command
     */
    private static final String COMMAND_SETALIAS = "SETALIAS";

    /**
     * The text string for the SHOWALIAS command
     */
    private static final String COMMAND_SHOWALIAS = "SHOWALIAS";

    /**
     * The text string for the UNSETALIAS command
     */
    private static final String COMMAND_UNSETALIAS = "UNSETALIAS";

    /**
     * The text string for the USER command
     */
    private static final String COMMAND_USER = "USER";

    /**
     * The text string for the LISTSPOOL command
     */
    private static final String COMMAND_LISTSPOOL = "LISTSPOOL";

    /**
     * The text string for the FLUSHSPOOL command
     */
    private static final String COMMAND_FLUSHSPOOL = "FLUSHSPOOL";

    /**
     * The text string for the DELETESPOOL command
     */
    private static final String COMMAND_DELETESPOOL = "DELETESPOOL";

    /**
     * The text string for the ADDHAM command
     */
    private static final String COMMAND_ADDHAM = "ADDHAM";
    
    /**
     * The text string for the ADDSPAM command
     */
    private static final String COMMAND_ADDSPAM = "ADDSPAM";
    
    private static final String COMMAND_EXPORTBAYESIANDATA = "EXPORTBAYESIANDATA";
   
    private static final String COMMAND_IMPORTBAYESIANDATA = "IMPORTBAYESIANDATA";
    
    /**
     * The text string for the QUIT command
     */
    private static final String COMMAND_QUIT = "QUIT";

    /**
     * The text string for the SHUTDOWN command
     */
    private static final String COMMAND_SHUTDOWN = "SHUTDOWN";


    /**
     * The per-service configuration data that applies to all handlers
     */
    private RemoteManagerHandlerConfigurationData theConfigData;

    /**
     * The current UsersRepository being managed/viewed/modified
     */
    private UsersRepository users;
    

    /**
     * Set the configuration data for the handler.
     *
     * @param theData the configuration data
     */
    public void setConfigurationData(Object theData) {
        if (theData instanceof RemoteManagerHandlerConfigurationData) {
            theConfigData = (RemoteManagerHandlerConfigurationData) theData;

            // Reset the users repository to the default.
            users = theConfigData.getUsersRepository();
        } else {
            throw new IllegalArgumentException("Configuration object does not implement RemoteManagerHandlerConfigurationData");
        }
    }

    /**
     * @see org.apache.avalon.cornerstone.services.connection.ConnectionHandler#handleConnection(Socket)
     */
    protected void handleProtocol() throws IOException {
        writeLoggedResponse("JAMES Remote Administration Tool " + Constants.SOFTWARE_VERSION );
        writeLoggedResponse("Please enter your login and password");
        String login = null;
        String password = null;
        do {
            if (login != null) {
                final String message = "Login failed for " + login;
                writeLoggedFlushedResponse(message);
            }
            writeLoggedFlushedResponse("Login id:");
            login = inReader.readLine().trim();
            writeLoggedFlushedResponse("Password:");
            password = inReader.readLine().trim();
        } while (!password.equals(theConfigData.getAdministrativeAccountData().get(login)) || password.length() == 0);

        StringBuffer messageBuffer =
            new StringBuffer(64)
                    .append("Welcome ")
                    .append(login)
                    .append(". HELP for a list of commands");
        out.println( messageBuffer.toString() );
        out.flush();
        if (getLogger().isInfoEnabled()) {
            StringBuffer infoBuffer =
                new StringBuffer(128)
                        .append("Login for ")
                        .append(login)
                        .append(" successful");
            getLogger().info(infoBuffer.toString());
        }

        try {
            out.print(theConfigData.getPrompt());
            out.flush();
            theWatchdog.start();
            while (parseCommand(inReader.readLine())) {
                theWatchdog.reset();
                out.print(theConfigData.getPrompt());
                out.flush();
            }
            theWatchdog.stop();
        } catch (IOException ioe) {
            //We can cleanly ignore this as it's probably a socket timeout
        } catch (Throwable thr) {
            System.out.println("Exception: " + thr.getMessage());
            getLogger().error("Encountered exception in handling the remote manager connection.", thr);
        }
        StringBuffer infoBuffer =
            new StringBuffer(64)
                    .append("Logout for ")
                    .append(login)
                    .append(".");
        getLogger().info(infoBuffer.toString());

    }

    /**
     * @see org.apache.james.core.AbstractJamesHandler#errorHandler(java.lang.RuntimeException)
     */
    protected void errorHandler(RuntimeException e) {
        out.println("Unexpected Error: "+e.getMessage());
        out.flush();
        if (getLogger().isErrorEnabled()) {
            StringBuffer exceptionBuffer =
                new StringBuffer(128)
                        .append("Exception during connection from ")
                        .append(remoteHost)
                        .append(" (")
                        .append(remoteIP)
                        .append("): ")
                        .append(e.getMessage());
            getLogger().error(exceptionBuffer.toString(),e);
        }
    }

    /**
     * Resets the handler data to a basic state.
     */
    protected void resetHandler() {
        // Reset user repository
        users = theConfigData.getUsersRepository();

        // Clear config data
        theConfigData = null;
    }

    /**
     * <p>This method parses and processes RemoteManager commands read off the
     * wire in handleConnection.  It returns true if expecting additional
     * commands, false otherwise.</p>
     *
     * @param rawCommand the raw command string passed in over the socket
     *
     * @return whether additional commands are expected.
     */
    private boolean parseCommand( String rawCommand ) {
        if (rawCommand == null) {
            return false;
        }
        String command = rawCommand.trim();
        String argument = null;
        int breakIndex = command.indexOf(" ");
        if (breakIndex > 0) {
            argument = command.substring(breakIndex + 1);
            command = command.substring(0, breakIndex);
        }
        command = command.toUpperCase(Locale.US);
        if (command.equals(COMMAND_ADDUSER)) {
            doADDUSER(argument);
        } else if (command.equals(COMMAND_SETPASSWORD)) {
            return doSETPASSWORD(argument);
        } else if (command.equals(COMMAND_DELUSER)) {
            return doDELUSER(argument);
        } else if (command.equals(COMMAND_LISTUSERS)) {
            return doLISTUSERS(argument);
        } else if (command.equals(COMMAND_COUNTUSERS)) {
            return doCOUNTUSERS(argument);
        } else if (command.equals(COMMAND_VERIFY)) {
            return doVERIFY(argument);
        } else if (command.equals(COMMAND_HELP)) {
            return doHELP(argument);
        } else if (command.equals(COMMAND_SETALIAS)) {
            return doSETALIAS(argument);
        } else if (command.equals(COMMAND_SETFORWARDING)) {
            return doSETFORWARDING(argument);
        } else if (command.equals(COMMAND_SHOWALIAS)) {
            return doSHOWALIAS(argument);
        } else if (command.equals(COMMAND_SHOWFORWARDING)) {
            return doSHOWFORWARDING(argument);
        } else if (command.equals(COMMAND_UNSETALIAS)) {
            return doUNSETALIAS(argument);
        } else if (command.equals(COMMAND_UNSETFORWARDING)) {
            return doUNSETFORWARDING(argument);
        } else if (command.equals(COMMAND_USER)) {
            return doUSER(argument);
        } else if (command.equals(COMMAND_LISTSPOOL)) {
            return doLISTSPOOL(argument);
        } else if (command.equals(COMMAND_FLUSHSPOOL)) {
            return doFLUSHSPOOL(argument);
        } else if (command.equals(COMMAND_DELETESPOOL)) {
            return doDELETESPOOL(argument);
        } else if (command.equals(COMMAND_ADDHAM)) {
            return doADDHAM(argument);
        } else if (command.equals(COMMAND_ADDSPAM)) {
            return doADDSPAM(argument);
        } else if (command.equals(COMMAND_EXPORTBAYESIANDATA)) {
            return doEXPORTBAYESIANDATA(argument);
        } else if (command.equals(COMMAND_IMPORTBAYESIANDATA)) {
            return doIMPORTBAYESIANDATA(argument);
        } else if (command.equals(COMMAND_MEMSTAT)) {
            return doMEMSTAT(argument);
        } else if (command.equals(COMMAND_QUIT)) {
            return doQUIT(argument);
        } else if (command.equals(COMMAND_SHUTDOWN)) {
            return doSHUTDOWN(argument);
        } else {
            return doUnknownCommand(rawCommand);
        }
        return true;
    }

    /**
     * Handler method called upon receipt of an MEMSTAT command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the argument passed in with the command
     */
    private boolean doMEMSTAT(String argument) {
        writeLoggedFlushedResponse("Current memory statistics:");
        writeLoggedFlushedResponse("\tFree Memory: " + Runtime.getRuntime().freeMemory());
        writeLoggedFlushedResponse("\tTotal Memory: " + Runtime.getRuntime().totalMemory());
        writeLoggedFlushedResponse("\tMax Memory: " + Runtime.getRuntime().maxMemory());

        if ("-gc".equalsIgnoreCase(argument)) {
            System.gc();
            writeLoggedFlushedResponse("And after System.gc():");
            writeLoggedFlushedResponse("\tFree Memory: " + Runtime.getRuntime().freeMemory());
            writeLoggedFlushedResponse("\tTotal Memory: " + Runtime.getRuntime().totalMemory());
            writeLoggedFlushedResponse("\tMax Memory: " + Runtime.getRuntime().maxMemory());
        }

        return true;
    }
    
    /**
     * Handler method called upon receipt of an ADDUSER command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the argument passed in with the command
     */
    private boolean doADDUSER(String argument) {
        int breakIndex = -1;
        if ((argument == null) ||
            (argument.equals("")) ||
            ((breakIndex = argument.indexOf(" ")) < 0)) {
            writeLoggedFlushedResponse("Usage: adduser [username] [password]");
            return true;
        }
        String username = argument.substring(0,breakIndex);
        String passwd = argument.substring(breakIndex + 1);
        if (username.equals("") || passwd.equals("")) {
            writeLoggedFlushedResponse("Usage: adduser [username] [password]");
            return true;
        }

        boolean success = false;
        if (users.contains(username)) {
            StringBuffer responseBuffer =
                new StringBuffer(64)
                        .append("User ")
                        .append(username)
                        .append(" already exists");
            String response = responseBuffer.toString();
            writeLoggedResponse(response);
        } else {
            success = users.addUser(username, passwd);
        }
        if ( success ) {
            StringBuffer responseBuffer =
                new StringBuffer(64)
                        .append("User ")
                        .append(username)
                        .append(" added");
            String response = responseBuffer.toString();
            out.println(response);
            getLogger().info(response);
        } else {
            out.println("Error adding user " + username);
            getLogger().error("Error adding user " + username);
        }
        out.flush();
        return true;
    }

    /**
     * Handler method called upon receipt of an SETPASSWORD command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the argument passed in with the command
     */
    private boolean doSETPASSWORD(String argument) {

        int breakIndex = -1;
        if ((argument == null) ||
            (argument.equals("")) ||
            ((breakIndex = argument.indexOf(" ")) < 0)) {
            writeLoggedFlushedResponse("Usage: setpassword [username] [password]");
            return true;
        }
        String username = argument.substring(0,breakIndex);
        String passwd = argument.substring(breakIndex + 1);

        if (username.equals("") || passwd.equals("")) {
            writeLoggedFlushedResponse("Usage: adduser [username] [password]");
            return true;
        }
        User user = users.getUserByName(username);
        if (user == null) {
            writeLoggedFlushedResponse("No such user " + username);
            return true;
        }
        boolean success = user.setPassword(passwd);
        if (success) {
            users.updateUser(user);
            StringBuffer responseBuffer =
                new StringBuffer(64)
                        .append("Password for ")
                        .append(username)
                        .append(" reset");
            String response = responseBuffer.toString();
            out.println(response);
            getLogger().info(response);
        } else {
            out.println("Error resetting password");
            getLogger().error("Error resetting password");
        }
        out.flush();
        return true;
    }

    /**
     * Handler method called upon receipt of an DELUSER command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the argument passed in with the command
     */
    private boolean doDELUSER(String argument) {
        String user = argument;
        if ((user == null) || (user.equals(""))) {
            writeLoggedFlushedResponse("Usage: deluser [username]");
            return true;
        }
        if (users.contains(user)) {
            try {
                users.removeUser(user);
                StringBuffer responseBuffer =
                                             new StringBuffer(64)
                                             .append("User ")
                                             .append(user)
                                             .append(" deleted");
                String response = responseBuffer.toString();
                out.println(response);
                getLogger().info(response);
            } catch (Exception e) {
                StringBuffer exceptionBuffer =
                                              new StringBuffer(128)
                                              .append("Error deleting user ")
                                              .append(user)
                                              .append(" : ")
                                              .append(e.getMessage());
                String exception = exceptionBuffer.toString();
                out.println(exception);
                getLogger().error(exception);
            }
        } else {
            StringBuffer responseBuffer =
                                         new StringBuffer(64)
                                         .append("User ")
                                         .append(user)
                                         .append(" doesn't exist");
            String response = responseBuffer.toString();
            out.println(response);
        }
        out.flush();
        return true;
    }

    /**
     * Handler method called upon receipt of an LISTUSERS command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the argument passed in with the command
     */
    private boolean doLISTUSERS(String argument) {
        writeLoggedResponse("Existing accounts " + users.countUsers());
        for (Iterator it = users.list(); it.hasNext();) {
           writeLoggedResponse("user: " + (String) it.next());
        }
        out.flush();
        return true;
    }

    /**
     * Handler method called upon receipt of an COUNTUSERS command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the argument passed in with the command
     */
    private boolean doCOUNTUSERS(String argument) {
        writeLoggedFlushedResponse("Existing accounts " + users.countUsers());
        return true;
    }

    /**
     * Handler method called upon receipt of an VERIFY command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the argument passed in with the command
     */
    private boolean doVERIFY(String argument) {
        String user = argument;
        if (user == null || user.equals("")) {
            writeLoggedFlushedResponse("Usage: verify [username]");
            return true;
        }
        if (users.contains(user)) {
            StringBuffer responseBuffer =
                new StringBuffer(64)
                        .append("User ")
                        .append(user)
                        .append(" exists");
            String response = responseBuffer.toString();
            writeLoggedResponse(response);
        } else {
            StringBuffer responseBuffer =
                new StringBuffer(64)
                        .append("User ")
                        .append(user)
                        .append(" does not exist");
            String response = responseBuffer.toString();
            writeLoggedResponse(response);
        }
        out.flush();
        return true;
    }

    /**
     * Handler method called upon receipt of an HELP command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the argument passed in with the command
     */
    private boolean doHELP(String argument) {
        out.println("Currently implemented commands:");
        out.println("help                                           display this help");
        out.println("listusers                                      display existing accounts");
        out.println("countusers                                     display the number of existing accounts");
        out.println("adduser [username] [password]                  add a new user");
        out.println("verify [username]                              verify if specified user exist");
        out.println("deluser [username]                             delete existing user");
        out.println("setpassword [username] [password]              sets a user's password");
        out.println("setalias [user] [alias]                        locally forwards all email for 'user' to 'alias'");
        out.println("showalias [username]                           shows a user's current email alias");
        out.println("unsetalias [user]                              unsets an alias for 'user'");
        out.println("setforwarding [username] [emailaddress]        forwards a user's email to another email address");
        out.println("showforwarding [username]                      shows a user's current email forwarding");
        out.println("unsetforwarding [username]                     removes a forward");
        out.println("user [repositoryname]                          change to another user repository");
        out.println("listspool [spoolrepositoryname]                list all mails which reside in the spool and have an error state");
        out.println("flushspool [spoolrepositoryname] ([key])       try to resend the mail assing to the given key. If no key is given all mails get resend");
        out.println("deletespool [spoolrepositoryname] ([key])      delete the mail assign to the given key. If no key is given all mails get deleted");
        out.println("addham dir/mbox [directory/mbox]               feed the BayesianAnalysisFeeder with the content of the directory or mbox file as HAM");
        out.println("addspam dir/mbox [directory/mbox]              feed the BayesianAnalysisFeeder with the content of the directory or mbox file as SPAM");
        out.println("exportbayesiandata [file]                      export the BayesianAnalysis data to a xml file");
        out.println("importbayesiandata [file]                      import the BayesianAnalysis data from a xml file");
        out.println("memstat ([-gc])                                shows memory usage. When called with -gc the garbage collector get called");
        out.println("shutdown                                       kills the current JVM (convenient when James is run as a daemon)");
        out.println("quit                                           close connection");
        out.flush();
        return true;

    }

    /**
     * Handler method called upon receipt of an SETALIAS command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the argument passed in with the command
     */
    private boolean doSETALIAS(String argument) {
        int breakIndex = -1;
        if ((argument == null) ||
            (argument.equals("")) ||
            ((breakIndex = argument.indexOf(" ")) < 0)) {
            writeLoggedFlushedResponse("Usage: setalias [username] [emailaddress]");
            return true;
        }
        String username = argument.substring(0,breakIndex);
        String alias = argument.substring(breakIndex + 1);
        if (username.equals("") || alias.equals("")) {
            writeLoggedFlushedResponse("Usage: setalias [username] [alias]");
            return true;
        }

        User baseuser = users.getUserByName(username);
        if (baseuser == null) {
            writeLoggedFlushedResponse("No such user " + username);
            return true;
        }
        if (! (baseuser instanceof JamesUser ) ) {
            writeLoggedFlushedResponse("Can't set alias for this user type.");
            return true;
        }

        JamesUser user = (JamesUser) baseuser;
        JamesUser aliasUser = (JamesUser) users.getUserByName(alias);
        if (aliasUser == null) {
            writeLoggedFlushedResponse("Alias unknown to server - create that user first.");
            return true;
        }

        boolean success = user.setAlias(alias);
        if (success) {
            user.setAliasing(true);
            users.updateUser(user);
            StringBuffer responseBuffer =
                new StringBuffer(64)
                        .append("Alias for ")
                        .append(username)
                        .append(" set to:")
                        .append(alias);
            String response = responseBuffer.toString();
            out.println(response);
            getLogger().info(response);
        } else {
            out.println("Error setting alias");
            getLogger().error("Error setting alias");
        }
        out.flush();
        return true;
    }

    /**
     * Handler method called upon receipt of an SETFORWARDING command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the argument passed in with the command
     */
    private boolean doSETFORWARDING(String argument) {
        int breakIndex = -1;
        if ((argument == null) ||
            (argument.equals("")) ||
            ((breakIndex = argument.indexOf(" ")) < 0)) {
            writeLoggedFlushedResponse("Usage: setforwarding [username] [emailaddress]");
            return true;
        }
        String username = argument.substring(0,breakIndex);
        String forward = argument.substring(breakIndex + 1);
        if (username.equals("") || forward.equals("")) {
           writeLoggedFlushedResponse("Usage: setforwarding [username] [emailaddress]");
           return true;
        }
        // Verify user exists
        User baseuser = users.getUserByName(username);
        if (baseuser == null) {
            writeLoggedFlushedResponse("No such user " + username);
            return true;
        } else if (! (baseuser instanceof JamesUser ) ) {
            writeLoggedFlushedResponse("Can't set forwarding for this user type.");
            return true;
        }
        JamesUser user = (JamesUser)baseuser;
        // Verify acceptable email address
        MailAddress forwardAddr;
        try {
             forwardAddr = new MailAddress(forward);
        } catch(ParseException pe) {
            writeLoggedResponse("Parse exception with that email address: " + pe.getMessage());
            writeLoggedFlushedResponse("Forwarding address not added for " + username);
            return true;
        }

        boolean success = user.setForwardingDestination(forwardAddr);
        if (success) {
            user.setForwarding(true);
            users.updateUser(user);
            StringBuffer responseBuffer =
                new StringBuffer(64)
                        .append("Forwarding destination for ")
                        .append(username)
                        .append(" set to:")
                        .append(forwardAddr.toString());
            String response = responseBuffer.toString();
            out.println(response);
            getLogger().info(response);
        } else {
            out.println("Error setting forwarding");
            getLogger().error("Error setting forwarding");
        }
        out.flush();
        return true;
    }

    /**
     * Handler method called upon receipt of an SHOWALIAS command.
     * Returns whether further commands should be read off the wire.
     *
     * @param username the user name
     */
    private boolean doSHOWALIAS(String username) {
        if ( username == null || username.equals("") ) {
            writeLoggedFlushedResponse("Usage: showalias [username]");
            return true;
        }


        User baseuser = users.getUserByName(username);
        if (baseuser == null) {
            writeLoggedFlushedResponse("No such user " + username);
            return true;
        } else if (! (baseuser instanceof JamesUser ) ) {
            writeLoggedFlushedResponse("Can't show aliases for this user type.");
            return true;
        }

        JamesUser user = (JamesUser)baseuser;
        if ( user == null ) {
            writeLoggedFlushedResponse("No such user " + username);
            return true;
        }

        if ( !user.getAliasing() ) {
            writeLoggedFlushedResponse("User " + username + " does not currently have an alias");
            return true;
        }

        String alias = user.getAlias();

        if ( alias == null || alias.equals("") ) {    //  defensive programming -- neither should occur
            String errmsg = "For user " + username + ", the system indicates that aliasing is set but no alias was found";
            out.println(errmsg);
            getLogger().error(errmsg);
            return true;
        }

        writeLoggedFlushedResponse("Current alias for " + username + " is: " + alias);
        return true;
    }

    /**
     * Handler method called upon receipt of an SHOWFORWARDING command.
     * Returns whether further commands should be read off the wire.
     *
     * @param username the user name
     */
    private boolean doSHOWFORWARDING(String username) {
        if ( username == null || username.equals("") ) {
            writeLoggedFlushedResponse("Usage: showforwarding [username]");
            return true;
        }

        // Verify user exists
        User baseuser = users.getUserByName(username);
        if (baseuser == null) {
            writeLoggedFlushedResponse("No such user " + username);
            return true;
        } else if (! (baseuser instanceof JamesUser ) ) {
            writeLoggedFlushedResponse("Can't set forwarding for this user type.");
            return true;
        }
        JamesUser user = (JamesUser)baseuser;
        if ( user == null ) {
            writeLoggedFlushedResponse("No such user " + username);
            return true;
        }

        if ( !user.getForwarding() ) {
            writeLoggedFlushedResponse("User " + username + " is not currently being forwarded");
            return true;
        }

        MailAddress fwdAddr = user.getForwardingDestination();

        if ( fwdAddr == null ) {    //  defensive programming -- should not occur
            String errmsg = "For user " + username + ", the system indicates that forwarding is set but no forwarding destination was found";
            out.println(errmsg);
            getLogger().error(errmsg);
            return true;
        }

        writeLoggedFlushedResponse("Current forwarding destination for " + username + " is: " + fwdAddr);
        return true;
    }

    /**
     * Handler method called upon receipt of an UNSETALIAS command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the argument passed in with the command
     */
    private boolean doUNSETALIAS(String argument) {
        if ((argument == null) || (argument.equals(""))) {
            writeLoggedFlushedResponse("Usage: unsetalias [username]");
            return true;
        }
        String username = argument;
        JamesUser user = (JamesUser) users.getUserByName(username);
        if (user == null) {
            writeLoggedResponse("No such user " + username);
        } else if (user.getAliasing()){
            user.setAliasing(false);
            users.updateUser(user);
            StringBuffer responseBuffer =
                new StringBuffer(64)
                        .append("Alias for ")
                        .append(username)
                        .append(" unset");
            String response = responseBuffer.toString();
            out.println(response);
            getLogger().info(response);
        } else {
            writeLoggedResponse("Aliasing not active for" + username);
        }
        out.flush();
        return true;
    }

    /**
     * Handler method called upon receipt of an UNSETFORWARDING command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the argument passed in with the command
     */
    private boolean doUNSETFORWARDING(String argument) {
        if ((argument == null) || (argument.equals(""))) {
            writeLoggedFlushedResponse("Usage: unsetforwarding [username]");
            return true;
        }
        String username = argument;
        JamesUser user = (JamesUser) users.getUserByName(username);
        if (user == null) {
            writeLoggedFlushedResponse("No such user " + username);
        } else if (user.getForwarding()){
            user.setForwarding(false);
            users.updateUser(user);
            StringBuffer responseBuffer =
                new StringBuffer(64)
                        .append("Forward for ")
                        .append(username)
                        .append(" unset");
            String response = responseBuffer.toString();
            out.println(response);
            out.flush();
            getLogger().info(response);
        } else {
            writeLoggedFlushedResponse("Forwarding not active for" + username);
        }
        return true;
    }

    /**
     * Handler method called upon receipt of a USER command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the argument passed in with the command
     */
    private boolean doUSER(String argument) {
        if (argument == null || argument.equals("")) {
            writeLoggedFlushedResponse("Usage: user [repositoryName]");
            return true;
        }
        String repositoryName = argument.toLowerCase(Locale.US);
        UsersRepository repos = theConfigData.getUserStore().getRepository(repositoryName);
        if ( repos == null ) {
            writeLoggedFlushedResponse("No such repository: " + repositoryName);
        } else {
            users = repos;
            StringBuffer responseBuffer =
                new StringBuffer(64)
                        .append("Changed to repository '")
                        .append(repositoryName)
                        .append("'.");
            writeLoggedFlushedResponse(responseBuffer.toString());
        }
        return true;
    }

    /**
     * Handler method called upon receipt of a LISTSPOOL command. Returns
     * whether further commands should be read off the wire.
     * 
     * @param argument
     *            the argument passed in with the command
     */
    private boolean doLISTSPOOL(String argument) {
        int count = 0;

        // check if the command was called correct
        if ((argument == null) || (argument.trim().equals(""))) {
            writeLoggedFlushedResponse("Usage: LISTSPOOL [spoolrepositoryname]");
            return true;
        }

        String url = argument;

        try {
            List spoolItems = theConfigData.getSpoolManagement().getSpoolItems(url, SpoolFilter.ERRORMAIL_FILTER);
            count = spoolItems.size();
            if (count > 0) out.println("Messages in spool:");
            for (Iterator iterator = spoolItems.iterator(); iterator.hasNext();) {
                String item = (String) iterator.next();
                out.println(item);
                out.flush();
            }
            out.println("Number of spooled mails: " + count);
            out.flush();
        } catch (Exception e) {
            out.println("Error opening the spoolrepository " + e.getMessage());
            out.flush();
            getLogger().error(
                    "Error opening the spoolrepository " + e.getMessage());
        }
        return true;
    }

    /**
     * Handler method called upon receipt of a LISTSPOOL command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the argument passed in with the command
     */
    private boolean doFLUSHSPOOL(String argument) {
        int count = 0;
        String[] args = null;

        if (argument != null)
            args = argument.split(" ");

        // check if the command was called correct
        if ((argument == null || argument.trim().equals(""))
                || (args.length < 1 || args.length > 2)) {
            writeLoggedFlushedResponse("Usage: FLUSHSPOOL [spoolrepositoryname] ([key])");
            return true;
        }

        String url = args[0];
        String key = args.length == 2 ? args[1] : null;
        try {
            count = theConfigData.getSpoolManagement().resendSpoolItems(url, key, null, SpoolFilter.ERRORMAIL_FILTER);
            out.println("Number of flushed mails: " + count);
            out.flush();

        } catch (Exception e) {
            out
                    .println("Error accessing the spoolrepository "
                            + e.getMessage());
            out.flush();
            getLogger().error(
                    "Error accessing the spoolrepository " + e.getMessage());
        }
        return true;
    }

    /**
     * Handler method called upon receipt of a DELETESPOOL command. Returns
     * whether further commands should be read off the wire.
     * 
     * @param argument
     *            the argument passed in with the command
     */
    private boolean doDELETESPOOL(String argument) {
        String[] args = null;

        if (argument != null)
            args = argument.split(" ");

        // check if the command was called correct
        if ((argument == null || argument.trim().equals(""))
                || (args.length < 1 || args.length > 2)) {
            writeLoggedFlushedResponse("Usage: DELETESPOOL [spoolrepositoryname] ([key])");
            return true;
        }

        String url = args[0];
        String key = args.length == 2 ? args[1] : null;

        try {
            ArrayList lockingFailures = new ArrayList();
            int count =  theConfigData.getSpoolManagement().removeSpoolItems(url, key, lockingFailures, SpoolFilter.ERRORMAIL_FILTER);

            for (Iterator iterator = lockingFailures.iterator(); iterator.hasNext();) {
                String lockFailureKey = (String) iterator.next();
                out.println("Error locking the mail with key:  " + lockFailureKey);
            }
            out.flush();

            out.println("Number of deleted mails: " + count);
            out.flush();

        } catch (Exception e) {
            out.println("Error opening the spoolrepository " + e.getMessage());
            out.flush();
            getLogger().error("Error opeing the spoolrepository " + e.getMessage());
        }
        return true;
    }

    /**
     * Handler method called upon receipt of a QUIT command. Returns whether
     * further commands should be read off the wire.
     * 
     * @param argument
     *            the argument passed in with the command
     */
    private boolean doQUIT(String argument) {
        writeLoggedFlushedResponse("Bye");
        return false;
    }

    /**
     * Handler method called upon receipt of a SHUTDOWN command. Returns whether
     * further commands should be read off the wire.
     * 
     * @param argument
     *            the argument passed in with the command
     */
    private boolean doSHUTDOWN(String argument) {
        writeLoggedFlushedResponse("Shutting down, bye bye");
        System.exit(0);
        return false;
    }

    /**
     * Handler method called upon receipt of an unrecognized command. Returns
     * whether further commands should be read off the wire.
     * 
     * @param argument
     *            the unknown command
     */
    private boolean doUnknownCommand(String argument) {
        writeLoggedFlushedResponse("Unknown command " + argument);
        return true;
    }
    
    /**
     * Handler method called upon receipt of a ADDHAM command. Returns
     * whether further commands should be read off the wire.
     * 
     * @param argument
     *            the argument passed in with the command
     */
    private boolean doADDHAM(String argument) {
        String [] args = null;
        int count = 0;
        
        if (argument != null) {
            args = argument.split(" "); 
        }
        
        // check if the command was called correct
        if (argument == null || argument.trim().equals("") || (args != null && args.length != 2)) {
            writeLoggedFlushedResponse("Usage: ADDHAM DIR/MBOX [dir/mbox]");
            return true;
        }

        try {
            
            // stop watchdog cause feeding can take some time
            theWatchdog.stop();  
            
            if (args[0].equalsIgnoreCase("DIR")) {
                count = theConfigData.getBayesianAnalyzerManagement().addHamFromDir(args[1]);
            } else if (args[0].equalsIgnoreCase("MBOX")) {
                count = theConfigData.getBayesianAnalyzerManagement().addHamFromMbox(args[1]);
            } else {
                writeLoggedFlushedResponse("Usage: ADDHAM DIR/MBOX [dir/mbox]");
                return true;
            }
            out.println("Feed the BayesianAnalysis with " + count + " HAM");
            out.flush();
        
        } catch (BayesianAnalyzerManagementException e) {
            getLogger().error("Error on feeding BayesianAnalysis: " + e);
            out.println("Error on feeding BayesianAnalysis: " + e);
            out.flush();
            return true;
        } finally {
            theWatchdog.start();
        }
    
        return true;
    }
    
    /**
     * Handler method called upon receipt of a ADDSPAM command. Returns
     * whether further commands should be read off the wire.
     * 
     * @param argument
     *            the argument passed in with the command
     */
    private boolean doADDSPAM(String argument) {
        String [] args = null;
        int count = 0;
        
        if (argument != null) {
            args = argument.split(" "); 
        }
        // check if the command was called correct
        if (argument == null || argument.trim().equals("") || (args != null && args.length != 2)) {
            writeLoggedFlushedResponse("Usage: ADDSPAM DIR/MBOX [dir/mbox]");
            return true;
        }

        try {
            
            // stop watchdog cause feeding can take some time
            theWatchdog.stop();
            
            if (args[0].equalsIgnoreCase("DIR")) {
                count = theConfigData.getBayesianAnalyzerManagement().addSpamFromDir(args[1]);
            } else if (args[0].equalsIgnoreCase("MBOX")) {
                count = theConfigData.getBayesianAnalyzerManagement().addSpamFromMbox(args[1]);
            } else {
                writeLoggedFlushedResponse("Usage: ADDHAM DIR/MBOX [dir/mbox]");
                return true;
            }
            out.println("Feed the BayesianAnalysis with " + count + " SPAM");
            out.flush();
            
        } catch (BayesianAnalyzerManagementException e) {
            getLogger().error("Error on feeding BayesianAnalysis: " + e);
            out.println("Error on feeding BayesianAnalysis: " + e);
            out.flush();
            return true;
        } finally {
            theWatchdog.start();
        }
    
        return true;
    }
    
   
    
    private boolean doEXPORTBAYESIANDATA(String argument) {
        // check if the command was called correct
        if (argument == null || argument.trim().equals("")) {
            writeLoggedFlushedResponse("Usage: EXPORTBAYESIANALYZERDATA [dir]");
            return true;
        }

        try {
            
            // stop watchdog cause feeding can take some time
            theWatchdog.stop();
            
            theConfigData.getBayesianAnalyzerManagement().exportData(argument);
            out.println("Exported the BayesianAnalysis data");
            out.flush();

        } catch (BayesianAnalyzerManagementException e) {
            getLogger().error("Error on exporting BayesianAnalysis data: " + e);
            out.println("Error on exporting BayesianAnalysis data: " + e);
            out.flush();
            return false;
        } finally {
            theWatchdog.start();
        }
    
        // check if any exception was thrown
        return true;
    }
    
    private boolean doIMPORTBAYESIANDATA(String argument) {
        // check if the command was called correct
        if (argument == null || argument.trim().equals("")) {
            writeLoggedFlushedResponse("Usage: IMPORTBAYESIANALYZERDATA [dir]");
            return true;
        }

        try {
            
            // stop watchdog cause feeding can take some time
            theWatchdog.stop();
            
            theConfigData.getBayesianAnalyzerManagement().importData(argument);
            out.println("Imported the BayesianAnalysis data");
            out.flush();

        } catch (BayesianAnalyzerManagementException e) {
            getLogger().error("Error on importing BayesianAnalysis data: " + e);
            out.println("Error on importing BayesianAnalysis data: " + e);
            out.flush();
            return false;
        } finally {
            theWatchdog.start();
        }
    
        return true;
    }
}
