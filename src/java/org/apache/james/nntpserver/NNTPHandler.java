/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.nntpserver;

import org.apache.avalon.cornerstone.services.connection.ConnectionHandler;
import org.apache.avalon.cornerstone.services.scheduler.PeriodicTimeTrigger;
import org.apache.avalon.cornerstone.services.scheduler.Target;
import org.apache.avalon.cornerstone.services.scheduler.TimeScheduler;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.Logger;
import org.apache.james.BaseConnectionHandler;
import org.apache.james.nntpserver.repository.NNTPArticle;
import org.apache.james.nntpserver.repository.NNTPGroup;
import org.apache.james.nntpserver.repository.NNTPLineReaderImpl;
import org.apache.james.nntpserver.repository.NNTPRepository;
import org.apache.james.services.UsersRepository;
import org.apache.james.services.UsersStore;
import org.apache.james.util.RFC977DateFormat;
import org.apache.james.util.RFC2980DateFormat;
import org.apache.james.util.SimplifiedDateFormat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;

/**
 * The NNTP protocol is defined by RFC 977.
 * This implementation is based on IETF draft 15, posted on 15th July '2002.
 * URL: http://www.ietf.org/internet-drafts/draft-ietf-nntpext-base-15.txt
 *
 * Common NNTP extensions are in RFC 2980.
 *
 * @author Fedor Karpelevitch
 * @author Harmeet <hbedi@apache.org>
 * @author Peter M. Goldstein <farsight@alum.mit.edu>
 */
public class NNTPHandler extends BaseConnectionHandler
    implements ConnectionHandler, Composable, Configurable, Target {

    /**
     * used to calculate DATE from - see 11.3
     */
    private static final SimplifiedDateFormat DF_RFC977 = new RFC977DateFormat();

    /**
     * Date format for the DATE keyword - see 11.1.1
     */
    private static final SimplifiedDateFormat DF_RFC2980 = new RFC2980DateFormat();

    /**
     * The UTC offset for this time zone.
     */
    public static final long UTC_OFFSET = Calendar.getInstance().get(Calendar.ZONE_OFFSET);

    /**
     * Timeout controller
     */
    private TimeScheduler scheduler;

    /**
     * The TCP/IP socket over which the POP3 interaction
     * is occurring
     */
    private Socket socket;

    /**
     * The reader associated with incoming characters.
     */
    private BufferedReader reader;

    /**
     * The writer to which outgoing messages are written.
     */
    private PrintWriter writer;

    /**
     * The current newsgroup.
     */
    private NNTPGroup group;

    /**
     * The repository that stores the news articles for this NNTP server.
     */
    private NNTPRepository repo;

    /**
     * The repository that stores the local users.  Used for authentication.
     */
    private UsersRepository userRepository = null;

    /**
     * Whether authentication is required to access this NNTP server
     */
    private boolean authRequired = false;

    /**
     * The user id associated with the NNTP dialogue
     */
    private String user;

    /**
     * The password associated with the NNTP dialogue
     */
    private String password;


    /**
     * @see org.apache.avalon.framework.component.Composable#compose(ComponentManager)
     */
    public void compose( final ComponentManager componentManager )
        throws ComponentException
    {
        getLogger().debug("NNTPHandler compose...begin");
        UsersStore usersStore = (UsersStore)componentManager.lookup(UsersStore.ROLE);
        userRepository = usersStore.getRepository("LocalUsers");

        scheduler = (TimeScheduler)componentManager.
            lookup( "org.apache.avalon.cornerstone.services.scheduler.TimeScheduler" );

        repo = (NNTPRepository)componentManager
            .lookup("org.apache.james.nntpserver.repository.NNTPRepository");
        getLogger().debug("NNTPHandler compose...end");
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration configuration)
            throws ConfigurationException {
        super.configure(configuration);
        getLogger().debug("NNTPHandler configure...begin");
        authRequired =
            configuration.getChild("authRequired").getValueAsBoolean(false);
        getLogger().debug("NNTPHandler configure...end");
    }

    /**
     * @see org.apache.avalon.cornerstone.services.connection.ConnectionHandler#handleConnection(Socket)
     */
    public void handleConnection( Socket connection ) throws IOException {
        try {
            this.socket = connection;
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream()) {
                // TODO: This implementation is sensitive to details of the PrintWriter
                //       implementation.  Specifically, that println(String) calls println().
                //       This should be corrected so that this sensitivity is removed.
                    public void println() {
                        // lines must end with CRLF, irrespective of the OS
                        print("\r\n");
                        flush();
                    }
                    public void println(String s) {
                        super.println(s);
                        if (getLogger().isDebugEnabled()) {
                            getLogger().debug("Sent: " + s);
                        }
                    }
                };
        } catch (Exception e) {
            getLogger().error( "Cannot open connection from: " + e.getMessage(), e );
        }

        String triggerId = this.toString();

        try {
            final PeriodicTimeTrigger trigger = new PeriodicTimeTrigger( timeout, -1 );
            scheduler.addTrigger( triggerId, trigger, this );

            // section 7.1
            if ( repo.isReadOnly() ) {
                StringBuffer respBuffer =
                    new StringBuffer(128)
                        .append("201 ")
                        .append(helloName)
                        .append(" NNTP Service Ready, posting prohibited");
                writer.println(respBuffer.toString());
            } else {
                StringBuffer respBuffer =
                    new StringBuffer(128)
                            .append("200 ")
                            .append(helloName)
                            .append(" NNTP Service Ready, posting permitted");
                writer.println(respBuffer.toString());
            }

            while (parseCommand(reader.readLine())) {
                scheduler.resetTrigger(triggerId);
            }

            getLogger().info("Connection closed");
        } catch (Exception e) {
            doQUIT(null);
            getLogger().error( "Exception during connection:" + e.getMessage(), e );
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ioe) {
                getLogger().warn("NNTPHandler: Unexpected exception occurred while closing reader: " + ioe);
            }

            if (writer != null) {
                writer.close();
            }

            scheduler.removeTrigger(triggerId);

            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException ioe) {
                getLogger().warn("NNTPHandler: Unexpected exception occurred while closing socket: " + ioe);
            }
        }
    }

    /**
     * Callback method called when the the PeriodicTimeTrigger in 
     * handleConnection is triggered.  In this case the trigger is
     * being used as a timeout, so the method simply closes the connection.
     *
     * @param triggerName the name of the trigger
     */
    public void targetTriggered( final String triggerName ) {
        getLogger().error("Connection timeout on socket");
        try {
            writer.println("Connection timeout. Closing connection");
            socket.close();
        } catch (IOException e) { }
    }

    /**
     * This method parses NNTP commands read off the wire in handleConnection.
     * Actual processing of the command (possibly including additional back and
     * forth communication with the client) is delegated to one of a number of
     * command specific handler methods.  The primary purpose of this method is
     * to parse the raw command string to determine exactly which handler should
     * be called.  It returns true if expecting additional commands, false otherwise.
     *
     * @param commandRaw the raw command string passed in over the socket
     *
     * @return whether additional commands are expected.
     */
    private boolean parseCommand(String commandRaw) {
        if (commandRaw == null) {
            return false;
        }
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Command received: " + commandRaw);
        }

        String command = commandRaw.trim();
        String argument = null;
        int spaceIndex = command.indexOf(" ");
        if (spaceIndex >= 0) {
            argument = command.substring(spaceIndex + 1);
            command = command.substring(0, spaceIndex);
        }
        command = command.toUpperCase(Locale.US);

        if (!isAuthorized(command) ) {
            writer.println("502 User is not authenticated");
            getLogger().debug("Command not allowed.");
            return true;
        }
        if ((command.equals("MODE")) && (argument != null) &&
            argument.toUpperCase(Locale.US).equals("READER")) {
            doMODEREADER(argument);
        } else if ( command.equals("LIST")) {
            doLIST(argument);
        } else if ( command.equals("GROUP") ) {
            doGROUP(argument);
        } else if ( command.equals("NEXT") ) {
            doNEXT(argument);
        } else if ( command.equals("LAST") ) {
            doLAST(argument);
        } else if ( command.equals("ARTICLE") ) {
            doARTICLE(argument);
        } else if ( command.equals("HEAD") ) {
            doHEAD(argument);
        } else if ( command.equals("BODY") ) {
            doBODY(argument);
        } else if ( command.equals("STAT") ) {
            doSTAT(argument);
        } else if ( command.equals("POST") ) {
            doPOST(argument);
        } else if ( command.equals("IHAVE") ) {
            doIHAVE(argument);
        } else if ( command.equals("QUIT") ) {
            doQUIT(argument);
        } else if ( command.equals("DATE") ) {
            doDATE(argument);
        } else if ( command.equals("HELP") ) {
            doHELP(argument);
        } else if ( command.equals("NEWGROUPS") ) {
            doNEWGROUPS(argument);
        } else if ( command.equals("NEWNEWS") ) {
            doNEWNEWS(argument);
        } else if ( command.equals("LISTGROUP") ) {
            doLISTGROUP(argument);
        } else if ( command.equals("OVER") ) {
            doOVER(argument);
        } else if ( command.equals("XOVER") ) {
            doXOVER(argument);
        } else if ( command.equals("PAT") ) {
            doPAT(argument);
        } else if ( command.equals("HDR") ) {
            doHDR(argument);
        } else if ( command.equals("XHDR") ) {
            doXHDR(argument);
        } else if ( command.equals("AUTHINFO") ) {
            doAUTHINFO(argument);
        } else {
            doUnknownCommand(command, argument);
        }
        return (command.equals("QUIT") == false);
    }

    /**
     * Handles an unrecognized command, logging that.
     *
     * @param command the command received from the client
     * @param argument the argument passed in with the command
     */
    private void doUnknownCommand(String command, String argument) {
        if (getLogger().isDebugEnabled()) {
            StringBuffer logBuffer =
                new StringBuffer(128)
                    .append("Received unknown command ")
                    .append(command)
                    .append(" with argument ")
                    .append(argument);
            getLogger().debug(logBuffer.toString());
        }
        writer.println("501 Syntax error");
    }

    /**
     * Implements only the originnal AUTHINFO.
     * for simple and generic AUTHINFO, 501 is sent back. This is as
     * per article 3.1.3 of RFC 2980
     *
     * @param argument the argument passed in with the AUTHINFO command
     */
    private void doAUTHINFO(String argument) {
        String command = null;
        String value = null;
        if (argument != null) {
            int spaceIndex = argument.indexOf(" ");
            if (spaceIndex >= 0) {
                command = argument.substring(0, spaceIndex);
                value = argument.substring(spaceIndex + 1);
            }
        }
        if (command == null) {
            writer.println("501 Syntax error");
            return;
        }
        if ( command.equals("USER") ) {
            user = value;
            writer.println("381 More authentication information required");
        } else if ( command.equals("PASS") ) {
            password = value;
            if ( isAuthenticated() ) {
                writer.println("281 Authentication accepted");
            } else {
                writer.println("482 Authentication rejected");
                // Clear bad authentication
                user = null;
                password = null;
            }
        }
    }

    /**
     * Lists the articles posted since the date passed in as
     * an argument.
     *
     * @param argument the argument passed in with the NEWNEWS command.
     *                 Should be a date.
     */
    private void doNEWNEWS(String argument) {
        // see section 11.4
        writer.println("230 list of new articles by message-id follows");
        Iterator iter = repo.getArticlesSince(getDateFrom(argument));
        while ( iter.hasNext() ) {
            StringBuffer iterBuffer =
                new StringBuffer(64)
                    .append("<")
                    .append(((NNTPArticle)iter.next()).getUniqueID())
                    .append(">");
            writer.println(iterBuffer.toString());
        }
        writer.println(".");
    }

    /**
     * Lists the groups added since the date passed in as
     * an argument.
     *
     * @param argument the argument passed in with the NEWNEWS command.
     *                 Should be a date.
     */
    private void doNEWGROUPS(String argument) {
        // see section 11.3
        // there seeem to be few differences.
        // draft-ietf-nntpext-base-15.txt mentions 231 in section 11.3.1, 
        // but examples have response code 230. rfc977 has 231 response code.
        // both draft-ietf-nntpext-base-15.txt and rfc977 have only group names 
        // in response lines, but INN sends 
        // '<group name> <last article> <first article> <posting allowed>'
        // NOTE: following INN over either document.
        writer.println("231 list of new newsgroups follows");
        Iterator iter = repo.getGroupsSince(getDateFrom(argument));
        while ( iter.hasNext() ) {
            NNTPGroup group = (NNTPGroup)iter.next();
            StringBuffer iterBuffer =
                new StringBuffer(128)
                    .append(group.getName())
                    .append(" ")
                    .append(group.getLastArticleNumber())
                    .append(" ")
                    .append(group.getFirstArticleNumber())
                    .append(" ")
                    .append((group.isPostAllowed()?"y":"n"));
            writer.println(iterBuffer.toString());
        }
        writer.println(".");
    }

    /**
     * Lists the help text for the service.
     *
     * @param argument the argument passed in with the HELP command.
     */
    private void doHELP(String argument) {
        writer.println("100 Help text follows");
        writer.println(".");
    }

    /**
     * Returns the current date according to the news server.
     *
     * @param argument the argument passed in with the DATE command
     */
    private void doDATE(String argument) {
        //Calendar c = Calendar.getInstance();
        //long UTC_OFFSET = c.get(c.ZONE_OFFSET) + c.get(c.DST_OFFSET);
        Date dt = new Date(System.currentTimeMillis()-UTC_OFFSET);
        String dtStr = DF_RFC2980.format(new Date(dt.getTime() - UTC_OFFSET));
        writer.println("111 "+dtStr);
    }

    /**
     * Quits the transaction.
     *
     * @param argument the argument passed in with the QUIT command
     */
    private void doQUIT(String argument) {
        writer.println("205 closing connection");
    }

    /**
     * Handles the LIST command and its assorted extensions.
     *
     * @param argument the argument passed in with the LIST command.
     */
    private void doLIST(String argument) {

        // see section 9.4.1
        String wildmat = "*";
        boolean isListNewsgroups = false;

        String extension = argument;
        if (argument != null) {
            int spaceIndex = argument.indexOf(" ");
            if (spaceIndex >= 0) {
                wildmat = argument.substring(spaceIndex + 1);
                extension = argument.substring(0, spaceIndex);
            }
            extension = extension.toUpperCase(Locale.US);
        }

        if (extension != null) {
            if (extension.equals("ACTIVE")) {
                isListNewsgroups = false;
            } else if (extension.equals("NEWSGROUPS") ) {
                isListNewsgroups = true;
            } else if (extension.equals("EXTENSIONS") ) {
                doLISTEXTENSIONS();
                return;
            } else if (extension.equals("OVERVIEW.FMT") ) {
                doLISTOVERVIEWFMT();
                return;
            } else if (extension.equals("ACTIVE.TIMES") ) {
                // not supported - 9.4.2.1, 9.4.3.1, 9.4.4.1
                writer.println("503 program error, function not performed");
                return;
            } else if (extension.equals("DISTRIBUTIONS") ) {
                // not supported - 9.4.2.1, 9.4.3.1, 9.4.4.1
                writer.println("503 program error, function not performed");
                return;
            } else if (extension.equals("DISTRIB.PATS") ) {
                // not supported - 9.4.2.1, 9.4.3.1, 9.4.4.1
                writer.println("503 program error, function not performed");
                return;
            } else {
                writer.println("501 Syntax error");
                return;
            }
        }

        Iterator iter = repo.getMatchedGroups(wildmat);
        writer.println("215 list of newsgroups follows");
        while ( iter.hasNext() ) {
            NNTPGroup theGroup = (NNTPGroup)iter.next();
            if (isListNewsgroups) {
                writer.println(theGroup.getListNewsgroupsFormat());
            } else {
                writer.println(theGroup.getListFormat());
            }
        }
        writer.println(".");
    }

    /**
     * Informs the server that the client has an article with the specified
     * message-ID.
     *
     * @param id the message id
     */
    private void doIHAVE(String id) {
        // see section 9.3.2.1
        if (!(id.startsWith("<") && id.endsWith(">"))) {
            writer.println("501 command syntax error");
            return;
        }
        NNTPArticle article = repo.getArticleFromID(id);
        if ( article != null ) {
            writer.println("435 article not wanted - do not send it");
        } else {
            writer.println("335 send article to be transferred. End with <CR-LF>.<CR-LF>");
            createArticle();
            writer.println("235 article received ok");
        }
    }

    /**
     * Posts an article to the news server.
     *
     * @param argument the argument passed in with the AUTHINFO command
     */
    private void doPOST(String argument) {
        // see section 9.3.1.1
        writer.println("340 send article to be posted. End with <CR-LF>.<CR-LF>");
        createArticle();
        writer.println("240 article received ok");
    }

    /**
     * Sets the current article pointer.
     *
     * @param the argument passed in to the HEAD command,
     *        which should be an article number or message id.
     *        If no parameter is provided, the current selected
     *        article is used.
     */
    private void doSTAT(String param) {
        doARTICLE(param,ArticleWriter.Factory.STAT(writer));
    }

    /**
     * Displays the body of a particular article.
     *
     * @param the argument passed in to the HEAD command,
     *        which should be an article number or message id.
     *        If no parameter is provided, the current selected
     *        article is used.
     */
    private void doBODY(String param) {
        doARTICLE(param,ArticleWriter.Factory.BODY(writer));
    }

    /**
     * Displays the header of a particular article.
     *
     * @param the argument passed in to the HEAD command,
     *        which should be an article number or message id.
     *        If no parameter is provided, the current selected
     *        article is used.
     */
    private void doHEAD(String param) {
        doARTICLE(param,ArticleWriter.Factory.HEAD(writer));
    }

    /**
     * Displays the header and body of a particular article.
     *
     * @param the argument passed in to the HEAD command,
     *        which should be an article number or message id.
     *        If no parameter is provided, the current selected
     *        article is used.
     */
    private void doARTICLE(String param) {
        doARTICLE(param,ArticleWriter.Factory.ARTICLE(writer));
    }

    /**
     * The implementation of the method body for the ARTICLE, STAT,
     * HEAD, and BODY commands.
     *
     * @param the argument passed in to the command,
     *        which should be an article number or message id.
     *        If no parameter is provided, the current selected
     *        article is used.
     * @param articleWriter the writer that determines the output
     *                      written to the client.
     */
    private void doARTICLE(String param,ArticleWriter articleWriter) {
        // section 9.2.1
        NNTPArticle article = null;
        if ( (param != null) && param.startsWith("<") && param.endsWith(">") ) {
            article = repo.getArticleFromID(param);
            if ( article == null ) {
                writer.println("430 no such article");
            } else {
                StringBuffer respBuffer =
                    new StringBuffer(64)
                            .append("220 0 ")
                            .append(param)
                            .append(" article retrieved and follows");
                writer.println(respBuffer.toString());
            }
        } else {
            if ( group == null ) {
                writer.println("412 no newsgroup selected");
            } else {
                if ( param == null ) {
                    if ( group.getCurrentArticleNumber() < 0 ) {
                        writer.println("420 no current article selected");
                    } else {
                        article = group.getCurrentArticle();
                    }
                }
                else {
                    article = group.getArticle(Integer.parseInt(param));
                }
                if ( article == null ) {
                    writer.println("423 no such article number in this group");
                } else {
                    StringBuffer respBuffer =
                        new StringBuffer(128)
                                .append("220 ")
                                .append(article.getArticleNumber())
                                .append(" ")
                                .append(article.getUniqueID())
                                .append(" article retrieved and follows");
                    writer.println(respBuffer.toString());
                }
            }
        }
        if ( article != null ) {
            articleWriter.write(article);
        }   
    }

    /**
     * Advances the current article pointer to the next article in the group.
     *
     * @param argument the argument passed in with the NEXT command
     */
    private void doNEXT(String argument) {
        // section 9.1.1.3.1
        if ( group == null ) {
            writer.println("412 no newsgroup selected");
        } else if ( group.getCurrentArticleNumber() < 0 ) {
            writer.println("420 no current article has been selected");
        } else if ( group.getCurrentArticleNumber() >= group.getLastArticleNumber() ) {
            writer.println("421 no next article in this group");
        } else {
            group.setCurrentArticleNumber(group.getCurrentArticleNumber()+1);
            NNTPArticle article = group.getCurrentArticle();
            StringBuffer respBuffer =
                new StringBuffer(64)
                        .append("223 ")
                        .append(article.getArticleNumber())
                        .append(" ")
                        .append(article.getUniqueID());
            writer.println(respBuffer.toString());
        }
    }

    /**
     * Advances the currently selected article pointer to the last article
     * in the selected group.
     *
     * @param argument the argument passed in with the LAST command
     */
    private void doLAST(String argument) {
        // section 9.1.1.2.1
        if ( group == null ) {
            writer.println("412 no newsgroup selected");
        } else if ( group.getCurrentArticleNumber() < 0 ) {
            writer.println("420 no current article has been selected");
        } else if ( group.getCurrentArticleNumber() <= group.getFirstArticleNumber() ) {
            writer.println("422 no previous article in this group");
        } else {
            group.setCurrentArticleNumber(group.getCurrentArticleNumber()-1);
            NNTPArticle article = group.getCurrentArticle();
            StringBuffer respBuffer =
                new StringBuffer(64)
                        .append("223 ")
                        .append(article.getArticleNumber())
                        .append(" ")
                        .append(article.getUniqueID());
            writer.println(respBuffer.toString());
        }
    }

    /**
     * Selects a group to be the current newsgroup.
     *
     * @param group the name of the group being selected.
     */
    private void doGROUP(String groupName) {
        NNTPGroup newGroup = repo.getGroup(groupName);
        // section 9.1.1.1
        if ( newGroup == null ) {
            writer.println("411 no such newsgroup");
        } else {
            group = newGroup;
            // if the number of articles in group == 0
            // then the server may return this information in 3 ways, 
            // The clients must honor all those 3 ways.
            // our response is: 
            // highWaterMark == lowWaterMark and number of articles == 0
            int articleCount = group.getNumberOfArticles();
            int lowWaterMark = group.getFirstArticleNumber();
            int highWaterMark = group.getLastArticleNumber();
            StringBuffer respBuffer =
                new StringBuffer(128)
                        .append("211 ")
                        .append(articleCount)
                        .append(" ")
                        .append(lowWaterMark)
                        .append(" ")
                        .append(highWaterMark)
                        .append(" ")
                        .append(group.getName())
                        .append(" group selected");
            writer.println(respBuffer.toString());
        }
    }

    /**
     * Lists the extensions supported by this news server.
     */
    private void doLISTEXTENSIONS() {
        // 8.1.1
        writer.println("202 Extensions supported:");
        writer.println("LISTGROUP");
        writer.println("AUTHINFO");
        writer.println("OVER");
        writer.println("XOVER");
        writer.println("HDR");
        writer.println("XHDR");
        writer.println(".");
    }

    /**
     * Informs the server that the client is a newsreader.
     *
     * @param argument the argument passed in with the MODE READER command
     */
    private void doMODEREADER(String argument) {
        // 7.2
        writer.println(repo.isReadOnly()
                       ? "201 Posting Not Permitted" : "200 Posting Permitted");
    }

    /**
     * Gets a listing of article numbers in specified group name
     * or in the already selected group if the groupName is null.
     *
     * @param groupName the name of the group to list
     */
    private void doLISTGROUP(String groupName) {
        // 9.5.1.1.1
        if (groupName==null) {
            if ( group == null) {
                writer.println("412 not currently in newsgroup");
                return;
            }
        }
        else {
            group = repo.getGroup(groupName);
            if ( group == null ) {
                writer.println("411 no such newsgroup");
                return;
            }
        }
        if ( group != null ) {
            writer.println("211 list of article numbers follow");

            Iterator iter = group.getArticles();
            while (iter.hasNext()) {
                NNTPArticle article = (NNTPArticle)iter.next();
                writer.println(article.getArticleNumber());
            }
            writer.println(".");
            this.group = group;
            group.setCurrentArticleNumber(group.getFirstArticleNumber());
        }
    }

    /**
     * Handles the LIST OVERVIEW.FMT command.  Not supported.
     */
    private void doLISTOVERVIEWFMT() {
        // 9.5.3.1.1
        // 503 means information is not available as per 9.5.2.1
        writer.println("503 program error, function not performed");
    }

    /**
     * Handles the PAT command.  Not supported.
     *
     * @param argument the argument passed in with the PAT command
     */
    private void doPAT(String argument) {
        // 9.5.3.1.1 in draft-12
        writer.println("500 Command not recognized");
    }

    /**
     * Get the values of the headers for the selected newsgroup, 
     * with an optional range modifier.
     *
     * @param argument the argument passed in with the XHDR command.
     */
    private void doXHDR(String argument) {
        doHDR(argument);
    }

    /**
     * Get the values of the headers for the selected newsgroup, 
     * with an optional range modifier.
     *
     * @param argument the argument passed in with the HDR command.
     */
    private void doHDR(String argument) {
        // 9.5.3
        String hdr = argument;
        String range = null;
        int spaceIndex = argument.indexOf(" ");
        if (spaceIndex >= 0 ) {
            range = hdr.substring(spaceIndex + 1);
            hdr = hdr.substring(0, spaceIndex);
        }
        NNTPArticle[] article = getRange(range);
        if ( article == null ) {
            writer.println("412 no newsgroup selected");
        } else if ( article.length == 0 ) {
            writer.println("430 no such article");
        } else {
            writer.println("221 Header follows");
            for ( int i = 0 ; i < article.length ; i++ ) {
                String val = article[i].getHeader(hdr);
                if ( val == null ) {
                    val = "";
                }
                StringBuffer hdrBuffer =
                    new StringBuffer(128)
                            .append(article[i].getArticleNumber())
                            .append(" ")
                            .append(val);
                writer.println(hdrBuffer.toString());
            }
            writer.println(".");
        }
    }

    /**
     * Returns information from the overview database regarding the
     * current article, or a range of articles.
     *
     * @param range the optional article range.
     */
    private void doXOVER(String range) {
        doOVER(range);
    }

    /**
     * Returns information from the overview database regarding the
     * current article, or a range of articles.
     *
     * @param range the optional article range.
     */
    private void doOVER(String range) {
        // 9.5.2.2.1
        if ( group == null ) {
            writer.println("412 No newsgroup selected");
            return;
        }
        NNTPArticle[] article = getRange(range);
        ArticleWriter articleWriter = ArticleWriter.Factory.OVER(writer);
        if ( article.length == 0 ) {
            writer.println("420 No article(s) selected");
        } else {
            writer.println("224 Overview information follows");
            for ( int i = 0 ; i < article.length ; i++ ) {
                articleWriter.write(article[i]);
            }
            writer.println(".");
        }
    }

    /**
     * Handles the transaction for getting the article data.
     */
    private void createArticle() {
        repo.createArticle(new NNTPLineReaderImpl(reader));
    }

    /**
     * Returns the date from @param input.
     * The input tokens are assumed to be in format date time [GMT|UTC] .
     * 'date' is in format [XX]YYMMDD. 'time' is in format 'HHMMSS'
     * NOTE: This routine could do with some format checks.
     *
     * @param argument the date string
     */
    private Date getDateFrom(String argument) {
        StringTokenizer tok = new StringTokenizer(argument, " ");
        String date = tok.nextToken();
        String time = tok.nextToken();
        boolean utc = ( tok.hasMoreTokens() );
        Date d = new Date();
        try {
            StringBuffer dateStringBuffer =
                new StringBuffer(64)
                    .append(date)
                    .append(" ")
                    .append(time);
            Date dt = DF_RFC977.parse(dateStringBuffer.toString());
            if ( utc ) {
                dt = new Date(dt.getTime()+UTC_OFFSET);
            }
            return dt;
        } catch ( ParseException pe ) {
            StringBuffer exceptionBuffer =
                new StringBuffer(128)
                    .append("date extraction failed: ")
                    .append(date)
                    .append(",")
                    .append(time)
                    .append(",")
                    .append(utc);
            throw new NNTPException(exceptionBuffer.toString());
        }
    }

    /**
     * Returns the list of articles that match the range.
     * @return null indicates insufficient information to
     * fetch the list of articles
     */
    private NNTPArticle[] getRange(String range) {
        // check for msg id
        if ( range != null && range.startsWith("<") ) {
            NNTPArticle article = repo.getArticleFromID(range);
            return ( article == null )
                ? new NNTPArticle[0] : new NNTPArticle[] { article };
        }

        if ( group == null ) {
            return null;
        }
        if ( range == null ) {
            range = "" + group.getCurrentArticleNumber();
        }

        int start = -1;
        int end = -1;
        int idx = range.indexOf('-');
        if ( idx == -1 ) {
            start = end = Integer.parseInt(range);
        } else {
            start = Integer.parseInt(range.substring(0,idx));
            if ( idx+1 == range.length() ) {
                end = group.getLastArticleNumber();
            } else {
                end = Integer.parseInt(range.substring(idx+1));
            }
        }
        List list = new ArrayList();
        for ( int i = start ; i <= end ; i++ ) {
            NNTPArticle article = group.getArticle(i);
            if ( article != null ) {
                list.add(article);
            }
        }
        return (NNTPArticle[])list.toArray(new NNTPArticle[0]);
    }

    /**
     * Return whether the user associated with the connection (possibly no
     * user) is authorized to execute the command.
     *
     * @param the command being tested
     * @return whether the command is authorized
     */
    private boolean isAuthorized(String command) {
        boolean allowed = isAuthenticated();
        if (allowed) {
            return true;
        }
        // some commands are authorized, even if the user is not authenticated
        allowed = allowed || command.equals("AUTHINFO");
        allowed = allowed || command.equals("MODE");
        allowed = allowed || command.equals("QUIT");
        return allowed;
    }

    /**
     * Return whether the connection has been authenticated.
     *
     * @return whether the connection has been authenticated.
     */
    private boolean isAuthenticated() {
        if ( authRequired ) {
            if  ((user != null) && (password != null) && (userRepository != null)) {
                return userRepository.test(user,password);
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

}
