/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.imapserver;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.*;

import org.apache.avalon.*;
import org.apache.avalon.services.*;
//import org.apache.avalon.utils.*;
import org.apache.james.*;
import org.apache.james.core.EnhancedMimeMessage;
import org.apache.james.services.*;
import org.apache.james.util.InternetPrintWriter;
import org.apache.log.LogKit;
import org.apache.log.Logger;
import org.apache.mailet.Mail;

/**
 * An IMAP Handler handles one IMAP connection. TBC - it may spawn worker
 * threads someday.
 *
 * <p> Based on SMTPHandler and POP3Handler by Federico Barbieri <scoobie@systemy.it>
 *
 * @author  <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.1 on 14 Dec 2000
 */
public class SingleThreadedConnectionHandler implements ConnectionHandler {
  
    private static final boolean DEBUG = true; //mainly to switch on stack traces;

    // Connection states
    private static final int NON_AUTHENTICATED = 0;
    private static final int AUTHENTICATED = 1;
    private static final int SELECTED = 2;
    private static final int LOGOUT = 3;

    // Connection termination options
    private static final int NORMAL_CLOSE = 0;
    private static final int OK_BYE = 1;
    private static final int UNTAGGED_BYE = 2;
    private static final int TAGGED_NO = 3;
    private static final int NO_BYE = 4;

    // Basic response types
    private static final String OK = "OK";
    private static final String NO = "NO";
    private static final String BAD = "BAD";
    private static final String UNTAGGED = "*";

    private static final String SP = " ";
    private static final String VERSION = "IMAP4rev1";
    private static final String CAPABILITY_RESPONSE = "CAPABILITY " + VERSION
	+ " LOGIN-REFERRALS" + " NAMESPACE" + " MAILBOX-REFERRALS"
	+ " ACL"; //add as implemented

    private static final String LIST_WILD = "*";
    private static final String LIST_WILD_FLAT = "%";
    private static final char[] CTL = {};
    private static final String[] ATOM_SPECIALS
	= {"(", ")", "{", " ", LIST_WILD, LIST_WILD_FLAT,};

    private static final String AUTH_FAIL_MSG
	= "NO Command not authorized on this mailbox";
    private static final String BAD_LISTRIGHTS_MSG
	= "BAD Command should be <tag> <LISTRIGHTS> <mailbox> <identifier>";
    private static final String BAD_MYRIGHTS_MSG
	= "BAD Command should be <tag> <MYRIGHTS> <mailbox>";
    private static final String BAD_LIST_MSG
	= "BAD Command should be <tag> <LIST> <reference name> <mailbox>";
    private static final String BAD_LSUB_MSG
	= "BAD Command should be <tag> <LSUB> <reference name> <mailbox>";
    private static final String NO_NOTLOCAL_MSG
	= "NO Mailbox does not exist on this server";

    private ComponentManager compMgr;
    private Configuration conf;
    private Context context;
    private Logger logger= LogKit.getLoggerFor("james.IMAPServer") ;
    private Logger securityLogger = LogKit.getLoggerFor("james.Security") ;
    private MailServer mailServer;
    private UsersRepository users;
    private Scheduler scheduler;
    private long timeout;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private OutputStream outs;
    private String remoteHost;
    private String remoteIP;
    private String servername;
    private String softwaretype;
    private int state;
    private String user;

    private IMAPSystem imapSystem;
    private Host imapHost;
    private String namespaceToken;
    private String currentNamespace = null;
    private String currentSeperator = null;
    private String commandRaw;

    //currentFolder holds the client-dependent absolute address of the current
    //folder, that is current Namespace and full mailbox hierarchy.
    private String currentFolder = null;       
    private ACLMailbox currentMailbox = null;
    private boolean currentIsReadOnly = false;
    private boolean connectionClosed = false;
    private String tag;
    private boolean checkMailboxFlag = false;
    private int exists;
    private int recent;
    private List sequence;
 

 
    /**
     * Constructor has no parameters to allow Class.forName() stuff.
     */
    public void configure(Configuration conf) throws ConfigurationException {
        this.conf = conf;
	timeout = conf.getChild("connectiontimeout").getValueAsLong();
    }

    public void compose(ComponentManager comp) {
        compMgr = comp;
    }

    public void contextualize(Context context) {
        this.context = context;
    }

    public void init() throws Exception {
	try {
	    logger.info("SingleThreadedConnectionHandler starting ...");
	    this.mailServer = (MailServer)
		compMgr.lookup("org.apache.james.services.MailServer");
	    users = (UsersRepository) compMgr.lookup("org.apache.james.services.UsersRepository");
	    scheduler = (Scheduler) compMgr.lookup("org.apache.avalon.services.Scheduler");
	    softwaretype = "JAMES IMAP4rev1 Server "
		+ Constants.SOFTWARE_VERSION;
	    servername = (String) context.get(Constants.HELO_NAME);
	    imapSystem = (IMAPSystem)
		compMgr.lookup("org.apache.james.imapserver.IMAPSystem");
	    imapHost = (Host) compMgr.lookup("org.apache.james.imapserver.Host");
	    namespaceToken = imapSystem.getNamespaceToken();
	    logger.info("SingleThreadedConnectionHandler initialized");
	} catch (Exception e) {
	    logger.error("Exception initialising SingleThreadedConnectionHandler : " + e);
	    logger.error("Exception message is: " + e.getMessage());
	    e.printStackTrace();
	    throw e;
	}
    }

    public void parseRequest(Socket socket) {

        try {
            this.socket = socket;
            in = new BufferedReader(new
		InputStreamReader(socket.getInputStream()));
            outs = socket.getOutputStream();
            out = new InternetPrintWriter(outs, true);
            remoteHost = socket.getInetAddress ().getHostName ();
            remoteIP = socket.getInetAddress ().getHostAddress ();
        } catch (Exception e) {
            logger.error("Cannot open connection from " + remoteHost + " ("
		       + remoteIP + "): " + e.getMessage());
        }
        logger.info("Connection from " + remoteHost + " (" + remoteIP + ")");
    }


    public void run() {

        try {
	    scheduler.setAlarm(this.toString(), new Scheduler.Alarm(timeout), this);
           
	    if (false) { // arbitrary rejection of connection
		// could screen connections by IP or host or implement
		// connection pool management
		connectionClosed = closeConnection(UNTAGGED_BYE,
						   " connection rejected.",
						   "");
	    
	    } else {
		if (false) { // connection is pre-authenticated
		    out.println(UNTAGGED + SP + "PREAUTH" + SP + VERSION + SP
				+ "server" + SP + this.servername + SP
				+ "logged in as" + SP + user);
		    state = AUTHENTICATED;
		    user = "preauth user";
		    securityLogger.info("Pre-authenticated connection from  "
			       + remoteHost + "(" + remoteIP
			       + ") received by SingleThreadedConnectionHandler");
		} else {
		    out.println(UNTAGGED + SP + OK + SP + VERSION + SP
				+ "server " + this.servername + SP + "ready.");
		    state = NON_AUTHENTICATED;
		    user = "unknown";
		    securityLogger.info("Non-authenticated connection from  "
			       + remoteHost + "(" + remoteIP
			       + ") received by SingleThreadedConnectionHandler");
		}
		while (parseCommand(in.readLine())) {
		    scheduler.resetAlarm(this.toString());
		}
	    }

	    if (!connectionClosed) {
		connectionClosed 
		    = closeConnection(UNTAGGED_BYE,
				      "Server error, closing connection", ""); 
	    }
	   
        } catch (Exception e) {
	    // This should never happen once code is debugged
            logger.error("Exception during connection from " + remoteHost
		       + " (" + remoteIP + ") : " + e.getMessage());
	    e.printStackTrace();
	    connectionClosed = closeConnection(UNTAGGED_BYE,
					      "Error processing command.", "");
        }
    }


    public void wake(String name, Scheduler.Event event) {
        logger.info("Connection timeout on socket");
	connectionClosed = closeConnection(UNTAGGED_BYE,
					   "Autologout. Idle too long.", "");
    }


    private boolean closeConnection(int exitStatus, String message1,
				    String message2) {
	scheduler.removeAlarm(this.toString());
	if (state == SELECTED) {
	    currentMailbox.removeMailboxEventListener(this);
	    imapHost.releaseMailbox(user, currentMailbox);
	}

	try {

	    switch(exitStatus) {
	    case 0 :
		out.println(UNTAGGED + SP + "BYE" + SP + "server logging out");
		out.println(tag + SP + OK + SP + "LOGOUT completed");
		break;
	    case 1 :
		out.println(UNTAGGED + SP + "BYE" + SP + message1);
		out.println(tag + SP + OK + SP + message2);
		break;
	    case 2:
		out.println(UNTAGGED + SP + "BYE" + SP + message1);
		break;
	    case 3 :
		out.println(tag + SP + NO + SP + message1);
		break;
	    case 4 :
		out.println(UNTAGGED + SP + "BYE" + SP + message1);
		out.println(tag + SP + NO + SP + message2);
		break;
	    }
	    out.flush();
	    socket.close();
	    logger.info("Connection closed" + SP + exitStatus + SP + message1
		       +  SP + message2);
	} catch (IOException ioe) {
	    logger.error("Exception while closing connection from " + remoteHost
		       + " (" + remoteIP + ") : " + ioe.getMessage());
	    try {
                socket.close();
            } catch (IOException ioe2) {
            }
	}
	return true;
    }


    private boolean parseCommand(String next) {
	commandRaw = next;
	String folder = null;
	String command = null;
	boolean subscribeOnly = false;

        if (commandRaw == null) return false;
	//        logger.debug("Command recieved: " + commandRaw + " from " + remoteHost
	//	   + "(" + remoteIP  + ")");
        //String command = commandRaw.trim();
        StringTokenizer commandLine = new StringTokenizer(commandRaw.trim(), " ");
        int arguments = commandLine.countTokens();
        if (arguments == 0) {
            return true;
        } else {
            String test = commandLine.nextToken();
	    if (test.length() < 10) {// this stops overlong junk.
		// we should validate the tag contents
		tag = test;
	    } else {
		out.println(UNTAGGED + SP + BAD + SP + "tag too long");
		return true;
	    }
        }
        if (arguments > 1) {
            String test = commandLine.nextToken();
	    if (test.length() < 13) {// this stops overlong junk.
		// we could validate the command contents,
		// but may not be worth it
		command = test;
	    }
	    else {
		out.println(tag + SP + BAD + SP
			    + "overlong command attempted");
		return true;
	    }
        } else {
	    out.println(UNTAGGED + SP + BAD + SP + "no command sent");
	    return true;
	}
	
	// At this stage we have a tag and a string which may be a command
	// Start with commands that are valid in any state
	// CAPABILITY, NOOP, LOGOUT

	if (command.equalsIgnoreCase("CAPABILITY")) {
	    out.println(UNTAGGED + SP + CAPABILITY_RESPONSE);
	    if (state == SELECTED ) {
		checkSize();
		checkExpunge();
	    }
	    out.println(tag + SP + OK + SP + "CAPABILITY completed");
	    logger.debug("Capability command completed for " + remoteHost
		   + "(" + remoteIP  + ")");
	    return true;
	    
	} else if (command.equalsIgnoreCase("NOOP")) {
	    if (state == SELECTED ) {
		checkSize();
		checkExpunge();
	    }
	    // we could send optional untagged status responses as well
	    out.println(tag + SP + OK + SP + "NOOP completed");
	    logger.debug("Noop command completed for " + remoteHost
		   + "(" + remoteIP  + ")");
	    return true;
	    
	} else if (command.equalsIgnoreCase("LOGOUT")) {
	    connectionClosed = closeConnection(NORMAL_CLOSE, "", "");
	    return false;
	    
	}
	
	// Commands only valid in NON_AUTHENTICATED state
	// AUTHENTICATE, LOGIN
	
	if (state == NON_AUTHENTICATED) {
	    if (command.equalsIgnoreCase("AUTHENTICATE")) {
		out.println(tag + SP + NO + SP + "Auth type not supported.");
		logger.info("Attempt to use Authenticate command by "
			   + remoteHost  + "(" + remoteIP  + ")");
		securityLogger.info("Attempt to use Authenticate command by "
			   + remoteHost  + "(" + remoteIP  + ")");
		return true;
	    } else if (command.equalsIgnoreCase("LOGIN")) {
		if (arguments != 4) {
		    out.println(tag + SP + BAD + SP
		    + "Command should be <tag> <LOGIN> <username> <password>");
		    logger.info("Wrong number of arguments for LOGIN command from "
			   + remoteHost  + "(" + remoteIP  + ")");
		    return true;
		}
		user = decodeAstring(commandLine.nextToken());
		String password = decodeAstring(commandLine.nextToken());
		if (users.test(user, password)) {
		    securityLogger.info("Login successful for " + user + " from  "
			       + remoteHost + "(" + remoteIP  + ")");
		    // four possibilites handled:
		    // private mail: isLocal, is Remote
		    // other mail (shared, news, etc.) is Local, is Remote
		    
		    if (imapHost.isHomeServer(user)) {
			out.println(tag + SP + OK + SP + "LOGIN completed");
			state = AUTHENTICATED;
			
		    } else  {
			String remoteServer = null;
			try {
			    remoteServer
				= imapSystem.getHomeServer(user);
			} catch (AuthenticationException ae) {
			    connectionClosed
				= closeConnection(TAGGED_NO,
				 " cannot find your inbox, closing connection",
			         "");
			    return false;
			}
			
			if (imapHost.hasLocalAccess(user)) {
			    out.println(tag + SP + OK + SP + "[REFERRAL "
					+ remoteServer +"]" + SP 
					+ "Your home server is remote, other mailboxes available here");
			    state = AUTHENTICATED;
			   
			} else {
			    closeConnection(TAGGED_NO, " [REFERRAL" + SP
					    + remoteServer +"]" + SP 
					    + "No mailboxes available here, try remote server", "");
			    return false;
			}
		    }
		    currentNamespace = imapHost.getDefaultNamespace(user);
		    currentSeperator
			= imapSystem.getHierarchySeperator(currentNamespace);
		    // position at root of default Namespace,
		    // which is not actually a folder
		    currentFolder = currentNamespace + currentSeperator + "";
		    logger.debug("Current folder for user "  + user + " from "
			       + remoteHost  + "(" + remoteIP  + ") is "
			       + currentFolder);
		    return true;


		} // failed password test
		// We should add ability to monitor attempts to login
		out.println(tag + SP + NO + SP + "LOGIN failed");
		securityLogger.error("Failed attempt to use Login command for account "
			   + user + " from "  + remoteHost  + "(" + remoteIP
			   + ")");
		return true;
	    } 
	    // bad client
	    out.println(tag + SP + NO + SP + "Must authenticate first");
	    return true;
	} // end of if (state == NON_AUTHENTICATED) 
	
	// Commands not yet processed should be valid in either
	// Authenticated or Selected states.
	logger.debug("Command recieved: " + commandRaw + " from " + remoteHost
		   + "(" + remoteIP  + ")");

	// Commands valid in both Authenticated and Selected states
	// NAMESPACE, GETACL, SETACL, DELETEACL, LISTRIGHTS, MYRIGHTS, SELECT
	if (state == AUTHENTICATED || state == SELECTED) {
	    
	    // NAMESPACE capability ------------------------------
	    if (command.equalsIgnoreCase("NAMESPACE")) {
		String namespaces = imapSystem.getNamespaces(user);
		out.println(UNTAGGED + SP + "NAMESPACE " + namespaces);
		logger.info("Provided NAMESPACE: " + namespaces );
		if (state == SELECTED ) {
		    checkSize();
		    checkExpunge();
		}
		out.println(tag + SP + OK + SP
			    + "NAMESPACE command completed");
		return true;
		
	   // ACL Capability  ---------------------------------------
	    } else if (command.equalsIgnoreCase("GETACL")) { 
		ACLMailbox target = null;
		if (arguments != 3) {
		    out.println(tag + SP + BAD + SP +
				"Command should be <tag> <GETACL> <mailbox>");
		    return true;
		}
		folder =  getFullName(commandLine.nextToken());
		if ( state == SELECTED && currentFolder.equals(folder) ) {
		    target = currentMailbox;
		} else {
		    target = getBox(user, folder);
		    if (target == null) return true;
		}
		try {
		    out.println(UNTAGGED + SP + "ACL " + target.getName() + SP
				+ target.getAllRights(user ));
		    logger.debug(UNTAGGED + SP + "ACL " + target.getName() + SP
				 + target.getAllRights(user ));
		} catch (AccessControlException ace) {
		    out.println(tag + SP + NO + SP + "Unknown mailbox");
		    logACE(ace);
		    return true;
		} catch (AuthorizationException aze) {
		    out.println(tag + SP + AUTH_FAIL_MSG);
		    logAZE(aze);
		    return true;
		}
		if (state == SELECTED ) {
		    checkSize();
		    checkExpunge();
		}
		out.println(tag + SP + OK + SP 
			    + "GetACL command completed");
		return true;    
	    
	    } else if (command.equalsIgnoreCase("SETACL")) {  
		ACLMailbox target = null;
		if (arguments != 5) {
		    out.println(tag + SP + BAD + SP +
				"Command should be <tag> <SETACL> <mailbox> <identity> <rights modification>");
		    return true;
		}
		folder =  getFullName(commandLine.nextToken());
		String identity = commandLine.nextToken();
		String changes = commandLine.nextToken();
		
		if ( (state == SELECTED && currentFolder.equals(folder))) {
		    target = currentMailbox;
		} else {
		    target = getBox(user, folder);
		    if (target == null) return true;
		}
		
		try {
		    if (target.setRights(user, identity, changes)) {
			out.println(tag + SP + OK + SP 
				    + "SetACL command completed");
			securityLogger.info("ACL rights for "  + identity + " in "
				   + folder  + " changed by " + user + " : "
				   +  changes);
		    } else {
			out.println(tag + SP + NO + SP 
				    + "SetACL command failed");
			securityLogger.info("Failed attempt to change ACL rights for "
				   + identity + " in " + folder  + " by "
				   + user);
		    }
		} catch (AccessControlException ace) {
		    out.println(tag + SP + NO + SP + "Unknown mailbox");
		    logACE(ace);
		    return true;
		} catch (AuthorizationException aze) {
		    out.println(tag + SP + AUTH_FAIL_MSG);
		    logAZE(aze);
		    return true;
		}
		if (state == SELECTED ) {
		    checkSize();
		    checkExpunge();
		}
		return true;

		
	    } else if (command.equalsIgnoreCase("DELETEACL")) {  
		ACLMailbox target = null;
		if (arguments != 4) {
		    out.println(tag + SP + BAD + SP +
				"Command should be <tag> <DELETEACL> <mailbox> <identity>");
		    return true;
		}
		folder =  getFullName(commandLine.nextToken());
		String identity = commandLine.nextToken();
		String changes = "";
		
		if ( (state == SELECTED && currentFolder.equals(folder))) {
		    target = currentMailbox;
		} else {
		    target = getBox(user, folder);
		    if (target == null) return true;
		}
		
		try {
		    if (target.setRights(user, identity, changes)) {
			out.println(tag + SP + OK + SP
				    + "DeleteACL command completed");
			securityLogger.info("ACL rights for "  + identity + " in "
				   + folder + " deleted by " + user);
		    } else {
			out.println(tag + SP + NO + SP 
				    + "SetACL command failed");
			securityLogger.info("Failed attempt to change ACL rights for "
				   + identity + " in " + folder  + " by "
				   + user);
		    }
		} catch (AccessControlException ace) {
		    out.println(tag + SP + NO + SP + "Unknown mailbox");
		    logACE(ace);
		    return true;
		} catch (AuthorizationException aze) {
		    out.println(tag + SP + AUTH_FAIL_MSG);
		    logAZE(aze);
		    return true;
		}
		if (state == SELECTED ) {
		    checkSize();
		    checkExpunge();
		}
		return true;
	
	    } else if (command.equalsIgnoreCase("LISTRIGHTS")) { 
		ACLMailbox target = null;
		if (arguments != 4) {
		    out.println(tag + SP + BAD_LISTRIGHTS_MSG);
		    return true;
		}
		folder =  getFullName(commandLine.nextToken());
		String identity = commandLine.nextToken();
		if ( state == SELECTED && currentFolder.equals(folder) ) {
		    target = currentMailbox;
		} else {
		    target = getBox(user, folder);
		    if (target == null) return true;
		}

		try {
		    out.println(UNTAGGED + SP + "LISTRIGHTS "
				+ target.getName() + SP + identity + SP
				+ target.getRequiredRights(user, identity)
				+ SP 
				+ target.getOptionalRights(user, identity));
		    out.println(tag + SP + OK + SP 
				+ "ListRights command completed");
		} catch (AccessControlException ace) {
		    out.println(tag + SP + NO + SP + "Unknown mailbox");
		    logACE(ace);
		    return true;
		} catch (AuthorizationException aze) {
		    out.println(tag + SP + AUTH_FAIL_MSG);
		    logAZE(aze);
		    return true;
		}
		if (state == SELECTED ) {
		    checkSize();
		    checkExpunge();
		}
		return true;
	
	    } else if (command.equalsIgnoreCase("MYRIGHTS")) {
		ACLMailbox target = null;
		if (arguments != 3) {
		    out.println(tag + SP + BAD_MYRIGHTS_MSG);
		    return true;
		}
		folder =  getFullName(commandLine.nextToken());
		if ( state == SELECTED && currentFolder.equals(folder) ) {
		    target = currentMailbox;
		} else {
		    target = getBox(user, folder);
		    if (target == null) return true;
		}

		try {
		    out.println(UNTAGGED + SP + "MYRIGHTS "
				+ target.getName() + SP
				+ target.getRights(user, user));
		    out.println(tag + SP + OK + SP
				+ "MYRIGHTS command completed");
		} catch (AccessControlException ace) {
		    out.println(tag + SP + NO + SP + "Unknown mailbox");
		    logACE(ace);
		    return true;
		} catch (AuthorizationException aze) {
		    out.println(tag + SP + AUTH_FAIL_MSG);
		    logAZE(aze);
		    return true;
		}
		if (state == SELECTED ) {
		    checkSize();
		    checkExpunge();
		}
		 return true;   
	    
		
	   // Standard IMAP commands --------------------------
		
	    } else if (command.equalsIgnoreCase("SELECT")
		       || command.equalsIgnoreCase("EXAMINE")) {
		// selecting a mailbox deselects current mailbox,
		// even if this select fails
		if (state == SELECTED) {
		    currentMailbox.removeMailboxEventListener(this);
		    imapHost.releaseMailbox(user, currentMailbox);
		    state = AUTHENTICATED;
		    currentMailbox = null;
		    currentIsReadOnly = false;
		}
		
		if (arguments != 3) {
		    if (command.equalsIgnoreCase("SELECT") ){
			out.println(tag + SP + BAD + SP 
			    + "Command should be <tag> <SELECT> <mailbox>");
		    } else {
			out.println(tag + SP + BAD + SP
			     + "Command should be <tag> <EXAMINE> <mailbox>");
		    }
		    return true;
		}
		
		folder =  getFullName(commandLine.nextToken());
		currentMailbox = getBox(user,  folder);
		if (currentMailbox == null) {
		    return true;
		}
		try { // long tries clause against an AccessControlException
		    if (!currentMailbox.hasReadRights(user)) {
			out.println(tag + SP + NO + SP 
				    + "Read access not granted." );
			return true;
		    }
		    if (command.equalsIgnoreCase("SELECT") ){
			if (!currentMailbox.isSelectable(user)) {
			    out.println(tag + SP + NO + SP
					+ "Mailbox exists but is not selectable");
			    return true;
			}
		    }
		
		    // Have mailbox with at least read rights. Server setup.
		    currentMailbox.addMailboxEventListener(this);
		    currentFolder = folder;
		    state = SELECTED;
		    exists = -1;
		    recent = -1;
		    logger.debug("Current folder for user "  + user + " from "
				 + remoteHost  + "(" + remoteIP  + ") is "
				 + currentFolder);

		    // Inform client
		    out.println(UNTAGGED + SP + "FLAGS (" 
				+ currentMailbox.getSupportedFlags() + ")" );
		    if (!currentMailbox.allFlags(user)) {
			out.println(UNTAGGED + SP + OK + " [PERMANENTFLAGS ("
				    + currentMailbox.getPermanentFlags(user)
				    + ") ]");
		    }
		    checkSize();
		    out.println(UNTAGGED + SP + OK + " [UIDVALIDITY "
				+ currentMailbox.getUIDValidity() + " ]");
		    int oldestUnseen = currentMailbox.getOldestUnseen(user);
		    if (oldestUnseen > 0 ) {
			out.println(UNTAGGED + SP + OK + " [UNSEEN "
				    + oldestUnseen + " ]");
		    } else {
			out.println(UNTAGGED + SP + OK + " No unseen messages");
		    }
		    sequence = currentMailbox.listUIDs(user);

		    if (command.equalsIgnoreCase("EXAMINE")) {
			currentIsReadOnly = true;
			
			out.println(tag + SP + OK + SP
				    + "[READ-ONLY] Examine completed");
			return true;
			
		    } else if (currentMailbox.isReadOnly(user)) {
			currentIsReadOnly = true;
			out.println(tag + SP + OK + SP
				    + "[READ-ONLY] Select completed");
			return true;
		    }
		    out.println(tag + SP + OK + SP
				+ "[READ-WRITE] Select completed");
		    return true;
		} catch (AccessControlException ace) {
		    out.println(tag + SP + NO + SP + "No such mailbox.");
		    logACE(ace);
		    return true;
		}
		// End of SELECT || EXAMINE
	    } else if (command.equalsIgnoreCase("CREATE")) {
		if (arguments != 3) {
		    out.println(tag + SP + BAD + SP +
				"Command should be <tag> <CREATE> <mailbox>");
		    return true;
		}
		folder =  getFullName(commandLine.nextToken());
		if(currentFolder == folder) {
		    out.println(tag + SP + NO + SP
				+ "Folder exists and is selected." );
		    return true;
		}
		try {
		    ACLMailbox target = imapHost.createMailbox(user, folder);
		    out.println(tag + SP + OK + SP + "Create completed");
		    imapHost.releaseMailbox(user, target);
		}  catch (AccessControlException ace) {
		    out.println(tag + SP + NO + SP
			+ "No such mailbox. ");
		    logACE(ace);
		    return true;
		} catch (MailboxException mbe) {
		    if (mbe.isRemote()) {
			out.println(tag + SP + NO + SP + "[REFERRAL "
				    + mbe.getRemoteServer() +"]" 
				    + SP + "Wrong server. Try remote." );
		    } else  {
			out.println(tag + SP + NO + SP + mbe.getStatus() );
		    }
		    return true;
		} catch (AuthorizationException aze) {
		    out.println(tag + SP + NO + SP
			+ "You do not have the rights to create mailbox: "
			+ folder);
		    return true;
		}
		if (state == SELECTED ) {
		    checkSize();
		    checkExpunge();
		}
		return true;


	    } else if (command.equalsIgnoreCase("DELETE")) {
		if (arguments != 3) {
		    out.println(tag + SP + BAD + SP +
				"Command should be <tag> <DELETE> <mailbox>");
		    return true;
		}
		folder =  getFullName(commandLine.nextToken());
		if(currentFolder == folder) {
		    out.println(tag + SP + NO + SP
				+ "You can't delete a folder while you have it selected." );
		    return true;
		}
		try {
		    if( imapHost.deleteMailbox(user, folder)) {
			out.println(tag + SP + OK + SP + "Delete completed");
		    } else {
			out.println(tag + SP + NO + SP
				    + "Delete failed, unknown error");
			logger.info("Attempt to delete mailbox " + folder
				   + " by user " + user + " failed.");
		    }
		} catch (MailboxException mbe) {
		    if (mbe.getStatus().equals(MailboxException.NOT_LOCAL)) {
			out.println(tag + SP + NO_NOTLOCAL_MSG);
		    } else  {
			out.println(tag + SP + NO + SP + mbe.getMessage() );
		    }
		    return true;
		} catch (AuthorizationException aze) {
		    out.println(tag + SP + NO + SP
				+ "You do not have the rights to delete mailbox: " + folder);
		    logAZE(aze);
		    return true;
		}
		if (state == SELECTED ) {
		    checkSize();
		    checkExpunge();
		}
		return true;


	    } else if (command.equalsIgnoreCase("RENAME")) {
		if (arguments != 4) {
		    out.println(tag + SP + BAD + SP +
				"Command should be <tag> <RENAME> <oldname> <newname>");
		    return true;
		}
		folder =  getFullName(commandLine.nextToken());
		String newName = getFullName(commandLine.nextToken());
		if(currentFolder == folder) {
		    out.println(tag + SP + NO + SP
				+ "You can't rename a folder while you have it selected." );
		    return true;
		}
		try {
		    if(imapHost.renameMailbox(user, folder, newName)) {
			out.println(tag + SP + OK + SP + "Rename completed");
		    } else {
			out.println(tag + SP + NO + SP
				    + "Rename failed, unknown error");
			logger.info("Attempt to rename mailbox " + folder
				   + " to " + newName
				   + " by user " + user + " failed.");
		    }
		} catch (MailboxException mbe) {
		    if (mbe.getStatus().equals(MailboxException.NOT_LOCAL)) {
			out.println(tag + SP + NO_NOTLOCAL_MSG);
		    } else  {
			out.println(tag + SP + NO + SP + mbe.getMessage() );
		    }
		    return true;
		} catch (AuthorizationException aze) {
		    out.println(tag + SP + NO + SP 
				+ "You do not have the rights to delete mailbox: " + folder);
		    return true;
		}
		if (state == SELECTED ) {
		    checkSize();
		    checkExpunge();
		}
		return true;


	    } else if (command.equalsIgnoreCase("SUBSCRIBE")) {
		if (arguments != 3) {
		    out.println(tag + SP + BAD + SP +
				"Command should be <tag> <SUBSCRIBE> <mailbox>");
		    return true;
		}
		folder =  getFullName(commandLine.nextToken());
	
		try {
		    if( imapHost.subscribe(user, folder) ) {
			out.println(tag + SP + OK + SP 
				    + "Subscribe completed");
		    } else {
			out.println(tag + SP + NO + SP + "Unknown error." );
		    }
		} catch (MailboxException mbe) {
		    if (mbe.isRemote()) {
			out.println(tag + SP + NO + SP + "[REFERRAL " 
				    + mbe.getRemoteServer() +"]" 
				    + SP + "Wrong server. Try remote." );
		    } else  {
			out.println(tag + SP + NO + SP + "No such mailbox" );
		    }
		    return true;
		} catch (AccessControlException ace) {
		    out.println(tag + SP + NO + SP + "No such mailbox");
		    logACE(ace);
		    return true;
		}
		if (state == SELECTED ) {
		    checkSize();
		    checkExpunge();
		}
		return true;

	    } else if (command.equalsIgnoreCase("UNSUBSCRIBE")) {
		if (arguments != 3) {
		    out.println(tag + SP + BAD + SP +
				"Command should be <tag> <UNSUBSCRIBE> <mailbox>");
		    return true;
		}
		folder =  getFullName(commandLine.nextToken());
	
		try {
		    if( imapHost.unsubscribe(user, folder) ) {
			out.println(tag + SP + OK + SP 
				    + "Unsubscribe completed");
		    } else {
			out.println(tag + SP + NO + SP + "Unknown error." );
		    }
		} catch (MailboxException mbe) {
		    if (mbe.isRemote()) {
			out.println(tag + SP + NO + SP + "[REFERRAL " 
				    + mbe.getRemoteServer() +"]" 
				    + SP + "Wrong server. Try remote." );
		    } else  {
			out.println(tag + SP + NO + SP + "No such mailbox" );
		    }
		    return true;
		} catch (AccessControlException ace) {
		    out.println(tag + SP + NO + SP + "No such mailbox");
		    logACE(ace);
		    return true;
		}
		if (state == SELECTED ) {
		    checkSize();
		    checkExpunge();
		}
		return true;

	    } else if (command.equalsIgnoreCase("LIST")
		       || command.equalsIgnoreCase("LSUB")) {
		if (arguments != 4) {
		    if (command.equalsIgnoreCase("LIST")) {
			out.println(tag + SP + BAD_LIST_MSG);
		    } else {
			out.println(tag + SP + BAD_LSUB_MSG);
		    }
		    return true;
		}
		if (command.equalsIgnoreCase("LIST")) {
		    subscribeOnly =false;
		} else {
		    subscribeOnly = true;
		}
		String reference = decodeAstring(commandLine.nextToken());
		folder = decodeAstring(commandLine.nextToken());

		if (reference.equals("")) {
		    reference = currentFolder;
		} else {
		    reference = getFullName(reference);
		}
		Collection list = null;
		try {
		    list = imapHost.listMailboxes(user, reference, folder,
						  subscribeOnly);
		    if (list == null) {
			logger.debug(tag + SP + NO + SP + command
				     + " unable to interpret mailbox");
			out.println(tag + SP + NO + SP + command
				+ " unable to interpret mailbox");
		    } else if (list.size() == 0) {
			logger.debug("List request matches zero mailboxes: " + commandRaw);
			out.println(tag + SP + OK + SP + command
				    + " completed");
		    } else {
			Iterator it = list.iterator();
			while (it.hasNext()) {
			    String listResponse = (String)it.next();
			    out.println(UNTAGGED + SP + command.toUpperCase()
					+ SP + listResponse);
			    logger.debug(UNTAGGED + SP + command.toUpperCase()
					 + SP + listResponse);
			}
			out.println(tag + SP + OK + SP + command
				    + " completed");
		    }
		} catch (MailboxException mbe) {
		    if (mbe.isRemote()) {
			out.println(tag + SP + NO + SP + "[REFERRAL "
				    + mbe.getRemoteServer() +"]" 
				    + SP + "Wrong server. Try remote." );
		    } else  {
			out.println(tag + SP + NO + SP
				    + "No such mailbox" );
		    }
		    return true;
		} catch (AccessControlException ace) {
		    out.println(tag + SP + NO + SP + "No such mailbox");
		    logACE(ace);
		    return true;
		}
	    
		if (state == SELECTED ) {
		    checkSize();
		    checkExpunge();
		}
		return true;

	    } else if (command.equalsIgnoreCase("STATUS")) {
		if (arguments < 4) {
		    out.println(tag + SP + BAD + SP +
				"Command should be <tag> <STATUS> <mailboxname> (status data items)");
		    return true;
		}
		folder =  getFullName(commandLine.nextToken());
		List dataNames = new ArrayList();
		String  attr = commandLine.nextToken();
		if (! attr.startsWith("(")) { //single attr
		    out.println(tag + SP + BAD + SP +
				"Command should be <tag> <STATUS> <mailboxname> (status data items)");
		    return true;
		} else if (attr.endsWith(")")){ //single attr in paranthesis
		    dataNames.add(attr.substring(1, attr.length()-1 ));
		} else { // multiple attrs
		    dataNames.add(attr.substring(1).trim());
		    while(commandLine.hasMoreTokens()) {
			attr = commandLine.nextToken();
			if (attr.endsWith(")")) {
			    dataNames.add(attr.substring(0, attr.length()-1 ));
			} else {
			    dataNames.add(attr);
			}
		    }
		}
		try {
		    String response = imapHost.getMailboxStatus(user, folder,
							 dataNames);
		    out.println(UNTAGGED + " STATUS " + folder + " ("
				+ response + ")");
		    out.println(tag + SP + OK + SP + "Status completed");
		} catch (MailboxException mbe) {
		    if (mbe.isRemote()) {
			out.println(tag + SP + NO + SP + "[REFERRAL "
				    + mbe.getRemoteServer() +"]" 
				    + SP + "Wrong server. Try remote." );
		    } else  {
			out.println(tag + SP + NO + SP
					+ "No such mailbox" );
		    }
		    return true;
		} catch (AccessControlException ace) {
		    out.println(tag + SP + NO + SP + "No such mailbox");
		    logACE(ace);
		    return true;
		}
		if (state == SELECTED ) {
		    checkSize();
		    checkExpunge();
		}
		return true;

	    } else if (command.equalsIgnoreCase("APPEND")) {
		if (true) {
		    out.println(tag + SP + BAD + SP +
				"Append Command not yet implemented.");
		    return true;
		}

	    }
		
	} // end of Auth & Selected
	 
	    
	// Commands valid only in Authenticated State
	// None
	if (state == AUTHENTICATED) {
	    out.println(tag + SP + BAD + SP
			+ "Command not valid in this state");
	    return true;
	}
    
    
	// Commands valid only in Selected state
	// CHECK
	
	if (state == SELECTED) {
	    if (command.equalsIgnoreCase("CHECK")) {
		if (currentMailbox.checkpoint()) {
		    out.println(tag + SP + OK + SP
				+ "Check completed");
		    checkSize();
		    checkExpunge();
		    return true;
		} else {
		    out.println(tag + SP + NO + SP
				+ "Check failed");
		    return true;
		}
	    } else if (command.equalsIgnoreCase("CLOSE")) {
		try {
		    currentMailbox.expunge(user);
		} catch (Exception e) {
		    logger.error("Exception while expunging mailbox on CLOSE : " + e);
		}
		currentMailbox.removeMailboxEventListener(this);
		imapHost.releaseMailbox(user, currentMailbox);
		state = AUTHENTICATED;
		currentMailbox = null;
		currentIsReadOnly = false;
		out.println(tag + SP + OK + SP
			+ "CLOSE completed");
		return true;
	    } else if (command.equalsIgnoreCase("COPY")) {
		if (arguments < 4) {
		    out.println(tag + SP + BAD + SP +
				"Command should be <tag> <COPY> <message set> <mailbox name>");
		    return true;
		}
		List set = decodeSet(commandLine.nextToken());
		logger.debug("Fetching message set of size: " + set.size());
		String  targetFolder = getFullName(commandLine.nextToken());

		ACLMailbox targetMailbox = getBox(user,  targetFolder);
		if (targetMailbox == null) {
		    return true;
		}
		try { // long tries clause against an AccessControlException
		    if (!currentMailbox.hasInsertRights(user)) {
			out.println(tag + SP + NO + SP 
				    + "Insert access not granted." );
			return true;
		    }
		    for (int i = 0; i < set.size(); i++) {
			int msn = ((Integer)set.get(i)).intValue();
			MessageAttributes attrs = currentMailbox.getMessageAttributes(msn, user);




		    }
		} catch (AccessControlException ace) {
		    out.println(tag + SP + NO + SP + "No such mailbox.");
		    logACE(ace);
		    return true;
		} catch (AuthorizationException aze) {
		    out.println(tag + SP + NO + SP
				+ "You do not have the rights to expunge mailbox: " + folder);
		    logAZE(aze);
		    return true;
		}


		out.println(tag + SP + OK + SP
			+ "CLOSE completed");
		return true;
	    } else if (command.equalsIgnoreCase("EXPUNGE")) {
		try {
		    if(currentMailbox.expunge(user)) {
			checkExpunge();
			checkSize();
			out.println(tag + SP + OK + SP
				    + "EXPUNGE complete.");
		    } else {
			out.println(tag + SP + NO + SP
				    + "Unknown server error.");
		    }
		    return true;
		} catch (AccessControlException ace) {
		    out.println(tag + SP + NO + SP + "No such mailbox");
		    logACE(ace);
		    return true;
		} catch (AuthorizationException aze) {
		    out.println(tag + SP + NO + SP
				+ "You do not have the rights to expunge mailbox: " + folder);
		    logAZE(aze);
		    return true;
		} catch (Exception e) {
		    out.println(tag + SP + NO + SP
				+ "Unknown server error.");
		    logger.error("Exception expunging mailbox " + folder + " by user " + user + " was : " + e);
		    if (DEBUG) {e.printStackTrace();}
		    return true;
		}
	   } else if (command.equalsIgnoreCase("FETCH")) {
		if (arguments < 4) {
		    out.println(tag + SP + BAD + SP +
				"Command should be <tag> <FETCH> <message set> <message data item names>");
		    return true;
		}
		fetchCommand(commandLine, false);
		return true;
		// end of FETCH
	    } else if (command.equalsIgnoreCase("STORE")) {
		if (arguments < 5) {
		    out.println(tag + SP + BAD + SP +
				"Command should be <tag> <STORE> <message set> <message data item names> <value for message data item>");
		    return true;
		}
		storeCommand(commandLine, false);
		return true;
	    } else if (command.equalsIgnoreCase("UID")) {
		if (arguments < 4) {
		    out.println(tag + SP + BAD + SP +
				"Command should be <tag> <UID> <command> <command parameters>");
		    return true;
		}
		String uidCommand = commandLine.nextToken();
		if (uidCommand.equalsIgnoreCase("STORE")) {
		    storeCommand(commandLine, true);
		    return true;
		} else if (uidCommand.equalsIgnoreCase("FETCH")) {
		    fetchCommand(commandLine, true);
		    return true;
		}
	    } else {
		// Other commands for selected state .....
		out.println(tag + SP + BAD + SP + "Protocol error");
		return true;
		
	    } // end state SELECTED
	}
	// Shouldn't happen
	out.println(tag + SP + BAD + SP + "Protocol error");
	return true;
	
    } // end of parseCommand

    
    
    public void stop() {
	// todo
        logger.error("Stop IMAPHandler");
    }
    
    public void destroy() {
	
        logger.error("Destroy IMAPHandler");
    }
    
 

    public void receiveEvent(MailboxEvent me) {
	if (state == SELECTED) {
	    checkMailboxFlag = true;
	}
    }
    
    private ACLMailbox getBox(String user, String mailboxName) {
	ACLMailbox tempMailbox = null;
	try {
	    tempMailbox = imapHost.getMailbox(user, mailboxName);
	} catch (MailboxException me) {
	    if (me.isRemote()) {
		out.println(tag + SP + NO + SP + "[REFERRAL " + me.getRemoteServer() +"]" + SP + "Remote mailbox" );
	    } else {
		out.println(tag + SP + NO + SP + "Unknown mailbox" );
		logger.info("MailboxException in method getBox for user: "
			   + user + " mailboxName: " + mailboxName + " was "
			   + me.getMessage());
	    }
	    
	} catch (AccessControlException e) {
	    out.println(tag + SP + NO + SP + "Unknown mailbox" );
	} 
	return tempMailbox;
    }
    
    private String getFullName(String name) {
	logger.debug("Method getFullName called for " + name);
	name = decodeAstring(name);
	if (name == null) {
	    logger.error("Received null name");
	    return null;
	}
	int inbox = name.toUpperCase().indexOf("INBOX");
	if (inbox == -1) {
	    if (name.startsWith(namespaceToken)) {          //absolute reference
		return name;
	    } else if (name.startsWith(currentSeperator)) {//rooted relative ref
		return currentNamespace + name;
	    }else {                                        //unrooted relative ref
		if (currentFolder.equals(currentNamespace + currentSeperator )) {
		    return currentFolder + name;
		} else {
		    return currentFolder + currentSeperator + name;
		}
	    }
	} else {
	    return ("#mail.INBOX");

	}

	

    }

    private void logACE(AccessControlException ace) {
	securityLogger.error("AccessControlException by user "  + user
			       + " from "  + remoteHost  + "(" + remoteIP
			       + ") with " + commandRaw + " was "
			       + ace.getMessage());
    }

    private void logAZE(AuthorizationException aze) {
	securityLogger.error("AuthorizationException by user "  + user
			       + " from "  + remoteHost  + "(" + remoteIP
			       + ") with " + commandRaw + " was "
			       + aze.getMessage());
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
     * List of integers. Where the string requests ranges or uses the * wildcard, the results are
     * uids that exist in the mailbox. This minimizes attempts to refer to non-existent messages.
     */
    private List decodeUIDSet(String rawSet, List uidsList) throws IllegalArgumentException {
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

    private String decodeAstring(String rawAstring) {

	if (rawAstring.startsWith("\"")) {
	    //quoted string
	    if (rawAstring.endsWith("\"")) {
		if (rawAstring.length() == 2) {
		    return new String(); //ie blank
		} else {
		    return rawAstring.substring(1, rawAstring.length() - 1);
		}
	    } else {
		logger.error("Quoted string with no closing quote.");
		return null;
	    }
	} else {
	    //atom
	    return rawAstring;
	}
    }

    private void checkSize() {
	int newExists = currentMailbox.getExists();
	if (newExists != exists) {
	    out.println(UNTAGGED + SP + newExists + " EXISTS");
	    exists = newExists;
	}
	int newRecent = currentMailbox.getRecent();
	if (newRecent != recent) {
	    out.println(UNTAGGED + SP + newRecent + " RECENT");
	    recent = newRecent;
	}
	return;
    }

    private void checkExpunge() {
	List newList = currentMailbox.listUIDs(user);
	for (int k = 0; k < newList.size(); k++) {
	    logger.debug("New List msn " + (k+1) + " is uid "  + newList.get(k));
	}
	for (int i = sequence.size() -1; i > -1 ; i--) {
	    Integer j = (Integer)sequence.get(i);
	    logger.debug("Looking for old msn " + (i+1) + " was uid " + j);
	    if (! newList.contains((Integer)sequence.get(i))) {
		out.println(UNTAGGED + SP + (i+1) + " EXPUNGE");
	    }
	}
	sequence = newList;
	//newList = null;
	return;
    }

    private void storeCommand(StringTokenizer commandLine, boolean useUIDs) {
	List set;
	List uidsList = null;
	if (useUIDs) {
	    uidsList = currentMailbox.listUIDs(user);

	    set = decodeUIDSet(commandLine.nextToken(), uidsList);
	} else {
	    set = decodeSet(commandLine.nextToken());
	}
	StringBuffer buf = new StringBuffer();
	while (commandLine.hasMoreTokens()) {
	    buf.append(commandLine.nextToken());
	}
	String request = buf.toString();
	try {
	    for (int i = 0; i < set.size(); i++) {
		if (useUIDs) {
		    Integer uidObject = (Integer)set.get(i);
		    int uid = uidObject.intValue();
		    if (currentMailbox.setFlagsUID(uid, user, request)) {
			if (request.toUpperCase().indexOf("SILENT") == -1) {
			    String newflags = currentMailbox.getFlagsUID(uid, user);
			    int msn = uidsList.indexOf(uidObject) + 1;
			    out.println(UNTAGGED + SP + msn + SP + "FETCH (FLAGS " + newflags + " UID " + uid + ")");
			} else {
				//silent
			}
		    } else {
			//failed
			out.println(tag + SP + NO + SP + "Unable to store flags for message: " + uid);
		    }
		} else {
		    int msn = ((Integer)set.get(i)).intValue();
		    if (currentMailbox.setFlags(msn, user, request)) {
			if (request.toUpperCase().indexOf("SILENT") == -1) {
			    String newflags = currentMailbox.getFlags(msn, user);
			    out.println(UNTAGGED + SP + msn + SP + "FETCH (FLAGS " + newflags + ")");
			} else {
				//silent
			}
		    } else {
			//failed
			out.println(tag + SP + NO + SP + "Unable to store flags for message: " + msn);
		    }
		}
	    }
	    checkSize();
	    out.println(tag + SP + OK + SP + "STORE completed");
	    
	} catch (AccessControlException ace) {
	    out.println(tag + SP + NO + SP + "No such mailbox");
	    logACE(ace);
	    return;
	} catch (AuthorizationException aze) {
	    out.println(tag + SP + NO + SP
			+ "You do not have the rights to store those flags");
	    logAZE(aze);
	    return;
	}catch (IllegalArgumentException iae) {
	    out.println(tag + SP + BAD + SP
			+ "Arguments to store not recognised.");
	    logger.error("Unrecognised arguments for STORE by user "  + user
			 + " from "  + remoteHost  + "(" + remoteIP
			 + ") with " + commandRaw);
	    return;
	}
	
	return;
    }

    /**
     * Implements IMAP fetch commands against Mailbox. This implementation attempts to satisfy the fetch
     * command with the smallest objects deserialized from storage.
     *
     * Not yet complete - no partial (octet-counted or sub-parts) fetches.
     */
    private void fetchCommand(StringTokenizer commandLine, boolean useUIDs) {
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
	logger.debug("Fetching message set of size: " + set.size());
	String firstFetchArg = commandLine.nextToken();
	int pos =  commandRaw.indexOf(firstFetchArg);
	//int pos = commandRaw.indexOf(setArg) + setArg.length() + 1;
	String fetchAttrsRaw = null;
	if (firstFetchArg.startsWith("(")) { //paranthesised fetch attrs
	    fetchAttrsRaw = commandRaw.substring(pos + 1, commandRaw.lastIndexOf(")"));
	} else {
	    fetchAttrsRaw = commandRaw.substring(pos);
	}
//if (commandRaw.startsWith("(", pos)) { //paranthesised fetch attrs
//    fetchAttrsRaw = commandRaw.substring(commandRaw.indexOf("(", pos) + 1, commandRaw.lastIndexOf(")"));
//} else {
//    fetchAttrsRaw = commandRaw.substring(pos);
//}
	logger.debug("Found fetchAttrsRaw: " + fetchAttrsRaw);
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
			if (arg.equalsIgnoreCase("BODY[Header]")) {
			    response += "BODY[HEADER] ";
			} else {
			    response += "BODY.PEEK[HEADER] ";
			}
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
			Iterator lit = lines.iterator();
			while (lit.hasNext()) {
			    out.println((String)lit.next());
			}
			out.println();
			if (useUIDs) {
			    out.println(  " UID " + uid + ")");
			} else {
			    out.println( ")" );
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
			Iterator lit = lines.iterator();
			while (lit.hasNext()) {
			    out.println((String)lit.next());
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
	    checkSize();
	    return;
	} catch (AccessControlException ace) {
	    out.println(tag + SP + NO + SP + "No such mailbox");
	    logACE(ace);
	    return;
	} catch (AuthorizationException aze) {
	    out.println(tag + SP + NO + SP
			+ "You do not have the rights to read from mailbox: " + currentFolder);
	    logAZE(aze);
	    return ;
	} catch (Exception e) {
	    out.println(tag + SP + NO + SP
			+ "Unknown server error.");
	    logger.error("Exception expunging mailbox " + currentFolder + " by user " + user + " was : " + e);
	    if (DEBUG) {e.printStackTrace();}
	    return;
	}

    }
}




