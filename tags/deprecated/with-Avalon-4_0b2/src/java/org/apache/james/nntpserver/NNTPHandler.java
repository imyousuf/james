/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.nntpserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.AbstractLoggable;
import org.apache.avalon.cornerstone.services.connection.ConnectionHandler;
import org.apache.avalon.cornerstone.services.scheduler.PeriodicTimeTrigger;
import org.apache.avalon.cornerstone.services.scheduler.Target;
import org.apache.avalon.cornerstone.services.scheduler.TimeScheduler;
import org.apache.avalon.excalibur.collections.ListUtils;
import org.apache.james.Constants;
import org.apache.james.BaseConnectionHandler;
import org.apache.james.nntpserver.repository.NNTPArticle;
import org.apache.james.nntpserver.repository.NNTPGroup;
import org.apache.james.nntpserver.repository.NNTPLineReaderImpl;
import org.apache.james.nntpserver.repository.NNTPRepository;
import org.apache.james.nntpserver.repository.NNTPUtil;
import org.apache.james.services.UsersRepository;
import org.apache.james.services.UsersStore;

/**
 * The NNTP protocol is defined by RFC 977.
 * This implementation is based on IETF draft 13, posted on 2nd April '2001.
 * URL: http://www.ietf.org/internet-drafts/draft-ietf-nntpext-base-13.txt
 *
 * Common NNTP extensions are in RFC 2980.
 *
 * @author Fedor Karpelevitch
 * @author Harmeet <hbedi@apache.org>
 */
public class NNTPHandler extends BaseConnectionHandler
    implements ConnectionHandler, Composable, Configurable, Target {

    // timeout controllers
    private TimeScheduler scheduler;

    // communciation.
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    // authentication.
    private AuthState authState;
    private boolean authRequired = false;
    private UsersRepository users;

    // data abstractions.
    private NNTPGroup group;
    private NNTPRepository repo;

    public void configure( Configuration configuration ) throws ConfigurationException {
        super.configure(configuration);
        authRequired=configuration.getChild("authRequired").getValueAsBoolean(false);
        authState = new AuthState(authRequired,users);
    }

    public void compose( final ComponentManager componentManager )
        throws ComponentException
    {
        //System.out.println(getClass().getName()+": compose - "+authRequired);
        UsersStore usersStore = (UsersStore)componentManager.
            lookup( "org.apache.james.services.UsersStore" );
        users = usersStore.getRepository("LocalUsers");
        scheduler = (TimeScheduler)componentManager.
            lookup( "org.apache.avalon.cornerstone.services.scheduler.TimeScheduler" );
        repo = (NNTPRepository)componentManager
            .lookup("org.apache.james.nntpserver.repository.NNTPRepository");
    }

    public void handleConnection( Socket connection ) throws IOException {
        try {
            this.socket = connection;
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream()) {
                    public void println() {
                        print("\r\n");
                        flush();
                    }
                };
            getLogger().info( "Connection from " + socket.getInetAddress());
        } catch (Exception e) {
            getLogger().error( "Cannot open connection from: " + e.getMessage(), e );
        }

        try {
            final PeriodicTimeTrigger trigger = new PeriodicTimeTrigger( timeout, -1 );
            scheduler.addTrigger( this.toString(), trigger, this );

            // section 7.1
            writer.println("200 "+helloName+
                           (repo.isReadOnly()?" Posting Not Premitted":" Posting Permitted"));

            while (parseCommand(reader.readLine()))
                scheduler.resetTrigger(this.toString());

            reader.close();
            writer.close();
            socket.close();
            scheduler.removeTrigger(this.toString());
            getLogger().info("Connection closed");
        } catch (Exception e) {
            doQUIT();
            //writer.println("502 Error closing connection.");
            //writer.flush();
            getLogger().error( "Exception during connection:" + e.getMessage(), e );
            try { socket.close();   } catch (IOException ioe) {  }
        }
    }

    public void targetTriggered( final String triggerName ) {
        getLogger().error("Connection timeout on socket");
        try {
            writer.println("Connection timeout. Closing connection");
            socket.close();
        } catch (IOException e) { }
    }

    // checks if a command is allowed. The authentication state is validated.
    private boolean isAllowed(String command) {
        boolean allowed = authState.isAuthenticated();
        // some commads are allowed, even if the user is not authenticated
        allowed = allowed || command.equalsIgnoreCase("AUTHINFO");
        allowed = allowed || command.equalsIgnoreCase("AUTHINFO");
        allowed = allowed || command.equalsIgnoreCase("MODE");
        allowed = allowed || command.equalsIgnoreCase("QUIT");
        if ( allowed == false )
            writer.println("502 User is not authenticated");
        return allowed;
    }
    private boolean parseCommand(String commandRaw) {
        if (commandRaw == null)
            return false;
        getLogger().info("Command received: " + commandRaw);
        //System.out.println("NNTPHandler> "+commandRaw);

        StringTokenizer tokens = new StringTokenizer(commandRaw);
        if (!tokens.hasMoreTokens())
            return false;
        final String command = tokens.nextToken();

        //System.out.println("allowed="+isAllowed(command)+","+authState.isAuthenticated());
        if ( isAllowed(command) == false )
            return true;
        if ( command.equalsIgnoreCase("MODE") && tokens.hasMoreTokens() &&
             tokens.nextToken().equalsIgnoreCase("READER") )
            doMODEREADER();
        else if ( command.equalsIgnoreCase("LIST") && tokens.hasMoreTokens() &&
                  tokens.nextToken().equalsIgnoreCase("EXTENSIONS") )
            doLISTEXTENSIONS();
        else if ( command.equalsIgnoreCase("LIST") && tokens.hasMoreTokens() &&
                  tokens.nextToken().equalsIgnoreCase("OVERVIEW.FMT") )
            doLISTOVERVIEWFMT();
        else if ( command.equalsIgnoreCase("GROUP") )
            doGROUP(tokens.hasMoreTokens()?tokens.nextToken():null);
        else if ( command.equalsIgnoreCase("LAST") )
            doLAST();
        else if ( command.equalsIgnoreCase("ARTICLE") )
            doARTICLE(tokens.hasMoreTokens()?tokens.nextToken():null);
        else if ( command.equalsIgnoreCase("HEAD") )
            doHEAD(tokens.hasMoreTokens()?tokens.nextToken():null);
        else if ( command.equalsIgnoreCase("BODY") )
            doBODY(tokens.hasMoreTokens()?tokens.nextToken():null);
        else if ( command.equalsIgnoreCase("STAT") )
            doSTAT(tokens.hasMoreTokens()?tokens.nextToken():null);
        else if ( command.equalsIgnoreCase("POST") )
            doPOST();
        else if ( command.equalsIgnoreCase("IHAVE") )
            doIHAVE(tokens.hasMoreTokens()?tokens.nextToken():null);
        else if ( command.equalsIgnoreCase("LIST") )
            doLIST(tokens);
        else if ( command.equalsIgnoreCase("QUIT") )
            doQUIT();
        else if ( command.equalsIgnoreCase("DATE") )
            doDATE();
        else if ( command.equalsIgnoreCase("HELP") )
            doHELP();
        else if ( command.equalsIgnoreCase("NEWNEWS") )
            doNEWSGROUPS(tokens);
        else if ( command.equalsIgnoreCase("NEWNEWS") )
            doNEWNEWS(tokens);
        else if ( command.equalsIgnoreCase("LISTGROUP") )
            doLISTGROUP(tokens.hasMoreTokens()?tokens.nextToken():null);
        else if ( command.equalsIgnoreCase("OVER") )
            doOVER(tokens.hasMoreTokens()?tokens.nextToken():null);
        else if ( command.equalsIgnoreCase("XOVER") )
            doXOVER(tokens.hasMoreTokens()?tokens.nextToken():null);
        else if ( command.equalsIgnoreCase("PAT") )
            doPAT();
        else if ( command.equalsIgnoreCase("HDR") )
            doHDR(tokens);
        else if ( command.equalsIgnoreCase("XHDR") )
            doXHDR(tokens);
        else if ( command.equalsIgnoreCase("AUTHINFO") )
            doAUTHINFO(tokens);
        else
            writer.println("501 Syntax error");
        return (command.equalsIgnoreCase("QUIT") == false);
    }

    // implements only the originnal AUTHINFO
    // for simple and generic AUTHINFO, 501 is sent back. This is as
    // per article 3.1.3 of RFC 2980
    private void doAUTHINFO(StringTokenizer tok) {
        String command = tok.nextToken();
        if ( command.equalsIgnoreCase("USER") ) {
            authState.setUser(tok.nextToken());
            writer.println("381 More authentication information required");
        } else if ( command.equalsIgnoreCase("PASS") ) {
            authState.setPassword(tok.nextToken());
            if ( authState.isAuthenticated() )
                writer.println("281 Authentication accepted");
            else
                writer.println("482 Authentication rejected");
        }
    }

    private void doNEWNEWS(StringTokenizer tok) {
        writer.println("230 list of new articles by message-id follows");
        Iterator iter = repo.getArticlesSince(getDateFrom(tok));
        while ( iter.hasNext() )
            writer.println("<"+((NNTPArticle)iter.next()).getUniqueID()+">");
        writer.println(".");
    }
    private void doNEWSGROUPS(StringTokenizer tok) {
        writer.println("230 list of new newsgroups follows");
        Iterator iter = repo.getGroupsSince(getDateFrom(tok));
        while ( iter.hasNext() )
            writer.println(((NNTPGroup)iter.next()).getName());
        writer.println(".");
    }
    // returns the date from @param input.
    // The input tokens are assumed to be in format date time [GMT|UTC] .
    // 'date' is in format [XX]YYMMDD. 'time' is in format 'HHMMSS'
    // NOTE: This routine would do with some format checks.
    private Date getDateFrom(StringTokenizer tok) {
        String date = tok.nextToken();
        String time = tok.nextToken();
        boolean  utc = ( tok.hasMoreTokens() );
        Date d = new Date();
        DateFormat df = ( date.length() == 8 ) ? DF_DATEFROM_LONG : DF_DATEFROM_SHORT;
        try {
            Date dt = df.parse(date+" "+time);
            if ( utc )
                dt = new Date(dt.getTime()+UTC_OFFSET);
            return dt;
        } catch ( ParseException pe ) {
            throw new NNTPException("date extraction failed: "+date+","+time+","+utc);
        }
    }

    private void doHELP() {
        writer.println("100 Help text follows");
        writer.println(".");
    }

    // used to calculate DATE from - see 11.3
    public static final DateFormat DF_DATEFROM_LONG = new SimpleDateFormat("yyyyMMdd HHmmss");
    public static final DateFormat DF_DATEFROM_SHORT = new SimpleDateFormat("yyMMdd HHmmss");

    // Date format for the DATE keyword - see 11.1.1
    public static final DateFormat DF_DATE = new SimpleDateFormat("yyyyMMddHHmmss");
    public static final long UTC_OFFSET = Calendar.getInstance().get(Calendar.ZONE_OFFSET);
    private void doDATE() {
        //Calendar c = Calendar.getInstance();
        //long UTC_OFFSET = c.get(c.ZONE_OFFSET) + c.get(c.DST_OFFSET);
        Date dt = new Date(System.currentTimeMillis()-UTC_OFFSET);
        String dtStr = DF_DATE.format(new Date(dt.getTime() - UTC_OFFSET));
        writer.println("111 "+dtStr);
    }
    private void doQUIT() {
        writer.println("205 closing connection");
    }
    private void doLIST(StringTokenizer tok) {
        // see section 9.4.1
        String wildmat = "*";
        LISTGroup output = LISTGroup.Factory.ACTIVE(writer);
        if ( tok.hasMoreTokens() ) {
            String param = tok.nextToken();
            // list of variations not supported - 9.4.2.1, 9.4.3.1, 9.4.4.1
            String[] notSupported = { "ACTIVE.TIMES", "DISTRIBUTIONS", "DISTRIB.PATS" };
            for ( int i = 0 ; i < notSupported.length ; i++ ) {
                if ( param.equalsIgnoreCase("ACTIVE.TIMES") ) {
                    writer.println("503 program error, function not performed");
                    return;
                }
            }
            if ( param.equalsIgnoreCase("NEWSGROUPS") )
                output = LISTGroup.Factory.NEWSGROUPS(writer);
            else
                assert(param,param.equalsIgnoreCase("ACTIVE"));
            if ( tok.hasMoreTokens() )
                wildmat = tok.nextToken();
        }
        Iterator iter = repo.getMatchedGroups(wildmat);
        writer.println("215 list of newsgroups follows");
        while ( iter.hasNext() )
            output.show((NNTPGroup)iter.next());
        writer.println(".");
    }
    private void assert(String id,boolean b) {
        if ( b == false )
            throw new RuntimeException(id);
    }
    private void doIHAVE(String id) {
        // see section 9.3.2.1
        assert(id,id.startsWith("<") && id.endsWith(">"));
        NNTPArticle article = repo.getArticleFromID(id);
        if ( article != null )
            writer.println("435 article not wanted - do not send it");
        else {
            writer.println("335 send article to be transferred. End with <CR-LF>.<CR-LF>");
            createArticle();
            writer.println("235 article received ok");
        }
    }
    private void doPOST() {
        // see section 9.3.1.1
        writer.println("340 send article to be posted. End with <CR-LF>.<CR-LF>");
        createArticle();
        writer.println("240 article received ok");
    }
    private void createArticle() {
        repo.createArticle(new NNTPLineReaderImpl(reader));
    }
    private void doSTAT(String param) {
        doARTICLE(param,ArticleWriter.Factory.STAT(writer));
    }
    private void doBODY(String param) {
        doARTICLE(param,ArticleWriter.Factory.BODY(writer));
    }
    private void doHEAD(String param) {
        doARTICLE(param,ArticleWriter.Factory.HEAD(writer));
    }
    private void doARTICLE(String param) {
        doARTICLE(param,ArticleWriter.Factory.ARTICLE(writer));
    }
    private void doARTICLE(String param,ArticleWriter articleWriter) {
        // section 9.2.1
        NNTPArticle article = null;
        if ( (param != null) && param.startsWith("<") && param.endsWith(">") ) {
            article = repo.getArticleFromID(param.substring(1,param.length()-2));
            if ( article == null )
                writer.println("430 no such article");
            else
                writer.println("220 0 "+param+" article retrieved and follows");
        }
        else {
            if ( group == null )
                writer.println("412 no newsgroup selected");
            else {
                if ( param == null ) {
                    if ( group.getCurrentArticleNumber() < 0 )
                        writer.println("420 no current article selected");
                    else
                        article = group.getCurrentArticle();
                }
                else
                    article = group.getArticle(Integer.parseInt(param));
                if ( article == null )
                    writer.println("423 no such article number in this group");
                else
                    writer.println("220 "+article.getArticleNumber()+" "+
                                   article.getUniqueID()+" article retrieved and follows");
            }
        }
        if ( article != null )
            articleWriter.write(article);
    }
    private void doNEXT() {
        // section 9.1.1.3.1
        if ( group == null )
            writer.println("412 no newsgroup selected");
        else if ( group.getCurrentArticleNumber() < 0 )
            writer.println("420 no current article has been selected");
        else if ( group.getCurrentArticleNumber() >= group.getLastArticleNumber() )
            writer.println("421 no next article in this group");
        else {
            NNTPArticle article = group.getCurrentArticle();
            group.setCurrentArticleNumber(group.getCurrentArticleNumber()+1);
            writer.println("223 "+article.getArticleNumber()+" "+article.getUniqueID());
        }
    }
    private void doLAST() {
        // section 9.1.1.2.1
        if ( group == null )
            writer.println("412 no newsgroup selected");
        else if ( group.getCurrentArticleNumber() < 0 )
            writer.println("420 no current article has been selected");
        else if ( group.getCurrentArticleNumber() <= group.getFirstArticleNumber() )
            writer.println("422 no previous article in this group");
        else {
            NNTPArticle article = group.getCurrentArticle();
            group.setCurrentArticleNumber(group.getCurrentArticleNumber()-1);
            writer.println("223 "+article.getArticleNumber()+" "+article.getUniqueID());
        }
    }
    private void doGROUP(String groupName) {
        group = repo.getGroup(groupName);
        // section 9.1.1.1
        if ( group == null )
            writer.println("411 no such newsgroup");
        else
            writer.println("211 "+group.getNumberOfArticles()+" "+
                           group.getFirstArticleNumber()+" "+
                           group.getLastArticleNumber()+" "+
                           group.getName()+" group selected");

    }
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

    private void doMODEREADER() {
        // 7.2
        writer.println(repo.isReadOnly()
                       ? "201 Posting Not Permitted" : "200 Posting Permitted");
    }

    private void doLISTGROUP(String groupName) {
        // 9.5.1.1.1
        NNTPGroup group = null;
        if (groupName==null) {
            if ( group == null )
                writer.println("412 not currently in newsgroup");
        }
        else {
            group = repo.getGroup(groupName);
            if ( group == null )
                writer.println("411 no such newsgroup");
        }
        if ( group != null ) {
            writer.println("211 list of article numbers follow");

            for (Iterator iter = group.getArticles();iter.hasNext();) {
                NNTPArticle article = (NNTPArticle)iter.next();
                writer.println(article.getArticleNumber());
            }
            writer.println(".");
            this.group = group;
            group.setCurrentArticleNumber(group.getFirstArticleNumber());
        }
    }

    private void doLISTOVERVIEWFMT() {
        // 9.5.3.1.1
        // 503 means information is not available as per 9.5.2.1
        writer.println("503 program error, function not performed");
    }
    private void doPAT() {
        // 9.5.3.1.1 in draft-12
        writer.println("500 Command not recognized");
    }
    private void doXHDR(StringTokenizer tok) {
        doHDR(tok);
    }
    private void doHDR(StringTokenizer tok) {
        // 9.5.3
        writer.println("500 Command not recognized");
        String hdr = tok.nextToken();
        String range = tok.hasMoreTokens() ? tok.nextToken() : null;
        NNTPArticle[] article = getRange(range);
        if ( article == null )
            writer.println("412 no newsgroup selected");
        else if ( article.length == 0 )
            writer.println("430 no such article");
        else {
            writer.println("221 Header follows");
            for ( int i = 0 ; i < article.length ; i++ ) {
                String val = article[i].getHeader(hdr);
                if ( val == null )
                    val = "";
                writer.println(article[i].getArticleNumber()+" "+val);
            }
            writer.println(".");
        }
    }
    // returns the list of articles that match the range.
    // @return null indicates insufficient information to
    // fetch the list of articles
    private NNTPArticle[] getRange(String range) {
        // check for msg id
        if ( range != null && range.startsWith("<") ) {
            NNTPArticle article = repo.getArticleFromID(range);
            return ( article == null )
                ? new NNTPArticle[0] : new NNTPArticle[] { article };
        }

        if ( group == null )
            return null;
        if ( range == null )
            range = ""+group.getCurrentArticleNumber();

        int start = -1;
        int end = -1;
        int idx = range.indexOf('-');
        if ( idx == -1 ) {
            start = end = Integer.parseInt(range);
        } else {
            start = Integer.parseInt(range.substring(0,idx));
            if ( idx+1 == range.length() )
                end = group.getLastArticleNumber();
            else
                end = Integer.parseInt(range.substring(idx+1));
        }
        List list = new ArrayList();
        for ( int i = start ; i <= end ; i++ ) {
            NNTPArticle article = group.getArticle(i);
            if ( article != null )
                list.add(article);
        }
        return (NNTPArticle[])list.toArray(new NNTPArticle[0]);
    }

    private void doXOVER(String range) {
        doOVER(range);
    }
    private void doOVER(String range) {
        // 9.5.2.2.1
        if ( group == null ) {
            writer.println("412 No newsgroup selected");
            return;
        }
        NNTPArticle[] article = getRange(range);
        ArticleWriter articleWriter = ArticleWriter.Factory.OVER(writer);
        if ( article.length == 0 )
            writer.println("420 No article(s) selected");
        else {
            writer.println("224 Overview information follows");
            for ( int i = 0 ; i < article.length ; i++ )
                articleWriter.write(article[i]);
            writer.println(".");
        }
    }
}
