/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.imapserver;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.IOException;

import java.util.*;
import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;

import org.apache.james.AccessControlException;
import org.apache.james.AuthorizationException;
import org.apache.james.core.EnhancedMimeMessage;

import org.apache.log.Logger;


/**
 * Implements the IMAP FETCH command for a given ImapRequest.
 *
 * References: rfc 2060, rfc 2193, rfc 2221
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.1 on 17 Jan 2001
 */

public class CommandFetch {
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
    private Logger logger;
    private String commandRaw;
    private PrintWriter out;
    private OutputStream outs;
    private String tag;
    private String user;
    private SingleThreadedConnectionHandler caller;
    private String currentFolder;

    /**
     * Debugging method - will probably disappear
     */
    public void setRequest(ImapRequest request) {
	commandLine = request.getCommandLine();
	useUIDs = request.useUIDs();
	currentMailbox = request.getCurrentMailbox();
	commandRaw = request.getCommandRaw();
	tag = request.getTag();
	currentFolder = request.getCurrentFolder();

	caller = request.getCaller();
	logger = caller.getLogger();
	out = caller.getPrintWriter();
	outs = caller.getOutputStream();
	user = caller.getUser();
    }

    /**
     * Implements IMAP fetch commands given an ImapRequest. 
     * This implementation attempts to satisfy the fetch command with the
     * smallest objects deserialized from storage.
     * <p>Warning - maybecome service(ImapRequest request)
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
	    set = decodeSet(setArg);
	}
	if (DEEP_DEBUG) {
            logger.debug("Fetching message set of size: " + set.size());
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
            logger.debug("Found fetchAttrsRaw: " + fetchAttrsRaw);
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
		while (! endOfFields) {
		    String field = fetchTokens.nextToken();
		    attrWithFields.append(" " + field);
		    if (field.indexOf(")") != -1) {
			endOfFields = true;
		    } 
		}
		fetchAttrs.add(attrWithFields.toString());
	    }
	}

    
	// convert macro fetch commands to basic commands
	for(int k = 0; k < fetchAttrs.size(); k++) {
	    String arg = (String)fetchAttrs.get(k);
	    if (arg.equalsIgnoreCase("FAST")) {
		fetchAttrs.add("FLAGS");
		fetchAttrs.add("INTERNALDATE");
		fetchAttrs.add("RFC822.SIZE");
	    } else if (arg.equalsIgnoreCase("ALL")) {
		fetchAttrs.add("FLAGS");
		fetchAttrs.add("INTERNALDATE");
		fetchAttrs.add("RFC822.SIZE");
		fetchAttrs.add("ENVELOPE");
	    } else if (arg.equalsIgnoreCase("FULL")) {
		fetchAttrs.add("FLAGS");
		fetchAttrs.add("INTERNALDATE");
		fetchAttrs.add("RFC822.SIZE");
		fetchAttrs.add("ENVELOPE");
		fetchAttrs.add("BODY");
	    }
	    logger.debug("Found fetchAttrs: " + arg);
	}

	try {
	    for (int i = 0; i < set.size(); i++) {
		Integer uidObject = null;
		int uid = 0;
		int msn = 0;
		if (useUIDs) {
		    uidObject = (Integer)set.get(i);
		    uid = uidObject.intValue();
		    msn = uidsList.indexOf(uidObject) + 1;
		} else {
		    msn = ((Integer)set.get(i)).intValue();
		}
		MessageAttributes  attrs = null;
		String flags = null;
		EnhancedMimeMessage msg = null;
		String response = UNTAGGED + SP + msn + SP + "FETCH (";
		boolean responseAdded = false;
		Iterator it = fetchAttrs.iterator();
		while(it.hasNext()) {
		    String  arg = (String) it.next();
		    // commands that only need flags object
		    if (arg.equalsIgnoreCase("FLAGS")) {
			if (flags == null) {
			    if (useUIDs) {
				flags = currentMailbox.getFlagsUID(uid, user);
			    } else {
				flags = currentMailbox.getFlags(msn, user);
			    }
			}
			if (flags == null) { // bad
			    out.println(tag + SP + msn + SP + NO + "Error retrieving message flags.");
			    logger.error("Retrieved null flags for msn:" + msn);
			    return;
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
			if (attrs == null) {
			    if (useUIDs) {
				attrs = currentMailbox.getMessageAttributesUID(uid, user);
			    } else {
				attrs = currentMailbox.getMessageAttributes(msn, user);
			    }
			}
			if (attrs == null) { // bad
			    out.println(tag + SP + msn + SP + NO + "Error retrieving message attributes.");
			    logger.error("Retrieved null attributes for msn:" + msn);
			    return;
			} 
			if (responseAdded) {
			    response += SP + "INTERNALDATE \""
				+ attrs.getInternalDateAsString() + "\")" ;
			} else {
			    response += "INTERNALDATE \""
				+ attrs.getInternalDateAsString() + "\")" ;
			    responseAdded = true;
			}
		    } else if (arg.equalsIgnoreCase("RFC822.SIZE")) {
			if (attrs == null) {
			    if (useUIDs) {
				attrs = currentMailbox.getMessageAttributesUID(uid, user);
			    } else {
				attrs = currentMailbox.getMessageAttributes(msn, user);
			    }
			}
			if (attrs == null) { // bad
			    out.println(tag + SP + msn + SP + NO + "Error retrieving message attributes.");
			    logger.error("Retrieved null attributes for msn:" + msn);
			    return;
			} 
			if (responseAdded) {
			    response += SP + "RFC822.SIZE " + attrs.getSize();
			} else {
			    response +=  "RFC822.SIZE " + attrs.getSize();
			    responseAdded = true;
			}
		    } else   if (arg.equalsIgnoreCase("ENVELOPE")) {
			if (attrs == null) {
			    if (useUIDs) {
				attrs = currentMailbox.getMessageAttributesUID(uid, user);
			    } else {
				attrs = currentMailbox.getMessageAttributes(msn, user);
			    }
			}
			if (attrs == null) { // bad
			    out.println(tag + SP + msn + SP + NO + "Error retrieving message attributes.");
			    logger.error("Retrieved null attributes for msn:" + msn);
			    return;
			} 
			if (responseAdded) {
			    response += SP + "ENVELOPE " + attrs.getEnvelope();
			} else {
			    response +=  "ENVELOPE " + attrs.getEnvelope();
			    responseAdded = true;
			}
		    } else if (arg.equalsIgnoreCase("BODY")) {
			if (attrs == null) {
			    if (useUIDs) {
				attrs = currentMailbox.getMessageAttributesUID(uid, user);
			    } else {
				attrs = currentMailbox.getMessageAttributes(msn, user);
			    }
			}
			if (attrs == null) { // bad
			    out.println(tag + SP + msn + SP + NO + "Error retrieving message attributes.");
			    logger.error("Retrieved null attributes for msn:" + msn);
			    return;
			} 
			if (responseAdded) {
			    response += SP + "BODY " + attrs.getBodyStructure();
			} else {
			    response +=  "BODY " + attrs.getBodyStructure();
			    responseAdded = true;
			}
		    } else if (arg.equalsIgnoreCase("BODYSTRUCTURE")) {
			if (attrs == null) {
			    if (useUIDs) {
				attrs = currentMailbox.getMessageAttributesUID(uid, user);
			    } else {
				attrs = currentMailbox.getMessageAttributes(msn, user);
			    }
			}
			if (attrs == null) { // bad
			    out.println(tag + SP + msn + SP + NO + "Error retrieving message attributes.");
			    logger.error("Retrieved null attributes for msn:" + msn);
			    return;
			} 
			if (responseAdded) {
			    response += SP + "BODYSTRUCTURE "+ attrs.getBodyStructure();
			} else {
			    response +=  "BODYSTRUCTURE "+ attrs.getBodyStructure();
			    responseAdded = true;
			}
		    }  else if (arg.equalsIgnoreCase("UID")) {
			if (!useUIDs){
			    if (attrs == null) {
				attrs = currentMailbox.getMessageAttributes(msn, user);
				uid = attrs.getUID();
			    }
			    if (attrs == null) { // bad
				out.println(tag + SP + msn + SP + NO + "Error retrieving message attributes.");
				logger.error("Retrieved null attributes for msn:" + msn);
				return;
			    } 
			
			    if (responseAdded) {
				response += SP + "UID "+ uid;
			    } else {
				response +=  "UID "+ uid;
				responseAdded = true;
			    }
			} // don't duplicate on UID FETCH requests
		    }
		    // commands that can be satisifed with just top-level headers of message and flags 
		    else if (arg.equalsIgnoreCase("BODY[HEADER]")
				|| arg.equalsIgnoreCase("BODY.PEEK[HEADER]")) {
			if (responseAdded) { // unlikely
			    if (useUIDs) {
				response += " UID " + uid + ")";
			    } else {
				response += ")";
			    }
			    out.println(response);
			    logger.debug("Sending: " + response);
			}
			InternetHeaders headers = null;
			if (useUIDs) {
			    headers = currentMailbox.getInternetHeadersUID(uid, user);
			} else {
			    headers = currentMailbox.getInternetHeaders(msn, user);
			}
			if (headers == null) { // bad
			    out.println(tag + SP + msn + SP + NO + "Error retrieving message.");
			    logger.error("Retrieved null headers for msn:" + msn);
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
			//if (arg.equalsIgnoreCase("BODY[Header]")) {
			    response += "BODY[HEADER] ";
			    //} else {
			    //    response += "BODY.PEEK[HEADER] ";
			    //}
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
			logger.debug("Sending: " + response);
			Iterator lit = lines.iterator();
			while (lit.hasNext()) {
			    String line = (String)lit.next();
			    out.println(line);
			    logger.debug("Sending: " + line);
			}
			out.println();
                        logger.debug("Sending blank line");
			if (useUIDs) {
			    out.println(  " UID " + uid + ")");
			    logger.debug("Sending: UID " + uid + ")");
			} else {
			    out.println( ")" );
                            logger.debug("Sending: )");
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
				    } else {
					currentMailbox.setFlags(msn, user, "+flags (\\Seen)");
					newflags = currentMailbox.getFlags(msn, user);
					out.println(UNTAGGED + SP + msn + SP + "FETCH (FLAGS "
						    + newflags + ")");
				    }
				}
			    } catch (AccessControlException ace) {
				logger.error("Exception storing flags for message: " + ace);
			    } catch (AuthorizationException aze) {
				logger.error("Exception storing flags for message: " + aze);
			    } catch (Exception e) {
				logger.error("Unanticipated exception storing flags for message: " + e);
			    }
			}	
			response = UNTAGGED + SP + msn + SP + "FETCH (";
			responseAdded = false;
		    } else if (arg.toUpperCase().startsWith("BODY[HEADER.FIELDS")
			       || arg.toUpperCase().startsWith("BODY.PEEK[HEADER.FIELDS")) {
			if (responseAdded) { 
			    if (useUIDs) {
				response += " UID " + uid + ")";
			    } else {
				response += ")";
			    }
			    out.println(response);
                            logger.debug("Sending: " + response);
			}
			InternetHeaders headers = null;
			if (useUIDs) {
			    headers = currentMailbox.getInternetHeadersUID(uid, user);
			} else {
			    headers = currentMailbox.getInternetHeaders(msn, user);
			}
			if (headers == null) { // bad
			    out.println(tag + SP + msn + SP + NO + "Error retrieving message.");
			    logger.error("Retrieved null headers for msn:" + msn);
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
			//if (peek) {response += ".PEEK";}
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
			    logger.debug("request for field: " + (String)it2.next());
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
                        logger.debug("Sending: " + response);
			Iterator lit = lines.iterator();
			while (lit.hasNext()) {
			    String line = (String)lit.next();
			    out.println(line);
                            logger.debug("Sending: " + line);
			}
			out.println();
			if (useUIDs) {
			    out.println(  " UID " + uid + ")");
			} else {
			    out.println(")");
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
				    } else {
					currentMailbox.setFlags(msn, user, "+flags (\\Seen)");
					newflags = currentMailbox.getFlags(msn, user);
					out.println(UNTAGGED + SP + msn + SP + "FETCH (FLAGS "
						    + newflags + ")");
				    }
				} catch (AccessControlException ace) {
				    logger.error("Exception storing flags for message: " + ace);
				} catch (AuthorizationException aze) {
				    logger.error("Exception storing flags for message: " + aze);
				} catch (Exception e) {
				    logger.error("Unanticipated exception storing flags for message: " + e);
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
			if (responseAdded) { // unlikely
			    if (useUIDs) {
				response += " UID " + uid + ")";
			    } else {
				response += ")";
			    }
			    out.println(response);
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
			    out.println(tag + SP + msn + SP + BAD + "Error retrieving message.");
			    logger.error("Retrieved null message");
			    return;
			} 
			try {
			    int size = msg.getMessageSize();
			    if (arg.equalsIgnoreCase("RFC822")) {
				out.println(UNTAGGED + SP + msn + SP + "FETCH ( RFC822 {" + size + "}");
			    } else {
				out.println(UNTAGGED + SP + msn + SP + "FETCH ( BODY[] {" + size + "}");
				}
			    msg.writeTo(outs);
			    if (useUIDs) {
				out.println(" UID " + uid + ")");
			    } else {
				out.println(")");
			    }
			    if (! arg.equalsIgnoreCase("BODY.PEEK[]")) {
				if (flags.indexOf("Seen") == -1 ) {
				    String newflags;
				    if (useUIDs) {
					currentMailbox.setFlagsUID(uid, user, "+flags (\\Seen)");
					newflags = currentMailbox.getFlagsUID(uid, user);
					out.println(UNTAGGED + SP + msn + SP + "FETCH (FLAGS "
						    + newflags + " UID " + uid +")");
				    } else {
					currentMailbox.setFlags(msn, user, "+flags (\\Seen)");
					newflags = currentMailbox.getFlags(msn, user);
					out.println(UNTAGGED + SP + msn + SP + "FETCH (FLAGS "
						    + newflags + ")");
				    }
				}
			    }
			} catch (MessagingException me) {
			    out.println(UNTAGGED + SP + NO + SP + "Error retrieving message");
			    logger.error("Exception retrieving message: " + me);
			} catch (IOException ioe) {
			    out.println(UNTAGGED + SP + NO + SP + "Error retrieving message");
			    logger.error("Exception sending message: " + ioe);
			} catch (Exception e) {
			    out.println(UNTAGGED + SP + NO + SP + "Error retrieving message");
			    logger.error("Unanticipated exception retrieving message: " + e);
			}
			response = UNTAGGED + SP + msn + SP + "FETCH (";
			responseAdded = false;
		    } else if (arg.equalsIgnoreCase("RFC822.TEXT")
			       || arg.equalsIgnoreCase("BODY[TEXT]")
			       || arg.equalsIgnoreCase("BODY.PEEK[TEXT]")) {
			if (responseAdded) { // unlikely
			    if (useUIDs) {
				response += " UID " + uid + ")";
			    } else {
				response += ")";
			    }
			    out.println(response);
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
			    out.println(tag + SP + msn + SP + NO + "Error retrieving message.");
			    logger.error("Retrieved null message");
			    return;
			} 
			try {
			    int size = msg.getSize();
			    if (arg.equalsIgnoreCase("RFC822.TEXT")) {
				out.println(UNTAGGED + SP + msn + SP + "FETCH ( RFC822.TEXT {" + size + "}");
			    } else {
				out.println(UNTAGGED + SP + msn + SP + "FETCH ( BODY[TEXT] {" + size + "}");
			    }
			    msg.writeContentTo(outs);
			    if (useUIDs) {
				out.println(  " UID " + uid + ")");
			    } else {
				out.println(")");
			    }
			    if (! arg.equalsIgnoreCase("BODY.PEEK[TEXT]")) {
				if (flags.indexOf("Seen") == -1 ) {
				    String newflags;
				    if (useUIDs) {
					currentMailbox.setFlagsUID(uid, user, "+flags (\\Seen)");
					newflags = currentMailbox.getFlagsUID(uid, user);
					out.println(UNTAGGED + SP + msn + SP + "FETCH (FLAGS "
						    + newflags + " UID " + uid +")");
				    } else {
					currentMailbox.setFlags(msn, user, "+flags (\\Seen)");
					newflags = currentMailbox.getFlags(msn, user);
					out.println(UNTAGGED + SP + msn + SP + "FETCH (FLAGS "
						    + newflags + ")");
				    }
				}
			    }
			} catch (MessagingException me) {
			    out.println(UNTAGGED + SP + NO + SP + "Error retrieving message");
			    logger.error("Exception retrieving message: " + me);
			} catch (IOException ioe) {
			    out.println(UNTAGGED + SP + NO + SP + "Error retrieving message");
			    logger.error("Exception sending message: " + ioe);
			} catch (Exception e) {
			    out.println(UNTAGGED + SP + NO + SP + "Error retrieving message");
			    logger.error("Unanticipated exception retrieving message: " + e);
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
			}
			out.println(tag + SP + NO + SP
				    + "FETCH attribute not recognized");
			logger.error("Received: " + arg + " as argument to fetch");
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
		}
	    } // end for loop
	
	    out.println(tag + SP + OK + SP + "FETCH completed");
	    caller.checkSize();
	    return;
	} catch (AccessControlException ace) {
	    out.println(tag + SP + NO + SP + "No such mailbox");
	    caller.logACE(ace);
	    return;
	} catch (AuthorizationException aze) {
	    out.println(tag + SP + NO + SP
			+ "You do not have the rights to read from mailbox: " + currentFolder);
	    caller.logAZE(aze);
	    return ;
	} catch (Exception e) {
	    out.println(tag + SP + NO + SP
			+ "Unknown server error.");
	    logger.error("Exception expunging mailbox " + currentFolder + " by user " + user + " was : " + e);
	    if (DEEP_DEBUG) {e.printStackTrace();}
	    return;
	}

    }

    /**
     * Turns a protocol-compliant string representing a message sequence number set into a
     * List of integers. Use of the wildcard * relies on contiguous proerty of msns.
     */
    private List decodeSet(String rawSet) throws IllegalArgumentException {
	if (rawSet == null) {
	    logger.debug("Null argument in decodeSet");
	    throw new IllegalArgumentException("Null argument");
	} else if (rawSet.equals("")) {
	    logger.debug("Empty argument in decodeSet");
	    throw new IllegalArgumentException("Empty string argument"); 
	}
	logger.debug(" decodeSet called for: " + rawSet);
	List response = new ArrayList();
	int checkComma = rawSet.indexOf(",");
	if (checkComma == -1) {
	    int checkColon = rawSet.indexOf(":");
	    if (checkColon == -1) {
		Integer seqNum = new Integer(rawSet.trim());
		if (seqNum.intValue() < 1) {
		    throw new IllegalArgumentException("Not a positive integer"); 
		} else {
		    response.add(seqNum);
		}
	    } else {
		Integer firstNum = new Integer(rawSet.substring(0, checkColon));
		int first = firstNum.intValue();
		Integer lastNum;
		int last;
		if (rawSet.indexOf("*") != -1) {
		    last = currentMailbox.getExists();
		    lastNum = new Integer(last);
		} else {
		    lastNum = new Integer(rawSet.substring(checkColon + 1));
		    last = lastNum.intValue();
		}
		if (first < 1 || last < 1) {
		    throw new IllegalArgumentException("Not a positive integer"); 
		} else if (first < last) {
		    response.add(firstNum);
		    for (int i = (first + 1); i < last; i++) {
			response.add(new Integer(i));
		    }
		    response.add(lastNum);
		} else if (first == last) {
		    response.add(firstNum);
		} else {
		    throw new IllegalArgumentException("Not an increasing range"); 
		}
	    }
  
	} else {
	    try {
		String firstRawSet = rawSet.substring(0, checkComma);
		String secondRawSet = rawSet.substring(checkComma + 1);
		response.addAll(decodeSet(firstRawSet));
		response.addAll(decodeSet(secondRawSet));
	    } catch (IllegalArgumentException e) {
		logger.debug("Wonky arguments in: " + rawSet + " " + e);
		throw e;
	    }
	}
	return response;
    }

    /**
     * Turns a protocol-compliant string representing a uid set into a
     * List of integers. Where the string requests ranges or uses the * (star)
     * wildcard, the results are uids that exist in the mailbox. This
     * minimizes attempts to refer to non-existent messages.
     */
    private List decodeUIDSet(String rawSet, List uidsList)
            throws IllegalArgumentException {
	if (rawSet == null) {
	    logger.debug("Null argument in decodeSet");
	    throw new IllegalArgumentException("Null argument");
	} else if (rawSet.equals("")) {
	    logger.debug("Empty argument in decodeSet");
	    throw new IllegalArgumentException("Empty string argument"); 
	}
	logger.debug(" decodeUIDSet called for: " + rawSet);
	Iterator it = uidsList.iterator();
	while (it.hasNext()) {
	    logger.info ("uids present : " + (Integer)it.next() );
	}
	List response = new ArrayList();
	int checkComma = rawSet.indexOf(",");
	if (checkComma == -1) {
	    int checkColon = rawSet.indexOf(":");
	    if (checkColon == -1) {
		Integer seqNum = new Integer(rawSet.trim());
		if (seqNum.intValue() < 1) {
		    throw new IllegalArgumentException("Not a positive integer"); 
		} else {
		    response.add(seqNum);
		}
	    } else {
		Integer firstNum = new Integer(rawSet.substring(0, checkColon));
		int first = firstNum.intValue();

		Integer lastNum;
		if (rawSet.indexOf("*") == -1) {
		    lastNum = new Integer(rawSet.substring(checkColon + 1));
		} else {
		    lastNum = (Integer)uidsList.get(uidsList.size()-1);
		}
		int last;
		last = lastNum.intValue();
		if (first < 1 || last < 1) {
		    throw new IllegalArgumentException("Not a positive integer"); 
		} else if (first < last) {
		    response.add(firstNum);
		    Collection uids;
		    if(uidsList.size() > 50) {
			uids = new HashSet(uidsList);
		    } else {
			uids = uidsList;
		    }
		    for (int i = (first + 1); i < last; i++) {
			Integer test = new Integer(i);
			if (uids.contains(test)) {
			    response.add(test);
			}
		    }
		    response.add(lastNum);
		    
		} else if (first == last) {
		    response.add(firstNum);
		} else {
		    throw new IllegalArgumentException("Not an increasing range"); 
		}

	    }
	    
	} else {
	    try {
		String firstRawSet = rawSet.substring(0, checkComma);
		String secondRawSet = rawSet.substring(checkComma + 1);
		response.addAll(decodeSet(firstRawSet));
		response.addAll(decodeSet(secondRawSet));
	    } catch (IllegalArgumentException e) {
		logger.debug("Wonky arguments in: " + rawSet + " " + e);
		throw e;
	    }
	}
	return response;
    }


}
