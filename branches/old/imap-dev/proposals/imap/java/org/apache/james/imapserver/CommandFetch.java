/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.imapserver;

import org.apache.james.imapserver.AccessControlException;
import org.apache.james.imapserver.AuthorizationException;
import org.apache.james.core.MimeMessageWrapper;
import org.apache.james.imapserver.commands.ImapCommand;

import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.*;

/**
 * Implements the IMAP FETCH command for a given ImapRequestImpl.
 *
 * References: rfc 2060, rfc 2193, rfc 2221
 * @author <a href="mailto:sascha@kulawik.de">Sascha Kulawik</a>
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.2 on 04 Aug 2002
 */
public class CommandFetch
    extends BaseCommand implements ImapCommand
{

    //mainly to switch on stack traces and catch responses;
    private static final boolean DEEP_DEBUG = true;

    private static final String OK = "OK";
    private static final String NO = "NO";
    private static final String BAD = "BAD";
    private static final String UNTAGGED = "*";
    private static final String SP = " ";

    private StringTokenizer commandLine;
    private boolean useUIDs;
    private ACLMailbox currentMailbox;
    private String commandRaw;
    private PrintWriter out;
    private OutputStream outs;
    private String tag;
    private String user;
    private SingleThreadedConnectionHandler caller;
    private String currentFolder;

    public boolean validForState( ImapSessionState state )
    {
        return ( state == ImapSessionState.SELECTED );
    }

    public boolean process( ImapRequest request, ImapSession session )
    {
        setRequest( request );
    StringTokenizer txt = request.getCommandLine();

/*  System.out.println("CommandFetch.process: #args="+txt.countTokens());
        while (txt.hasMoreTokens()) {
            System.out.println("CommandFetch.process: arg='"+txt.nextToken()+"'");
        }
*/
   //     ((ImapRequestImpl)request).setCommandLine(new java.util.StringTokenizer(request.getCommandRaw()));
        if ( request.arguments() < 2 ) {
            session.badResponse( "#args="+request.arguments()+" '"+request.getCommandLine().nextToken()+"', '"+request.getCommandLine().nextToken()+"' Command should be <tag> <FETCH> <message set> <message data item names>" );
            return true;
        }
        service();
        return true;
    }

    /**
     * Debugging method - will probably disappear
     */
    public void setRequest(ImapRequest request) {
        commandLine = request.getCommandLine();
        useUIDs = request.useUIDs();
        currentMailbox = request.getCurrentMailbox();
        System.out.println("currentMailbox="+((currentMailbox!=null)?currentMailbox.getClass().getName():"null"));
        commandRaw = request.getCommandRaw();
        tag = request.getTag();
        currentFolder = request.getCurrentFolder();

        caller = request.getCaller();
        out = caller.getPrintWriter();
        outs = caller.getOutputStream();
        user = caller.getUser();
    }

    /**
     * Implements IMAP fetch commands given an ImapRequestImpl.
     * This implementation attempts to satisfy the fetch command with the
     * smallest objects deserialized from storage.
     * <p>Warning - maybecome service(ImapRequestImpl request)
     * <p>Not yet complete - no partial (octet-counted or sub-parts) fetches.
     */
    public void service() {
        // decode the message set
        List set;
        List uidsList = null;
        String setArg = commandLine.nextToken();
        if (useUIDs) {
            uidsList = currentMailbox.listUIDs(user);
            set = decodeUIDSet(setArg, uidsList);
        } else {
            set = decodeSet(setArg, currentMailbox.getExists());
        }
        if (DEEP_DEBUG) {
            getLogger().debug("Fetching message set of size: " + set.size());
        }
        String firstFetchArg = commandLine.nextToken();
        int pos =  commandRaw.indexOf(firstFetchArg);
        //int pos = commandRaw.indexOf(setArg) + setArg.length() + 1;
        String fetchAttrsRaw = null;
        if (firstFetchArg.startsWith("(")) { //paranthesised fetch attrs
            fetchAttrsRaw = commandRaw.substring(pos + 1, commandRaw.lastIndexOf(")"));
        } else {
            fetchAttrsRaw = commandRaw.substring(pos);
        }

        if (DEEP_DEBUG) {
            System.out.println("Found fetchAttrsRaw: " + fetchAttrsRaw);
            getLogger().debug("Found fetchAttrsRaw: " + fetchAttrsRaw);
        }
        // decode the fetch attributes
        List fetchAttrs = new ArrayList();
        StringTokenizer fetchTokens = new StringTokenizer(fetchAttrsRaw);
        while (fetchTokens.hasMoreTokens()) {
            String  attr = fetchTokens.nextToken();
            if (attr.indexOf("(") == -1 ) { //not the start of a fields list
                fetchAttrs.add(attr);
            } else {
                StringBuffer attrWithFields = new StringBuffer();
                attrWithFields.append(fetchAttrs.remove(fetchAttrs.size() -1));
                attrWithFields.append(" " + attr);
                boolean endOfFields = false;
                while (! endOfFields && fetchTokens.hasMoreTokens()) {
                    String field = fetchTokens.nextToken();
                    attrWithFields.append(" " + field);
                    if (field.indexOf(")") != -1) {
                        endOfFields = true;
                    }
                }
                fetchAttrs.add(attrWithFields.toString());
            }
        }

        fetchAttrs = convertMacroCommands( fetchAttrs );
        try {
            for (int i = 0; i < set.size(); i++) {
                Integer uidObject = null;
                int uid = 0;
                int msn = 0;
                if (useUIDs) {
                    System.out.println("USE UIDS");
                    uidObject = (Integer)set.get(i);
                    uid = uidObject.intValue();
                    msn = uidsList.indexOf(uidObject) + 1;
                } else {
                    msn = ((Integer)set.get(i)).intValue();
                }
                MessageAttributes  attrs = null;
                String flags = null;
                //EnhancedMimeMessage msg = null;
                MimeMessageWrapper msg = null;
                String response = UNTAGGED + SP + msn + SP + "FETCH (";
                boolean responseAdded = false;
                Iterator it = fetchAttrs.iterator();
                while(it.hasNext()) {
                    String  arg = (String) it.next();
                    // commands that only need flags object
                    if (arg.equalsIgnoreCase("FLAGS")) {
                        if (flags == null) {
                            if (useUIDs) {
                                System.out.println("TRYING UIDFLAGS"+uid);
                                flags = currentMailbox.getFlagsUID(uid, user);
                            } else {
                                System.out.println("TRYING MSNFLAGS"+msn);
                                flags = currentMailbox.getFlags(msn, user);
                            }
                        }
                        if (flags == null) { // bad
                            //out.println(tag + SP + msn + SP + NO + SP + "Error retrieving message flags.");
                            //System.out.println(tag + SP + msn + SP + NO + SP + "Error retrieving message flags.");
                            //return;
                        }
                        if (responseAdded) {
                            response += SP + "FLAGS " + flags ;
                        } else {
                            response +=  "FLAGS " + flags ;
                            responseAdded = true;
                        }
                    }
                    // command that only need MessageAttributes object
                    else if (arg.equalsIgnoreCase("INTERNALDATE")) {
                        System.out.println("Starting INTERNALDATE");
                        if (attrs == null) {
                            if (useUIDs) {
                                attrs = currentMailbox.getMessageAttributesUID(uid, user);
                            } else {
                                attrs = currentMailbox.getMessageAttributes(msn, user);
                            }
                        }
                        if (attrs == null) { // bad
                            out.println(tag + SP + msn + SP + NO + SP + "Error retrieving message attributes.");
                            System.out.println(tag + SP + msn + SP + NO + SP + "Error retrieving message attributes.");
                            getLogger().error("Retrieved null attributes for msn:" + msn);
                            return;
                        }
                        if (responseAdded) {
                            response += SP + "INTERNALDATE \""
                                + attrs.getInternalDateAsString() + "\"" ;
                        } else {
                            response += "INTERNALDATE \""
                                + attrs.getInternalDateAsString() + "\"" ;
                            responseAdded = true;
                        }
                    } else if (arg.equalsIgnoreCase("RFC822.SIZE")) {
                        System.out.println("Starting RFC822.SIZE");
                        if (attrs == null) {
                            if (useUIDs) {
                                attrs = currentMailbox.getMessageAttributesUID(uid, user);
                            } else {
                                attrs = currentMailbox.getMessageAttributes(msn, user);
                            }
                        }
                        if (attrs == null) { // bad
                            out.println(tag + SP + msn + SP + NO + SP + "Error retrieving message attributes.");
                            System.out.println(tag + SP + msn + SP + NO + SP + "Error retrieving message attributes.");
                            getLogger().error("Retrieved null attributes for msn:" + msn);
                            return;
                        }
                        if (responseAdded) {
                            response += SP + "RFC822.SIZE " + attrs.getSize();
                        } else {
                            response +=  "RFC822.SIZE " + attrs.getSize();
                            responseAdded = true;
                        }
                    } else if (arg.equalsIgnoreCase("ENVELOPE")) {
                        System.out.println("Starting ENVELOPE");
                        if (attrs == null) {
                            if (useUIDs) {
                                attrs = currentMailbox.getMessageAttributesUID(uid, user);
                            } else {
                                attrs = currentMailbox.getMessageAttributes(msn, user);
                            }
                        }
                        if (attrs == null) { // bad
                            out.println(tag + SP + msn + SP + NO + SP + "Error retrieving message attributes.");
                            System.out.println(tag + SP + msn + SP + NO + SP + "Error retrieving message attributes.");
                            getLogger().error("Retrieved null attributes for msn:" + msn);
                            return;
                        }
                        if (responseAdded) {
                            response += SP + "ENVELOPE " + attrs.getEnvelope();
                        } else {
                            response +=  "ENVELOPE " + attrs.getEnvelope();
                            responseAdded = true;
                        }
                    } else if (arg.equalsIgnoreCase("BODY")) {
                       System.out.println("CommandFetch BODY start");
                       if (attrs == null) {
                        System.out.println("CommandFetch BODY fetching attrs");
                            if (useUIDs) {
                                attrs = currentMailbox.getMessageAttributesUID(uid, user);
                                System.out.println("currentMailbox.getMessageAttributesUID("+uid+","+user+")");
                            } else {
                                attrs = currentMailbox.getMessageAttributes(msn, user);
                                System.out.println("currentMailbox.getMessageAttributes("+msn+","+user+")");
                            }
                            System.out.println("attrs="+((attrs!=null)?attrs.getClass().getName():"null"));

                        }
                           System.out.println("CommandFetch BODY attrs="+attrs);
                            System.out.println("CommandFetch BODY attrs.getUID: "+attrs.getUID());
                        if (attrs == null) { // bad
                            out.println(tag + SP + msn + SP + NO + "Error retrieving message attributes.");
                            getLogger().error("Retrieved null attributes for msn:" + msn);
                            return;
                        }
                        if (responseAdded) {
                            response += SP + "BODY " + attrs.getBodyStructure();
                        } else {
                            response +=  "BODY " + attrs.getBodyStructure();
                            responseAdded = true;
                        }
                    } else if (arg.equalsIgnoreCase("BODYSTRUCTURE")) {
                        System.out.println("Starting BODYSTRUCTURE");
                        if (attrs == null) {
                            if (useUIDs) {
                                attrs = currentMailbox.getMessageAttributesUID(uid, user);
                            } else {
                                attrs = currentMailbox.getMessageAttributes(msn, user);
                            }
                        }
                        if (attrs == null) { // bad
                            out.println(tag + SP + msn + SP + NO + "Error retrieving message attributes.");
                            getLogger().error("Retrieved null attributes for msn:" + msn);
                            return;
                        }
                        if (responseAdded) {
                            response += SP + "BODYSTRUCTURE "+ attrs.getBodyStructure();
                        } else {
                            response +=  "BODYSTRUCTURE "+ attrs.getBodyStructure();
                            responseAdded = true;
                        }
                    }  else if (arg.equalsIgnoreCase("UID")) {
                        System.out.println("CommandFetch UID start");
                        if (!useUIDs || fetchAttrs.size() == 1){
                            System.out.println("CommandFetch.!useUIDs");
                            if (attrs == null) {
                                System.out.println("CommandFetch UID fetching attrs: "+attrs);
                                attrs = currentMailbox.getMessageAttributes(msn, user);
                            }
                            uid = attrs.getUID();
                            System.out.println("CommandFetch UID attrs: "+attrs);
                            System.out.println("CommandFetch UID attrs.getUID: "+attrs.getUID());
                            if (attrs == null) { // bad
                                out.println(tag + SP + msn + SP + NO + "Error retrieving message attributes.");
                                getLogger().error("Retrieved null attributes for msn:" + msn);
                                return;
                            }
                            System.out.println("CommandFetch UID printing UID: "+uid);

                            if (responseAdded) {
                                response += SP + "UID "+ uid;
                            } else {
                                response +=  "UID "+ uid;
                                responseAdded = true;
                            }
                        } // don't duplicate on UID FETCH requests
                        System.out.println("CommandFetch UID end");
                    }
                    // commands that can be satisifed with just top-level headers of message and flags
                    else if (arg.equalsIgnoreCase("BODY[HEADER]")
                            || arg.equalsIgnoreCase("BODY.PEEK[HEADER]") 
                            || "RFC822.HEADER".equalsIgnoreCase(arg)) {
                        System.out.println("Starting BODY[HEADER]");
                        if (responseAdded) { // unlikely
                            if (useUIDs) {
                                response += " UID " + uid + ")";
                            } else {
                                response += ")";
                            }
                            System.out.println(response);
                            out.println(response);
                            getLogger().debug("Sending: " + response);
                        }
                        InternetHeaders headers = null;
                        if (useUIDs) {
                            headers = currentMailbox.getInternetHeadersUID(uid, user);
                        } else {
                            headers = currentMailbox.getInternetHeaders(msn, user);
                        }
                        if (headers == null) { // bad
                            System.out.println(tag + SP + msn + SP + NO + "Error retrieving message1.");
                            out.println(tag + SP + msn + SP + NO + "Error retrieving message1.");
                            getLogger().error("Retrieved null headers for msn:" + msn);
                            return;
                        }
                        if (flags == null) {
                            if (useUIDs) {
                                flags = currentMailbox.getFlagsUID(uid, user);
                            } else {
                                flags = currentMailbox.getFlags(msn, user);
                            }
                        }
                        response = UNTAGGED + SP + msn + SP + "FETCH (";
                        response += "BODY[HEADER] ";
                        
                        Enumeration enum = headers.getAllHeaderLines();
                        List lines = new ArrayList();
                        int count = 0;
                        while (enum.hasMoreElements()) {
                            String line = (String)enum.nextElement();
                            count += line.length() + 2;
                            lines.add(line);
                        }
                        response += "{" + (count + 2) + "}";
                        out.println(response);
                        System.out.println(response);
                        getLogger().debug("Sending: " + response);
                        Iterator lit = lines.iterator();
                        while (lit.hasNext()) {
                            String line = (String)lit.next();
                            out.println(line);
                            System.out.println(line);
                            getLogger().debug("Sending: " + line);
                        }
                        out.println();
                        System.out.println();
                        getLogger().debug("Sending blank line");
                        if (useUIDs) {
                            out.println(  " UID " + uid + ")");
                            System.out.println(  " UID " + uid + ")");
                            getLogger().debug("Sending: UID " + uid + ")");
                        } else {
                            out.println( ")" );
                            System.out.println( ")" );
                            getLogger().debug("Sending: )");
                        }
                        if (! arg.equalsIgnoreCase("BODY.PEEK[HEADER]")) {
                            try { // around setFlags()
                                if (flags.indexOf("Seen") == -1 ) {
                                    String newflags;
                                    if (useUIDs) {
                                        currentMailbox.setFlagsUID(uid, user, "+flags (\\Seen)");
                                        newflags = currentMailbox.getFlagsUID(uid, user);
                                        out.println(UNTAGGED + SP + msn + SP + "FETCH (FLAGS "
                                                    + newflags + " UID " + uid +")");
                                        System.out.println(UNTAGGED + SP + msn + SP + "FETCH (FLAGS "
                                                    + newflags + " UID " + uid +")");
                                    } else {
                                        currentMailbox.setFlags(msn, user, "+flags (\\Seen)");
                                        newflags = currentMailbox.getFlags(msn, user);
                                        out.println(UNTAGGED + SP + msn + SP + "FETCH (FLAGS "
                                                    + newflags + ")");
                                        System.out.println(UNTAGGED + SP + msn + SP + "FETCH (FLAGS "
                                                    + newflags + ")");
                                    }
                                }
                            } catch (AccessControlException ace) {
                                getLogger().error("Exception storing flags for message: " + ace);
                            } catch (AuthorizationException aze) {
                                getLogger().error("Exception storing flags for message: " + aze);
                            } catch (Exception e) {
                                getLogger().error("Unanticipated exception storing flags for message: " + e);
                            }
                        }
                        response = UNTAGGED + SP + msn + SP + "FETCH (";
                        responseAdded = false;
                    } else if (arg.toUpperCase().startsWith("BODY[HEADER.FIELDS")
                               || arg.toUpperCase().startsWith("BODY.PEEK[HEADER.FIELDS")
                               || arg.toUpperCase().startsWith("(BODY.PEEK[HEADER.FIELDS")){
                        System.out.println("Starting BODY[HEADER.FIELDS]");
                        if (responseAdded) {
                            if (useUIDs) {
                                response += " UID " + uid + ")";
                            } else {
                                response += ")";
                            }
                            out.println(response);
                            System.out.println(response);
                            getLogger().debug("Sending: " + response);
                        }
                        InternetHeaders headers = null;
                        if (useUIDs) {
                            headers = currentMailbox.getInternetHeadersUID(uid, user);
                        } else {
                            headers = currentMailbox.getInternetHeaders(msn, user);
                        }
                        if (headers == null) { // bad
                            out.println(tag + SP + msn + SP + NO + SP + "Error retrieving message2.");
                            System.out.println(tag + SP + msn + SP + NO + SP + "Error retrieving message2.");
                            getLogger().error("Retrieved null headers for msn:" + msn);
                            return;
                        }
                        if (flags == null) {
                            if (useUIDs) {
                                flags = currentMailbox.getFlagsUID(uid, user);
                            } else {
                                flags = currentMailbox.getFlags(msn, user);
                            }
                        }
                        boolean not = (commandRaw.toUpperCase().indexOf("HEADER.FIELDS.NOT") != -1);
                        boolean peek = (commandRaw.toUpperCase().indexOf("PEEK") != -1);
                        response = UNTAGGED + SP + msn + SP + "FETCH (BODY" ;
                        if (peek) {response += ".PEEK";}
                        if (not) {
                            response += "[HEADER.FIELDS.NOT (";
                        } else {
                            response += "[HEADER.FIELDS (";
                        }
                        responseAdded = false;
                        //int h = commandRaw.indexOf("[");
                        int left = arg.indexOf("(");
                        int right = arg.indexOf(")");
                        String fieldsRequested = arg.substring(left + 1, right);
                        response += fieldsRequested + ")] ";
                        ArrayList fields = new ArrayList();
                        if (fieldsRequested.indexOf(" ") == -1) { //only one field
                            fields.add(fieldsRequested);
                        } else {
                            StringTokenizer tok = new StringTokenizer(fieldsRequested);
                            while (tok.hasMoreTokens()) {
                                fields.add((String)tok.nextToken());
                            }
                        }
                        Iterator it2 = fields.iterator();
                        while (it2.hasNext()) {
                            getLogger().debug("request for field: " + (String)it2.next());
                        }
                        String[] names = (String[])fields.toArray(new String[fields.size()]);
                        Enumeration enum = null;
                        if (not) {
                            enum = headers.getNonMatchingHeaderLines(names);
                        } else {
                            enum = headers.getMatchingHeaderLines(names);
                        }
                        List lines = new ArrayList();
                        int count = 0;
                        while (enum.hasMoreElements()) {
                            String line = (String)enum.nextElement();
                            count += line.length() + 2;
                            lines.add(line);
                        }
                        response += "{" + (count + 2) + "}";
                        out.println(response);
                        System.out.println(response);
                        getLogger().debug("Sending: " + response);
                        Iterator lit = lines.iterator();
                        while (lit.hasNext()) {
                            String line = (String)lit.next();
                            out.println(line);
                            System.out.println(line);
                            getLogger().debug("Sending: " + line);
                        }
                        out.println();
                        System.out.println();
                        if (useUIDs) {
                            out.println(  " UID " + uid + ")");
                            System.out.println(  " UID " + uid + ")");
                        } else {
                            out.println(")");
                            System.out.println(")");
                        }
                        if (! peek) {
                            if (flags.indexOf("Seen") == -1 ) {
                                try {
                                    String newflags;
                                    if (useUIDs) {
                                        currentMailbox.setFlagsUID(uid, user, "+flags (\\Seen)");
                                        newflags = currentMailbox.getFlagsUID(uid, user);
                                        out.println(UNTAGGED + SP + msn + SP + "FETCH (FLAGS "
                                                    + newflags + " UID " + uid +")");
                                        System.out.println(UNTAGGED + SP + msn + SP + "FETCH (FLAGS "
                                                    + newflags + " UID " + uid +")");
                                    } else {
                                        currentMailbox.setFlags(msn, user, "+flags (\\Seen)");
                                        newflags = currentMailbox.getFlags(msn, user);
                                        out.println(UNTAGGED + SP + msn + SP + "FETCH (FLAGS "
                                                    + newflags + ")");
                                        System.out.println(UNTAGGED + SP + msn + SP + "FETCH (FLAGS "
                                                    + newflags + ")");
                                    }
                                } catch (AccessControlException ace) {
                                    getLogger().error("Exception storing flags for message: " + ace);
                                } catch (AuthorizationException aze) {
                                    getLogger().error("Exception storing flags for message: " + aze);
                                } catch (Exception e) {
                                    getLogger().error("Unanticipated exception storing flags for message: " + e);
                                }
                            }
                        }
                        response = UNTAGGED + SP + msn + SP + "FETCH (";
                        responseAdded = false;
                    }
                    // Calls to underlying MimeMessage
                    else if (arg.equalsIgnoreCase("RFC822")
                             || arg.equalsIgnoreCase("BODY[]")
                             || arg.equalsIgnoreCase("BODY.PEEK[]")) {
                        System.out.println("Starting BODY[]");
                        if (responseAdded) { // unlikely
                            if (useUIDs) {
                                response += " UID " + uid + ")";
                            } else {
                                response += ")";
                            }
                            out.println(response);
                            System.out.println(response);
                        }
                        if (msg == null) { // probably
                            if (useUIDs) {
                                msg = currentMailbox.retrieveUID(uid, user);
                            } else {
                                msg = currentMailbox.retrieve(msn, user);
                            }
                        }
                        if (flags == null) {
                            if (useUIDs) {
                                flags = currentMailbox.getFlagsUID(uid, user);
                            } else {
                                flags = currentMailbox.getFlags(msn, user);
                            }
                        }
                        if (msg == null) { // bad
                            out.println(tag + SP + msn + SP + BAD + "Error retrieving message3.");
                            System.out.println(tag + SP + msn + SP + BAD + "Error retrieving message3.");
                            getLogger().error("Retrieved null message");
                            return;
                        }
                        try {
                            long size = msg.getMessageSize();
                            if (arg.equalsIgnoreCase("RFC822")) {
                                out.println(UNTAGGED + SP + msn + SP + "FETCH ( RFC822 {" + size + "}");
                                System.out.println(UNTAGGED + SP + msn + SP + "FETCH ( RFC822 {" + size + "}");
                            } else {
                                out.println(UNTAGGED + SP + msn + SP + "FETCH ( BODY[] {" + size + "}");
                                System.out.println(UNTAGGED + SP + msn + SP + "FETCH ( BODY[] {" + size + "}");
                            }
                            msg.writeTo(outs);
                            if (useUIDs) {
                                out.println(" UID " + uid + ")");
                                System.out.println(" UID " + uid + ")");
                            } else {
                                out.println(")");
                                System.out.println(")");
                            }
                            if (! arg.equalsIgnoreCase("BODY.PEEK[]")) {
                                if (flags.indexOf("Seen") == -1 ) {
                                    String newflags;
                                    if (useUIDs) {
                                        currentMailbox.setFlagsUID(uid, user, "+flags (\\Seen)");
                                        newflags = currentMailbox.getFlagsUID(uid, user);
                                        out.println(UNTAGGED + SP + msn + SP + "FETCH (FLAGS "
                                                    + newflags + " UID " + uid +")");
                                        System.out.println(UNTAGGED + SP + msn + SP + "FETCH (FLAGS "
                                                    + newflags + " UID " + uid +")");
                                    } else {
                                        currentMailbox.setFlags(msn, user, "+flags (\\Seen)");
                                        newflags = currentMailbox.getFlags(msn, user);
                                        out.println(UNTAGGED + SP + msn + SP + "FETCH (FLAGS "
                                                    + newflags + ")");
                                        System.out.println(UNTAGGED + SP + msn + SP + "FETCH (FLAGS "
                                                    + newflags + ")");
                                    }
                                }
                            }
                        } catch (MessagingException me) {
                            out.println(UNTAGGED + SP + NO + SP + "Error retrieving message4");
                            System.out.println(UNTAGGED + SP + NO + SP + "Error retrieving message4");
                            getLogger().error("Exception retrieving message: " + me);
                        } catch (IOException ioe) {
                            out.println(UNTAGGED + SP + NO + SP + "Error retrieving message5");
                            System.out.println(UNTAGGED + SP + NO + SP + "Error retrieving message5");
                            getLogger().error("Exception sending message: " + ioe);
                        } catch (Exception e) {
                            out.println(UNTAGGED + SP + NO + SP + "Error retrieving message6");
                            System.out.println(UNTAGGED + SP + NO + SP + "Error retrieving message6");
                            getLogger().error("Unanticipated exception retrieving message: " + e);
                        }
                        response = UNTAGGED + SP + msn + SP + "FETCH (";
                        responseAdded = false;
                    } else if (arg.equalsIgnoreCase("RFC822.TEXT")
                               || arg.equalsIgnoreCase("BODY[TEXT]")
                               || arg.equalsIgnoreCase("BODY.PEEK[TEXT]")) {
                        System.out.println("Starting BODY[TEXT]");
                        if (responseAdded) { // unlikely
                            if (useUIDs) {
                                response += " UID " + uid + ")";
                            } else {
                                response += ")";
                            }
                            out.println(response);
                            System.out.println(response);
                        }
                        if (msg == null) { // probably
                            if (useUIDs) {
                                msg = currentMailbox.retrieveUID(uid, user);
                            } else {
                                msg = currentMailbox.retrieve(msn, user);
                            }
                        }
                        if (flags == null) {
                            if (useUIDs) {
                                flags = currentMailbox.getFlagsUID(uid, user);
                            } else {
                                flags = currentMailbox.getFlags(msn, user);
                            }
                        }
                        if (msg == null) { // bad
                            out.println(tag + SP + msn + SP + NO + "Error retrieving message7.");
                            System.out.println(tag + SP + msn + SP + NO + "Error retrieving message7.");
                            getLogger().error("Retrieved null message");
                            return;
                        }
                        try {
                            int size = msg.getSize();
                            if (arg.equalsIgnoreCase("RFC822.TEXT")) {
                                out.println(UNTAGGED + SP + msn + SP + "FETCH ( RFC822.TEXT {" + size + "}");
                                System.out.println(UNTAGGED + SP + msn + SP + "FETCH ( RFC822.TEXT {" + size + "}");
                            } else {
                                out.println(UNTAGGED + SP + msn + SP + "FETCH ( BODY[TEXT] {" + size + "}");
                                System.out.println(UNTAGGED + SP + msn + SP + "FETCH ( BODY[TEXT] {" + size + "}");
                            }
                            msg.writeContentTo(outs);
                            if (useUIDs) {
                                out.println(  " UID " + uid + ")");
                                System.out.println(  " UID " + uid + ")");
                            } else {
                                out.println(")");
                                System.out.println(")");
                            }
                            if (! arg.equalsIgnoreCase("BODY.PEEK[TEXT]")) {
                                if (flags.indexOf("Seen") == -1 ) {
                                    String newflags;
                                    if (useUIDs) {
                                        currentMailbox.setFlagsUID(uid, user, "+flags (\\Seen)");
                                        newflags = currentMailbox.getFlagsUID(uid, user);
                                        out.println(UNTAGGED + SP + msn + SP + "FETCH (FLAGS "
                                                    + newflags + " UID " + uid +")");
                                        System.out.println(UNTAGGED + SP + msn + SP + "FETCH (FLAGS "
                                                    + newflags + " UID " + uid +")");
                                    } else {
                                        currentMailbox.setFlags(msn, user, "+flags (\\Seen)");
                                        newflags = currentMailbox.getFlags(msn, user);
                                        out.println(UNTAGGED + SP + msn + SP + "FETCH (FLAGS "
                                                    + newflags + ")");
                                        System.out.println(UNTAGGED + SP + msn + SP + "FETCH (FLAGS "
                                                    + newflags + ")");
                                    }
                                }
                            }
                        } catch (MessagingException me) {
                            out.println(UNTAGGED + SP + NO + SP + "Error retrieving message8:"+me);
                            System.out.println(UNTAGGED + SP + NO + SP + "Error retrieving message8:"+me);
                            me.printStackTrace();
                            getLogger().error("Exception retrieving message: " + me);
                        } catch (IOException ioe) {
                            out.println(UNTAGGED + SP + NO + SP + "Error retrieving message9");
                            System.out.println(UNTAGGED + SP + NO + SP + "Error retrieving message9");
                            getLogger().error("Exception sending message: " + ioe);
                        } catch (Exception e) {
                            out.println(UNTAGGED + SP + NO + SP + "Error retrieving message10");
                            System.out.println(UNTAGGED + SP + NO + SP + "Error retrieving message10");
                            getLogger().error("Unanticipated exception retrieving message: " + e);
                        }
                        response = UNTAGGED + SP + msn + SP + "FETCH (";
                        responseAdded = false;
                    }   else { //unrecognised or not yet implemented
                        if (responseAdded) {
                            if (useUIDs) {
                                response += " UID " + uid + ")";
                            } else {
                                response += ")";
                            }
                            out.println(response);
                            System.out.println(response);
                        }
                        out.println(tag + SP + NO + SP
                                    + "FETCH attribute not recognized");
                        System.out.println(tag + SP + NO + SP
                                    + "FETCH attribute not recognized");
                        getLogger().error("Received: " + arg + " as argument to fetch");
                        return;
                    }
                } // end while loop
                if (responseAdded) {
                    if (useUIDs) {
                        response += " UID " + uid + ")";
                    } else {
                        response += ")";
                    }
                    out.println(response);
                    System.out.println(response);
                }
            } // end for loop

            out.println(tag + SP + OK + SP + "FETCH completed");
            System.out.println(tag + SP + OK + SP + "FETCH completed");
            caller.checkSize();
            return;
        } catch (AccessControlException ace) {
            out.println(tag + SP + NO + SP + "No such mailbox");
            System.out.println(tag + SP + NO + SP + "No such mailbox");
            caller.logACE(ace);
            return;
        } catch (AuthorizationException aze) {
            out.println(tag + SP + NO + SP
                        + "You do not have the rights to read from mailbox: " + currentFolder);
            System.out.println(tag + SP + NO + SP
                        + "You do not have the rights to read from mailbox: " + currentFolder);
            caller.logAZE(aze);
            return ;
        } catch (Exception e) {
            out.println(tag + SP + NO + SP
                        + "Unknown server error.");
            System.out.println(tag + SP + NO + SP
                        + "Unknown server error.");
            getLogger().error("Exception expunging mailbox " + currentFolder + " by user " + user + " was : " + e);
            if (DEEP_DEBUG) {e.printStackTrace();}
            return;
        }
    }

    private List convertMacroCommands( List fetchAttributes )
    {
        List convertedAttributes = new ArrayList();

        // convert macro fetch commands to basic commands
        Iterator iter = fetchAttributes.iterator();
        while ( iter.hasNext() ) {
            String arg = (String)iter.next();
            if (arg.equalsIgnoreCase("FAST")) {
                convertedAttributes.add("FLAGS");
                convertedAttributes.add("INTERNALDATE");
                convertedAttributes.add("RFC822.SIZE");
            } else if (arg.equalsIgnoreCase("ALL")) {
                convertedAttributes.add("FLAGS");
                convertedAttributes.add("INTERNALDATE");
                convertedAttributes.add("RFC822.SIZE");
                convertedAttributes.add("ENVELOPE");
            } else if (arg.equalsIgnoreCase("FULL")) {
                convertedAttributes.add("FLAGS");
                convertedAttributes.add("INTERNALDATE");
                convertedAttributes.add("RFC822.SIZE");
                convertedAttributes.add("ENVELOPE");
                convertedAttributes.add("BODY");
            }
            else {
                convertedAttributes.add( arg );
            }
            getLogger().debug("Found convertedAttributes: " + arg);
        }

        return convertedAttributes;
    }
}
