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
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.mail.internet.ParseException;

import org.apache.james.Constants;
import org.apache.james.api.user.JamesUser;
import org.apache.james.api.user.User;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.api.vut.management.VirtualUserTableManagementException;
import org.apache.james.management.BayesianAnalyzerManagementException;
import org.apache.james.management.DomainListManagementException;
import org.apache.james.management.SpoolFilter;
import org.apache.james.services.ProcessorManagementService;
import org.apache.james.socket.AbstractJamesHandler;
import org.apache.mailet.MailAddress;


/**
 * Provides a console-based administration interface covering most of the management 
 * functionality found in the classes from package org.apache.james.management
 * 
 * TODO: -improve protocol
 *       -much more...
 *
 * @version $Revision$
 *
 */
public class RemoteManagerHandler
    extends AbstractJamesHandler {

    private static final Class[] WORKER_METHOD_PARAMETERSET = new Class[] {String.class};

    private static final List COMMANDLIST = Arrays.asList(new String[] { 
        "MEMSTAT",
        "ADDUSER",
        "SETPASSWORD",
        "DELUSER",
        "LISTUSERS",
        "COUNTUSERS",
        "VERIFY",
        "HELP",
        "SETFORWARDING",
        "SHOWFORWARDING",
        "UNSETFORWARDING",
        "SETALIAS",
        "SHOWALIAS",
        "UNSETALIAS",
        "USER",
        "LISTSPOOL",
        "FLUSHSPOOL",
        "DELETESPOOL",
        "MOVEMAILS",
        "ADDHAM",
        "ADDSPAM",
        "EXPORTBAYESIANDATA",
        "IMPORTBAYESIANDATA",
        "RESETBAYESIANDATA",
        "LISTPROCESSORS",
        "LISTMAILETS",
        "LISTMATCHERS",
        "SHOWMAILETINFO",
        "SHOWMATCHERINFO",
        "ADDMAPPING",
        "REMOVEMAPPING",
        "LISTMAPPING",
        "LISTALLMAPPINGS",
        "ADDDOMAIN",
        "REMOVEDOMAIN",
        "LISTDOMAINS",
        "QUIT",
        "SHUTDOWN"
    });

    /**
     * The per-service configuration data that applies to all handlers
     */
    private RemoteManagerHandlerConfigurationData theConfigData;

    /**
     * The current UsersRepository being managed/viewed/modified
     */
    private UsersRepository users;
    
    private CommandRegistry commandRegistry;
    
    private final static String HEADER_IDENTIFIER = "header=";
    private final static String REGEX_IDENTIFIER = "regex=";
    private final static String KEY_IDENTIFIER = "key=";

    private final static String ADD_MAPPING_ACTION = "ADD_MAPPING";
    private final static String REMOVE_MAPPING_ACTION = "REMOVE_MAPPING";

    
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
            
            Command[] commands = theConfigData.getCommands();
            commandRegistry = new CommandRegistry(commands);
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
     * @see org.apache.james.socket.AbstractJamesHandler#errorHandler(java.lang.RuntimeException)
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
        
        if (!COMMANDLIST.contains(command)) {
            final boolean result = commandRegistry.execute(command, argument, out);
            out.flush();
            return result;
        }
        
        try {
            Method method = getClass().getDeclaredMethod("do"+command, WORKER_METHOD_PARAMETERSET);
            Boolean returnFlag = (Boolean)method.invoke(this, new Object[] {argument});
            return returnFlag.booleanValue();
        } catch (SecurityException e) {
            writeLoggedFlushedResponse("could not determine executioner of command " + command);
        } catch (NoSuchMethodException e) {
            return doUnknownCommand(rawCommand);
        } catch (Exception e) {
            e.printStackTrace();
            writeLoggedFlushedResponse("could not execute command " + command);
        }
        return false;
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
            if((username.indexOf("@") < 0) == false) {
                if(theConfigData.getMailServer().supportVirtualHosting() == false) {
                    out.println("Virtualhosting not supported");
                    out.flush();
                    return true;
                }
                String domain = username.split("@")[1];
                if (theConfigData.getDomainListManagement().containsDomain(domain) == false) {
                    out.println("Domain not exists: " + domain);
                    out.flush();
                    return true;
                }
            }
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
        if (argument == null) {
            writeLoggedResponse("Existing accounts " + users.countUsers());
            for (Iterator it = users.list(); it.hasNext();) {
               writeLoggedResponse("user: " + (String) it.next());
            }
            out.flush();
            return true;
        } else {
            if(theConfigData.getMailServer().supportVirtualHosting() == false) {
                out.println("Virtualhosting not supported");
                out.flush();
                return true;
            }
        
            ArrayList userList = getDomainUserList(argument);
            writeLoggedResponse("Existing accounts from domain " + argument + " " + userList.size());
            for (int i = 0; i <userList.size(); i++) {
                writeLoggedResponse("user: " + userList.get(i));
            }
            out.flush();
            return true;
        }
    }

    /**
     * Handler method called upon receipt of an COUNTUSERS command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the argument passed in with the command
     */
    private boolean doCOUNTUSERS(String argument) {
        if (argument == null) {
            writeLoggedFlushedResponse("Existing accounts " + users.countUsers());
            return true;
        } else {
            if(theConfigData.getMailServer().supportVirtualHosting() == false) {
                out.println("Virtualhosting not supported");
                out.flush();
                return true;
           }
            
           writeLoggedFlushedResponse("Existing accounts for domain " + argument + " " + getDomainUserList(argument).size());
           return true;
        }
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
     * Handler method called upon receipt of a HELP command.
     * Returns whether further commands should be read off the wire.
     *
     * @param argument the argument passed in with the command
     */
    private boolean doHELP(String argument) {
        out.println("Currently implemented commands:");
        out.println("help                                                                    display this help");
        out.println("listusers                                                               display existing accounts");
        out.println("countusers                                                              display the number of existing accounts");
        out.println("adduser [username] [password]                                           add a new user");
        out.println("verify [username]                                                       verify if specified user exist");
        out.println("deluser [username]                                                      delete existing user");
        out.println("setpassword [username] [password]                                       sets a user's password");
        out.println("setalias [user] [alias]                                                 locally forwards all email for 'user' to 'alias'");
        out.println("showalias [username]                                                    shows a user's current email alias");
        out.println("unsetalias [user]                                                       unsets an alias for 'user'");
        out.println("setforwarding [username] [emailaddress]                                 forwards a user's email to another email address");
        out.println("showforwarding [username]                                               shows a user's current email forwarding");
        out.println("unsetforwarding [username]                                              removes a forward");
        out.println("user [repositoryname]                                                   change to another user repository");
        out.println("addmapping ([table=virtualusertablename]) [user@domain] [mapping]       add mapping for the given emailaddress");
        out.println("removemapping ([table=virtualusertablename]) [user@domain] [mapping]    remove mapping for the given emailaddress");
        out.println("listmapping ([table=virtualusertablename]) [user@domain]                list all mappings for the given emailaddress");
        out.println("listallmappings ([table=virtualusertablename])                          list all mappings");
        out.println("adddomain [domainname]                                                  add domain to local domains");
        out.println("removedomain [domainname]                                               remove domain from local domains");
        out.println("listdomains                                                             list local domains");
        out.println("listspool [spoolrepositoryname] ([header=name] [regex=value])           list all mails which reside in the spool and have an error state");
        out.println("flushspool [spoolrepositoryname] ([key] | [header=name] [regex=value])  try to resend the mail assing to the given key. If no key is given all mails get resend");
        out.println("deletespool [spoolrepositoryname] ([key] | [header=name] [regex=value]) delete the mail assigned to the given key. If no key is given all mails get deleted");
        out.println("movemails [srcSpoolrepositoryname] [dstSpoolrepositoryname] ([header=headername] [regex=regexValue])");
        out.println("    [srcstate=sourcestate] [dststate=destinationstate]                  move mails from the source repository to the destination repository.");
        out.println("listprocessors [processorname]                                          list names of all processors");
        out.println("listmailets [processorname]                                             list names of all mailets for specified processor");
        out.println("listmatchers [processorname]                                            list names of all mailets for specified processor");
        out.println("showmailetinfo [processorname] [#index]                                 shows configuration for mailet of specified processor at given index");
        out.println("showmatcherinfo [processorname] [#index]                                shows configuration for matcher of specified processor at given index");
        out.println("addham dir/mbox [directory/mbox]                                        feed the BayesianAnalysisFeeder with the content of the directory or mbox file as HAM");
        out.println("addspam dir/mbox [directory/mbox]                                       feed the BayesianAnalysisFeeder with the content of the directory or mbox file as SPAM");
        out.println("exportbayesiandata [file]                                               export the BayesianAnalysis data to a xml file");
        out.println("resetbayesiandata                                                       reset trained BayesianAnalysis data");
        out.println("memstat ([-gc])                                                         shows memory usage. When called with -gc the garbage collector get called");
        out.println("shutdown                                                                kills the current JVM (convenient when James is run as a daemon)");
        out.println("quit                                                                    close connection");
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
        String[] args = null;
        String headername = null;
        String regex = null;
        
        if (argument != null) args = argument.split(" ");

        // check if the command was called correct
        if ((argument == null) || (argument.trim().equals("")) || args.length < 1 || args.length > 3 || (args.length > 1 && !args[1].startsWith(HEADER_IDENTIFIER)) || (args.length > 2 && !args[2].startsWith(REGEX_IDENTIFIER))) {
            writeLoggedFlushedResponse("Usage: LISTSPOOL [spoolrepositoryname] ([header=headername] [regex=regexValue])");
            return true;
        }

        String url = args[0];
        
        if (args.length > 1) { 
            headername = args[1].substring(HEADER_IDENTIFIER.length());
            regex = args[2].substring(REGEX_IDENTIFIER.length());
        }
        
        try {
            List spoolItems;
            
            if (headername == null || regex == null) {
                spoolItems = theConfigData.getSpoolManagement().getSpoolItems(url, SpoolFilter.ERRORMAIL_FILTER);
            } else {
                spoolItems = theConfigData.getSpoolManagement().getSpoolItems(url, new SpoolFilter(SpoolFilter.ERROR_STATE,headername,regex));
            }
            
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
                || (args.length < 1 || args.length > 3 || (!args[1].startsWith(KEY_IDENTIFIER) && (args.length > 1  
                && !args[1].startsWith(HEADER_IDENTIFIER))) || (args.length == 3 && !args[2].startsWith(REGEX_IDENTIFIER)))) {
            writeLoggedFlushedResponse("Usage: FLUSHSPOOL [spoolrepositoryname] ([key=mKey] | [header=headername] [regex=regexValue] )");
            return true;
        }

        String url = args[0];
        String key = null;
        String header = null;
        String regex = null;
        
        if (args[1].startsWith(KEY_IDENTIFIER)) {
            key = args[1].substring(KEY_IDENTIFIER.length()); 
        } else {
            header = args[1].substring(HEADER_IDENTIFIER.length());
            regex = args[2].substring(REGEX_IDENTIFIER.length()); 
        }
        
        try {
            if (key != null) {
                count = theConfigData.getSpoolManagement().resendSpoolItems(url, key, null, SpoolFilter.ERRORMAIL_FILTER);
            } else {
                count = theConfigData.getSpoolManagement().resendSpoolItems(url, key, null, new SpoolFilter(SpoolFilter.ERROR_STATE,header,regex));
            }
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
                || (args.length < 1 || args.length > 3 || (args.length > 1 && (!args[1].startsWith(KEY_IDENTIFIER) 
                && !args[1].startsWith(HEADER_IDENTIFIER))) || (args.length == 3 && !args[2].startsWith(REGEX_IDENTIFIER)))) {
            writeLoggedFlushedResponse("Usage: DELETESPOOL [spoolrepositoryname] ([key=mKey] | [header=headername] [regex=regexValue])");
            return true;
        }

        String url = args[0];
        String key = null;
        String header = null;
        String regex = null;

        if (args[1].startsWith(KEY_IDENTIFIER)) {
            key = args[1].substring(KEY_IDENTIFIER.length()); 
        } else {
            header = args[1].substring(HEADER_IDENTIFIER.length());
            regex = args[2].substring(REGEX_IDENTIFIER.length()); 
        }

        try {
            ArrayList lockingFailures = new ArrayList();
            int count = 0;
            
            if (key != null) {
                count = theConfigData.getSpoolManagement().removeSpoolItems(url, key, lockingFailures, SpoolFilter.ERRORMAIL_FILTER);
            } else {
                count = theConfigData.getSpoolManagement().removeSpoolItems(url, key, lockingFailures, new SpoolFilter(SpoolFilter.ERROR_STATE,header,regex));
            }
            
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
     * Handler method called upon receipt of a MOVEMAILS command. Returns
     * whether further commands should be read off the wire.
     * 
     * @param argument
     *            the argument passed in with the command
     */
    private boolean doMOVEMAILS(String argument) {
        String[] args = null;

        if (argument != null)
            args = argument.split(" ");

        // check if the command was called correct
        if ((argument == null || argument.trim().equals(""))
                || (args.length < 2 || args.length > 6)) {
            writeLoggedFlushedResponse("Usage: MOVEMAILS [srcSpoolrepositoryname] [dstSpoolrepositoryname] ([header=headername] [regex=regexValue]) [srcstate=sourcestate] [dststate=destinationstate]");
            return true;
        }

        String srcUrl = args[0];
        String dstUrl = args[1];
        
        String dstState = null;
        String srcState = null;
        String header = null;
        String regex = null;

        for (int i = 2; i < args.length; i++) {
            if (args[i].startsWith(HEADER_IDENTIFIER)) {
                header = args[i].substring(HEADER_IDENTIFIER.length());
            } else if (args[i].startsWith(REGEX_IDENTIFIER)) {
                header = args[i].substring(REGEX_IDENTIFIER.length());
            } else if (args[i].startsWith("srcstate=")) {
                header = args[i].substring("srcstate=".length());
            } else if (args[i].startsWith("dststate=")) {
                header = args[i].substring("dststate=".length());
            } else {
                writeLoggedResponse("Unexpected parameter "+args[i]);
                writeLoggedFlushedResponse("Usage: MOVEMAILS [srcSpoolrepositoryname] [dstSpoolrepositoryname] ([header=headername] [regex=regexValue]) [srcstate=sourcestate] [dststate=destinationstate]");
                return true;
            }
        }
        
        if ((header != null && regex == null) || (header == null && regex != null)) {
            if (regex == null) {
                writeLoggedResponse("Bad parameters: used header without regex");
            } else {
                writeLoggedResponse("Bad parameters: used regex without header");
            }
            writeLoggedFlushedResponse("Usage: MOVEMAILS [srcSpoolrepositoryname] [dstSpoolrepositoryname] ([header=headername] [regex=regexValue]) [srcstate=sourcestate] [dststate=destinationstate]");
            return true;
        }

        try {
            int count = theConfigData.getSpoolManagement().moveSpoolItems(srcUrl, dstUrl, dstState, new SpoolFilter(srcState,header,regex));
            
            out.println("Number of moved mails: " + count);
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
            return true;
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
            return true;
        } finally {
            theWatchdog.start();
        }
    
        return true;
    }
    
    private boolean doRESETBAYESIANDATA(String argument) {

        try {           
            // stop watchdog cause feeding can take some time
            theWatchdog.stop();
            
            theConfigData.getBayesianAnalyzerManagement().resetData();
            out.println("Reseted the BayesianAnalysis data");
            out.flush();

        } catch (BayesianAnalyzerManagementException e) {
            getLogger().error("Error on reseting BayesianAnalysis data: " + e);
            out.println("Error on reseting BayesianAnalysis data: " + e);
            out.flush();
            return true;
        } finally {
            theWatchdog.start();
        }
    
        return true;
    }

    private boolean doLISTPROCESSORS(String argument) {
        String[] processorNames = theConfigData.getProcessorManagement().getProcessorNames();
        writeLoggedResponse("Existing processors: " + processorNames.length);
        for (int i = 0; i < processorNames.length; i++) {
            writeLoggedResponse("\t" + processorNames[i]);
         }
        return true;
    }

    private boolean processorExists(String name) {
        name = name.toLowerCase(Locale.US);
        List processorList = Arrays.asList(theConfigData.getProcessorManagement().getProcessorNames());
        return processorList.contains(name);
    }
    
    private boolean doLISTMAILETS(String argument) {
        ProcessorManagementService processorManagement = theConfigData.getProcessorManagement();
        if (argument == null || !processorExists(argument)) {
            writeLoggedFlushedResponse("Usage: LISTMAILETS [processor]");
            writeLoggedFlushedResponse("The list of valid processor names can be retrieved using command LISTPROCESSORS");
            return true;
        }
        String[] mailetNames = processorManagement.getMailetNames(argument);
        writeLoggedResponse("Existing mailets in processor: " + mailetNames.length);
        for (int i = 0; i < mailetNames.length; i++) {
            writeLoggedResponse((i+1) + ". " + mailetNames[i]);
         }
        return true;
    }

    private boolean doLISTMATCHERS(String argument) {
        ProcessorManagementService processorManagement = theConfigData.getProcessorManagement();
        if (argument == null || !processorExists(argument)) {
            writeLoggedFlushedResponse("Usage: LISTMATCHERS [processor]");
            writeLoggedFlushedResponse("The list of valid processor names can be retrieved using command LISTPROCESSORS");
            return true;
        }
        String[] matcherNames = processorManagement.getMatcherNames(argument);
        writeLoggedResponse("Existing matchers in processor: " + matcherNames.length);
        for (int i = 0; i < matcherNames.length; i++) {
            writeLoggedResponse((i+1) + ". " + matcherNames[i]);
         }
        return true;
    }

    private Object[] extractMailetInfoParameters(String argument, String commandHelp) {
        String[] argList = argument.split(" ");
        boolean argListOK = argument != null && argList != null && argList.length == 2;
        if (!argListOK) {
            writeLoggedFlushedResponse("Usage: SHOW" + commandHelp + "INFO [processor] [#index]");
            return null;
        }
        String processorName = argList[0];
        if (!processorExists(processorName)) {
            writeLoggedFlushedResponse("The list of valid processor names can be retrieved using command LISTPROCESSORS");
            return null;
        }
        int index = -1;
        try {
            index = Integer.parseInt(argList[1]) - 1;
        } catch (NumberFormatException e) {
            // fall thru with -1
        }
        if (index < 0) {
            writeLoggedFlushedResponse("The index parameter must be a positive number");
            return null;
        }
        
        return new Object[] {processorName, new Integer(index)};
    }

    private boolean doSHOWMAILETINFO(String argument) {
        ProcessorManagementService processorManagement = theConfigData.getProcessorManagement();
        Object[] parameters = extractMailetInfoParameters(argument, "MAILET");
        if (parameters == null) return true;
        
        // extract parsed parameters
        String processorName = (String) parameters[0];
        int index = ((Integer)parameters[1]).intValue();
        
        String[] mailetParameters = null; 
        try {
            mailetParameters = processorManagement.getMailetParameters(processorName, index);
        } catch (RuntimeException e) {
            // fall thru with NULL
        }
        if (mailetParameters == null) {
            writeLoggedFlushedResponse("The index is not referring to an existing mailet");
            return true;
        }
        writeLoggedResponse("Mailet parameters: " + mailetParameters.length);
        for (int i = 0; i < mailetParameters.length; i++) {
            String parameter = (String) mailetParameters[i];
            writeLoggedResponse("\t" + parameter);
         }
        return true;
    }

    private boolean doSHOWMATCHERINFO(String argument) {
        ProcessorManagementService processorManagement = theConfigData.getProcessorManagement();
        Object[] parameters = extractMailetInfoParameters(argument, "MATCHER");
        if (parameters == null) return true;
        
        // extract parsed parameters
        String processorName = (String) parameters[0];
        int index = ((Integer)parameters[1]).intValue();
        
        String[] matcherParameters = null; 
        try {
            matcherParameters = processorManagement.getMatcherParameters(processorName, index);
        } catch (RuntimeException e) {
            // fall thru with NULL
        }
        if (matcherParameters == null) {
            writeLoggedFlushedResponse("The index is not referring to an existing matcher");
            return true;
        }
        writeLoggedResponse("Matcher parameters: " + matcherParameters.length);
        for (int i = 0; i < matcherParameters.length; i++) {
            String parameter = (String) matcherParameters[i];
            writeLoggedResponse("\t" + parameter);
         }
        return true;
    }
    
    private boolean doADDMAPPING(String argument) {
        String[] args = null;
        
        if (argument != null)
            args = argument.split(" ");

        // check if the command was called correct
        if (argument == null || argument.trim().equals("") || args.length < 2 || args.length > 3) {
            writeLoggedFlushedResponse("Usage: ADDMAPPING [table=table] user@domain mapping");
            return true;
        }
        try {
            out.println("Adding mapping successfull: " + mappingAction(args,ADD_MAPPING_ACTION));
            out.flush();
        } catch (VirtualUserTableManagementException e) {
            getLogger().error("Error on adding mapping: " + e);
            out.println("Error on adding mapping: " + e);
            out.flush();
        } catch (IllegalArgumentException e) {
            getLogger().error("Error on adding mapping: " + e);
            out.println("Error on adding mapping: " + e);
            out.flush();
        }
        return true;
    }
    
    private boolean doREMOVEMAPPING(String argument) {
        String[] args = null;
        
        if (argument != null)
            args = argument.split(" ");

        // check if the command was called correct
        if (argument == null || argument.trim().equals("") || args.length < 2 || args.length > 3) {
            writeLoggedFlushedResponse("Usage: REMOVEMAPPING [table=table] user@domain mapping");
            return true;
        }
        try {
            out.println("Removing mapping successfull: " + mappingAction(args,REMOVE_MAPPING_ACTION));
            out.flush();
        } catch (VirtualUserTableManagementException e) {
            getLogger().error("Error on  removing mapping: " + e);
            out.println("Error on removing mapping: " + e);
            out.flush();
        } catch (IllegalArgumentException e) {
            getLogger().error("Error on removing mapping: " + e);
            out.println("Error on removing mapping: " + e);
            out.flush();
        }
        return true;
    }

    private boolean doLISTMAPPING(String argument) {
        String[] args = null;
        String table = null;
        String user = null;
        String domain = null;

        if (argument != null)
            args = argument.split(" ");

        // check if the command was called correct
        if (argument == null || argument.trim().equals("") || args.length < 1 || args.length > 2) {
            writeLoggedFlushedResponse("Usage: LISTMAPPING [table=table] user@domain");
            return true;
        }

        if (args[0].startsWith("table=")) {
            table = args[0].substring("table=".length());
            if (args[1].indexOf("@") > 0) {
                user = getMappingValue(args[1].split("@")[0]);
                domain = getMappingValue(args[1].split("@")[1]);
            } else {
            writeLoggedFlushedResponse("Usage: LISTMAPPING [table=table] user@domain");
                return true;
            }
        } else {
            if (args[0].indexOf("@") > 0) {
                user = getMappingValue(args[0].split("@")[0]);
                domain = getMappingValue(args[0].split("@")[1]);
            } else {
            writeLoggedFlushedResponse("Usage: LISTMAPPING [table=table] user@domain");
                return true;
            }
        }
        
        try {
            Collection mappings = theConfigData.getVirtualUserTableManagement().getUserDomainMappings(table, user, domain);
            if (mappings == null) {
                out.println("No mappings found");
                out.flush();
            } else {
                out.println("Mappings:");
                
                Iterator m = mappings.iterator();
                while(m.hasNext()) {
                    out.println(m.next());
                }
                out.flush();
            }
        } catch (VirtualUserTableManagementException e) {
            getLogger().error("Error on listing mapping: " + e);
            out.println("Error on listing mapping: " + e);
            out.flush();
        } catch (IllegalArgumentException e) {
            getLogger().error("Error on listing mapping: " + e);
            out.println("Error on listing mapping: " + e);
            out.flush();
        }
        return true;
    }
    
    private boolean doLISTALLMAPPINGS(String argument) {
        String[] args = null;
        String table = null;

        if (argument != null)
            args = argument.split(" ");

        // check if the command was called correct
        if (args != null && args.length > 1) {
            writeLoggedFlushedResponse("Usage: LISTALLMAPPINGS [table=table]");
            return true;
        }

        if (args != null && args[0].startsWith("table=")) {
            table = args[0].substring("table=".length());
       
        } 
        
        try {
            Map mappings = theConfigData.getVirtualUserTableManagement().getAllMappings(table);
            if (mappings == null) {
                out.println("No mappings found");
                out.flush();
            } else {
                out.println("Mappings:");
                
                Iterator m = mappings.keySet().iterator();
                while(m.hasNext()) {
                    String key = m.next().toString();
                    out.println(key + "  -> " + mappings.get(key));
                }
                out.flush();
            }
        } catch (VirtualUserTableManagementException e) {
            getLogger().error("Error on listing all mapping: " + e);
            out.println("Error on listing all mapping: " + e);
            out.flush();
        } catch (IllegalArgumentException e) {
            getLogger().error("Error on listing all mapping: " + e);
            out.println("Error on listing all mapping: " + e);
            out.flush();
        }
        return true;
    }
    
    private String getMappingValue(String raw) {
        if (raw.equals("*")) {
            return null;
        } else {
            return raw;
        }
    }
    
    private boolean mappingAction(String[] args, String action) throws IllegalArgumentException, VirtualUserTableManagementException{ 
        String table = null;
        String user = null;
        String domain = null;
        String mapping = null;
    
        if (args[0].startsWith("table=")) {
            table = args[0].substring("table=".length());
            if (args[1].indexOf("@") > 0) {
                user = getMappingValue(args[1].split("@")[0]);
                domain = getMappingValue(args[1].split("@")[1]);
            } else {
                throw new IllegalArgumentException("Invalid usage.");
            }
            mapping = args[2];
        } else {
            if (args[0].indexOf("@") > 0) {
                user = getMappingValue(args[0].split("@")[0]);
                domain = getMappingValue(args[0].split("@")[1]);
            } else {
                throw new IllegalArgumentException("Invalid usage.");
            }
            mapping = args[1];
        }
        
        if (action.equals(ADD_MAPPING_ACTION)) {
            return theConfigData.getVirtualUserTableManagement().addMapping(table, user, domain, mapping);
        } else if (action.equals(REMOVE_MAPPING_ACTION)){
            return theConfigData.getVirtualUserTableManagement().removeMapping(table, user, domain, mapping);
        } else {
            throw new IllegalArgumentException("Invalid action: " + action);
        }   
    }
    
    private boolean doLISTDOMAINS(String argument) {
        Collection domains = theConfigData.getDomainListManagement().getDomains();
        if (domains == null) {
            out.println("No domains found");
            out.flush();
        } else {
            out.println("Domains:");
                
            Iterator d = domains.iterator();
            while(d.hasNext()) {
                out.println(d.next());
            }
            out.flush();
        }   
        return true;
    }

    private boolean doADDDOMAIN(String argument) {

        // check if the command was called correct
        if (argument == null) {
            writeLoggedFlushedResponse("Usage: ADDDOMAIN domain");
            return true;
        }
        
        try {
            if(theConfigData.getDomainListManagement().addDomain(argument)) {
                out.println("Adding domain " + argument + " successful");
                out.flush();
            } else {
                out.println("Adding domain " + argument + " fail");
                out.flush();
            }
        } catch (DomainListManagementException e) {
            getLogger().error("Error on adding domain: " + e);
            out.println("Error on adding domain: " + e);
            out.flush();
        }
        return true;
    }
    
    private boolean doREMOVEDOMAIN(String argument) {
        // check if the command was called correct
        if (argument == null) {
            writeLoggedFlushedResponse("Usage: REMOVEDOMAIN domain");
            return true;
        }
        
        try {
            if(theConfigData.getDomainListManagement().removeDomain(argument)) {
                out.println("Removing domain " + argument + " successful");
                out.flush();
            } else {
                out.println("Removing domain " + argument + " fail");
                out.flush();
            }
        } catch (DomainListManagementException e) {
            getLogger().error("Error on removing domain: " + e);
            out.println("Error on removing domain: " + e);
            out.flush();
        }
        return true;
    }
    
    /**
     * Return an ArrayList which contains all usernames for the given domain
     * 
     * @param domain the domain
     * @return ArrayList which contains the users
     */
    private ArrayList getDomainUserList(String domain) {
        ArrayList userList = new ArrayList();
        
        for (Iterator it = users.list(); it.hasNext();) {
           String user = (String) it.next();
           if (user.endsWith(domain)) {
               userList.add(user);
           }
        }
        
        return userList;
    }
}
