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
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;

import org.apache.avalon.*;
import org.apache.avalon.blocks.*;
import org.apache.james.*;
import org.apache.james.services.*;
import org.apache.log.LogKit;
import org.apache.log.Logger;

/**
 * A single host that has an IMAP4rev1 messaging server.
 * There should be one instance of this class per instance of James.
 * An IMAP messaging system may span more than one host.
 * <p><code>String</code> parameters representing mailbox names must be the
 * full hierarchical name of the target, with namespace, as used by the
 * specified user. Examples: 
 * '#mail.Inbox' or '#shared.finance.Q2Earnings'.
 * <p>An imap Host must keep track of existing and deleted mailboxes. 
 *
 * References: rfc 2060, rfc 2193, rfc 2221
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.1 on 14 Dec 2000
 * @see FolderRecord
 * @see RecordRepository
 */

public class JamesHost implements Host, Component, Initializable {


    private Context context;
    private Configuration conf;
    private ComponentManager compMgr;
    private Logger logger  = LogKit.getLoggerFor("james.JamesHost");
    private String rootPath; // ends with File.seperator
    private File rootFolder;
    private IMAPSystem imapSystem;
    //private UserManager usersManager;
    private UsersRepository localUsers;
    private RecordRepository recordRep;
    private Map openMailboxes; //maps absoluteName to ACLMailbox
    private Map mailboxCounts; // maps absoluteName to Integer count of extant references.
    private String namespaceToken;
    private String privateNamespace;
    private String privateNamespaceSeparator;
    private String otherUsersNamespace;
    private String otherUsersNamespaceSeparator;
    private String sharedNamespace;
    private String sharedNamespaceSeparator;
   

    /*
     * Note on implemented namespaces.
     * 3 namespaces are (partially) implemented.
     * 1) Private namespace ie access to a user's own mailboxes.
     * Full mailbox names (ie what user sees) of the form:
     *   #mail.Inbox or #mail.apache.James
     * Absolute names of the form:
     *   #mail.fred.flintstone.Inbox or #mail.fred.flintstone.apache.James
     * 2) Other users namespace ie access to other users mailboxes
     * subject to access control, of course
     * Full mailbox names (ie what user sees) of the form:
     *   #users.captain.scarlet.Inbox or #users.RobinHood.apache.James
     * Absolute names of the form:
     *   #mail.captain.scarlet.Inbox or #mail.RobinHood.apache.James
     * 3) Shared mailboxes
     * not fully implemented.
     * Full mailbox names (ie what user sees) of the form:
     *   #shared.projectAlpha.masterplan or #shared.customerservice.Inbox
     * Absolute names of the form:
     *   #shared.projectAlpha.masterplan or #shared.customerservice.Inbox
     */

    /*
     * Note on filename extensions
     * Records in AvalonRecordRepository - no extension
     * Mailboxes - mbr
     * MimeMessage - msg
     * MessageAttributes - att
     */


    /* No constructor */

    public void configure(Configuration conf) throws ConfigurationException {
        this.conf = conf;
    }
    
    public void contextualize(Context context) {
        this.context = context;
    }
    
    public void compose(ComponentManager comp) {
        compMgr = comp;
    }
    
    public void init() throws Exception {

        logger.info("JamesHost init...");
	imapSystem = (IMAPSystem) compMgr.lookup("org.apache.james.imapserver.IMAPSystem");
	localUsers = (UsersRepository)compMgr.lookup("org.apache.james.services.UsersRepository");
	String recordRepDest
	    = conf.getChild("recordRepository").getValue();
	recordRep = new DefaultRecordRepository();
	recordRep.setPath(recordRepDest);
	logger.info("AvalonRecordRepository opened at " + recordRepDest);
	rootPath = conf.getChild("mailboxRepository").getValue();
	if (!rootPath.endsWith(File.separator)) {
	    rootPath = rootPath + File.separator;
	}
	rootFolder =  new File(rootPath);
	if (!rootFolder.isDirectory()) {
	    if (! rootFolder.mkdir()){
		throw new RuntimeException("Error: Cannot create directory for MailboxRepository");
	    }
	} else if (!rootFolder.canWrite()) {
	    throw new RuntimeException("Error: Cannot write to directory for MailboxRepository");
	}
	logger.info("IMAP Mailbox Repository opened at " + rootPath);
	Configuration namespaces = conf.getChild("namespaces");
	namespaceToken = namespaces.getAttribute("token");
	privateNamespace
	    = namespaces.getChild("privateNamespace").getValue();
	privateNamespaceSeparator
	    = namespaces.getChild("privateNamespace").getAttribute("separator");
	otherUsersNamespace
	    = namespaces.getChild("otherusersNamespace").getValue();
	otherUsersNamespaceSeparator
	    = namespaces.getChild("otherusersNamespace").getAttribute("separator");
	sharedNamespace
	    = namespaces.getChild("sharedNamespace").getValue();
	sharedNamespaceSeparator
	    = namespaces.getChild("sharedNamespace").getAttribute("separator");
	logger.info("Handling mail for namespaces: "+ privateNamespace + ", " + otherUsersNamespace + ", " + sharedNamespace);
	openMailboxes = new HashMap(31); // how big should this start?
	mailboxCounts = new HashMap(31);
        logger.info("JamesHost ...init end");
    }

   public void destroy() throws Exception {
   }



    /**
     * Establishes whether this host is the Home Server for the specified user.
     * Used during login to decide whether a LOGIN_REFERRAL has to be sent to
     * the client.
     *
     * @param username an email address
     * @returns true if inbox (and private mailfolders) are accessible through
     * this host. 
     */
    public boolean isHomeServer (String username) {
	return localUsers.contains(username);
    }


   /**
    * Establishes if the specified user can access any mailboxes on this host.
    * Used during login process to decide what sort of LOGIN-REFERRAL must be
    * sent to client.
    *
    * @param username an email address
    * @returns true if the specified user has at least read access to any
    * mailboxes on this host.
     */
    public boolean hasLocalAccess (String username) {
	return localUsers.contains(username);
    }

    /**
     * Returns a reference to an existing Mailbox. The requested mailbox
     * must already exists on this server and the requesting user must have at
     * least lookup rights.
     *
     * @param user email address on whose behalf the request is made.
     * @param mailboxName String name of the target.
     * @returns an Mailbox reference.
     * @throws AccessControlException if the user does not have at least
     * lookup rights.
     * @throws MailboxException if mailbox does not exist locally.
     */
    public synchronized ACLMailbox getMailbox(String user, String mailboxName)
	throws AccessControlException, MailboxException { 
	if (user == null || mailboxName == null) {
	    logger.error("Null parameters received in getMailbox(). " );
	    throw new RuntimeException("Null parameters received.");
	} else if (user.equals("")
		   ||(!mailboxName.startsWith(namespaceToken))) {
	    logger.error("Empty/ incorrect parameters received in getMailbox().");
	    throw new RuntimeException("Empty/incorrect parameters received.");
	}
	logger.debug("Getting mailbox " + mailboxName + " for " + user);
	String absoluteName = getAbsoluteName(user, mailboxName);
	if (absoluteName == null) {
	    logger.error("Parameters in getMailbox() cannot be interpreted. ");
	    throw new RuntimeException("Parameters in getMailbox() cannot be interpreted.");
	}
	return getAbsoluteMailbox(user, absoluteName);
    }

    private synchronized ACLMailbox getAbsoluteMailbox(String user, String absoluteName)
	throws AccessControlException, MailboxException { 

	ACLMailbox mailbox = null;
	FolderRecord record = null;

	// Has a folder with this name ever been created?
	if(! recordRep.containsRecord(absoluteName)) {
	    throw new MailboxException("Mailbox: " + absoluteName + " has never been created.", MailboxException.NOT_LOCAL);
	} else {
	    record = recordRep.retrieve(absoluteName);
	    if (record.isDeleted()) {
		throw new MailboxException("Mailbox has been deleted", MailboxException.LOCAL_BUT_DELETED);
	    } else if (openMailboxes.containsKey(absoluteName)) {
		mailbox = (ACLMailbox) openMailboxes.get(absoluteName);
		if (!mailbox.hasLookupRights(user)) {
		    throw new AccessControlException("No lookup rights.");
		} else {
		    Integer c = (Integer)mailboxCounts.get(absoluteName);
		    int count = c.intValue() + 1;
		    mailboxCounts.put(absoluteName, (new Integer(count)));
		    logger.info("Request no " + count + " for " + absoluteName);
		    return mailbox;
		}
	    } else {
		String owner = record.getUser();
		String key = getPath(absoluteName, owner);
		ObjectInputStream in = null;
		try {
		    in	= new ObjectInputStream( new FileInputStream(key + File.separator + FileMailbox.MAILBOX_FILE_NAME) );
		    mailbox = (FileMailbox) in.readObject();
		    mailbox.configure(conf);
		    mailbox.contextualize(context);
		    mailbox.compose(compMgr);
		    mailbox.reInit();
		} catch (Exception e) {
		    e.printStackTrace();
		    throw new
			RuntimeException("Exception caught while reading FileMailbox: " + e);
		} finally {
		    if (in != null) {
			try {
			    in.close();
			} catch (Exception ignored) {
			}
		    }
		    notifyAll();
		}
		if (!mailbox.hasLookupRights(user)) {
		    throw new AccessControlException("No lookup rights.");
		}
		openMailboxes.put(absoluteName, mailbox);
		mailboxCounts.put(absoluteName, new Integer(1));
		return mailbox;
	    }
	}
    }


    /**
     * Returns a reference to a newly created Mailbox. The request should
     * specify a mailbox that does not already exist on this server, that
     * could exist on this server and that the user has rights to create.
     * If a system allocates different namespaces to different hosts then a
     * request to create a mailbox in a namespace not served by this host would
     * be an error.
     * It is an error to create a mailbox with the name of a mailbox that has
     * been deleted, if that name is still in use. 
     *
     * @param user email address on whose behalf the request is made.
     * @param mailboxName String name of the target
     * @returns an Mailbox reference.
     * @throws AccessControlException if the user does not have lookup rights
     * for parent or any needed ancestor folder
     * lookup rights.
     * @throws AuthorizationException if mailbox could be created locally but
     * user does not have create rights.
     * @throws MailboxException if mailbox already exists, locally or remotely,
     * or if mailbox cannot be created locally.
     * @see FolderRecord
     */
    public synchronized ACLMailbox createMailbox(String user, String mailboxName)
	throws AccessControlException, AuthorizationException,
	       MailboxException {
	if (user == null || mailboxName == null) {
	    logger.error("Null parameters received in createMailbox(). " );
	    throw new RuntimeException("Null parameters received.");
	} else if (user.equals("")
		   ||(!mailboxName.startsWith(namespaceToken))) {
	    logger.error("Empty/ incorrect parameters received in createMailbox().");
	    throw new RuntimeException("Empty/incorrect parameters received.");
	}
	String absoluteName = getAbsoluteName(user, mailboxName);
	if (absoluteName == null) {
	    logger.error("Parameters in createMailbox() cannot be interpreted. ");
	    throw new RuntimeException("Parameters in createMailbox() cannot be interpreted.");
	}
	logger.debug("JamesHost createMailbox() for:  " + absoluteName);

	return createAbsoluteMailbox(user, absoluteName);
    }

    private synchronized ACLMailbox createAbsoluteMailbox(String user, String absoluteName)
	throws AccessControlException, AuthorizationException,
	       MailboxException {
	ACLMailbox mailbox = null;
	FolderRecord record = null;
	ACLMailbox parentMailbox = null;

	// Has a folder with this name ever been created?
	if( recordRep.containsRecord(absoluteName)) {
	    record = recordRep.retrieve(absoluteName);
	    if (!record.isDeleted()) {
		logger.error("Attempt to create an existing Mailbox.");
		throw new MailboxException("Mailbox already exists", MailboxException.ALREADY_EXISTS_LOCALLY);
	    }
	} else {
	    String parent
		= absoluteName.substring(0, absoluteName.lastIndexOf(privateNamespaceSeparator));
	    if (!(parent.startsWith(privateNamespace + privateNamespaceSeparator) || parent.startsWith(sharedNamespace + sharedNamespaceSeparator))) {
		logger.warn("No such parent: " + parent);
		throw new MailboxException("No such parent: " + parent);
	    }
	    //Recurse to a created and not deleted mailbox
	    try {
		parentMailbox = getAbsoluteMailbox(user, parent);
	    } catch (MailboxException mbe) {
		if (mbe.getStatus().equals(MailboxException.NOT_LOCAL)
		    || mbe.getStatus().equals(MailboxException.LOCAL_BUT_DELETED)) {
		    parentMailbox = createAbsoluteMailbox(user, parent);
		} else {
		    throw new MailboxException(mbe.getMessage(), mbe.getStatus());
		}
	    }
	    // Does user have create rights in parent mailbox?
	    if (!parentMailbox.hasCreateRights(user)) {
		throw new AuthorizationException("User does not have create rights.");
	    } 
	    try {
		mailbox = new FileMailbox();
		mailbox.configure(conf);
		mailbox.contextualize(context);
		mailbox.compose(compMgr);
		mailbox.prepareMailbox(user, absoluteName, user);
		mailbox.init();
	    } catch (Exception e) {
		logger.error("Exception creating mailbox: " + e);
		throw new MailboxException("Exception creating mailbox: " + e);
	    }
	     String mailboxName
		= absoluteName.substring(0, absoluteName.indexOf(user))
		+ absoluteName.substring(absoluteName.indexOf(user) + user.length(), absoluteName.length());
	    SimpleFolderRecord fr
		= new SimpleFolderRecord(mailboxName, user, absoluteName);
	    fr.init();
	    recordRep.store(fr);
	    openMailboxes.put(absoluteName, mailbox);
	    mailboxCounts.put(absoluteName, new Integer(1));
	}
	
	return mailbox;
    }

   /**
     * Releases a reference to a mailbox, allowing Host to do any housekeeping.
     *
     * @param mbox a non-null reference to an ACL Mailbox.
     */
    public void releaseMailbox(String user, ACLMailbox mailbox) {
	if (mailbox == null) {
	    logger.debug("Attempt to release mailbox with null reference");
	    return;
	}
	if (user != MailServer.MDA) {
	    mailbox.unsetRecent();
	}
	String absName = mailbox.getAbsoluteName();
	Integer c = (Integer)mailboxCounts.get(absName);
	int count = c.intValue() - 1;
	if (count < 1) {
	    openMailboxes.remove(absName);
	    mailboxCounts.remove(absName);
	    try {
		FolderRecord fr = recordRep.retrieve(absName);
		fr.setUidValidity(mailbox.getUIDValidity());
		fr.setHighestUid(mailbox.getNextUID() -1);
		fr.setLookupRights(mailbox.getUsersWithLookupRights());
		fr.setReadRights(mailbox.getUsersWithReadRights());
		fr.setMarked(mailbox.isMarked());
		fr.setNotSelectableByAnyone(mailbox.isNotSelectableByAnyone());
		fr.setExists(mailbox.getExists());
		fr.setRecent(mailbox.getRecent());
		fr.setUnseenbyUser(mailbox.getUnseenByUser());
		recordRep.store(fr);
		mailbox.dispose();
		mailbox = null;
		logger.info("Mailbox object destroyed: " + absName);
	    } catch (Exception e) {
		logger.error("Exception destroying mailbox object: " + e);
		e.printStackTrace();
	    }
	} else {
	    logger.info("Mailbox " + absName + " now has " + count + "live references");
	    mailboxCounts.put(absName, (new Integer(count)));
	}

    }

    /**
     * Deletes an existing MailBox. Specified mailbox must already exist on
     * this server, and the user must have rights to delete it. (Mailbox delete
     * rights are implementation defined, one way is if the user would have the
     * right to create it).
     * Implementations must track deleted mailboxes
     *
     * @param user email address on whose behalf the request is made.
     * @param mailboxName String name of the target
     * @returns true if mailbox deleted successfully
     * @throws MailboxException if mailbox does not exist locally or is any
     * identities INBOX.
     * @throws AuthorizationException if mailbox exists locally but user does
     * not have rights to delete it.
     * @see FolderRecord
     */
    public boolean deleteMailbox(String user, String mailboxName)
	throws MailboxException, AuthorizationException {
	if (user == null || mailboxName == null) {
	    logger.error("Null parameters received in deleteMailbox(). ");
	    throw new RuntimeException("Null parameters received.");
	} else if (user.equals("")
		   ||(!mailboxName.startsWith(namespaceToken))) {
	    logger.error("Empty/ incorrect parameters received in deleteMailbox().");
	    throw new RuntimeException("Empty/incorrect parameters received.");
	}
	String absoluteName = getAbsoluteName(user, mailboxName);
	if (absoluteName == null) {
	    logger.error("Parameters in deleteMailbox() cannot be interpreted. ");
	    throw new RuntimeException("Parameters in deleteMailbox() cannot be interpreted.");
	}
	logger.debug("JamesHost deleteMailbox() called for:  " + absoluteName);
	return false;
	//return deleteAbsoluteMailbox(user, absoluteName);
    }

   

    /**
     * Renames an existing MailBox. The specified mailbox must already
     * exist locally, the requested name must not exist locally already but
     * must be able to be created locally and the user must have rights to
     * delete the existing mailbox and create a mailbox with the new name.
     * Any inferior hierarchical names must also be renamed.
     * If INBOX is renamed, the contents of INBOX are transferred to a new
     * folder with the new name, but INBOX is not deleted. If INBOX has
     * inferior mailboxes these are not renamed.
     * It is an error to create a mailbox with the name of a mailbox that has
     * been deleted, if that name is still in use. 
     * Implementations must track deleted mailboxes
     *

     * @param user email address on whose behalf the request is made.
     * @param oldMailboxName String name of the existing mailbox
     * @param newMailboxName String target new name
     * @returns true if rename completed successfully
     * @throws MailboxException if mailbox does not exist locally, or there
     * is an existing mailbox with the new name. 
     * @throws AuthorizationException if user does not have rights to delete
     * the existing mailbox or create the new mailbox.
     * @see FolderRecord
     */
    public boolean renameMailbox(String user, String oldMailboxName,
				 String newMailboxName)
	throws MailboxException, AuthorizationException {
	return false;
    }

    /**
     * Returns the namespace which should be used for this user unless they
     * expicitly request another.
     * 
     * @param username String an email address
     * @returns a String of a namespace
     */
    public String getDefaultNamespace(String username) {
	return privateNamespace;
    }


    /**
     * Return UIDValidity for named mailbox. Implementations should track
     * existing and deleted folders. 
     *
     * @param mailbox String name of the existing mailbox
     * @returns  an integer containing the current UID Validity value.
     */
    //  public int getUIDValidity(String mailbox);


    /**
     * Returns an iterator over an unmodifiable collection of Strings
     * representing mailboxes on this host and their attributes. The specified
     * user must have at least lookup rights for each mailbox returned.
     * If the subscribedOnly flag is set, only mailboxes to which the
     * specified user is currently subscribed should be returned.
     * Implementations that may export circular hierarchies SHOULD restrict the
     * levels of hierarchy returned. The depth suggested by rfc 2683 is 20
     * hierarchy levels.
     * <p>The reference name must be non-empty. If the mailbox name is empty,
     * implementations must not throw either exception but must return a single
     * String (described below) if the reference name specifies a local mailbox
     * accessible to the user and a one-character String containing the
     * hierarchy delimiter of the referenced namespace, otherwise. 
     * <p>Each String returned should be a space seperated triple of name
     * attributes, hierarchy delimiter and full mailbox name.   The mailbox
     * name should include the namespace and be relative to the specified user.
     * <p> RFC comments: Implementations SHOULD return quickly. They SHOULD
     * NOT go to excess trouble to calculate\Marked or \Unmarked status.
     * <p>JAMES comment: By elimination, implementations should usually include
     * \Noinferiors or \Noselect, if appropriate. Also, if the reference name
     * and mailbox name resolve to a single local mailbox, implementations
     * should establish all attributes.
     * <p> Note that servers cannot unilaterally remove mailboxes from the
     * subscribed list. A request with the subscribedOnly flag set that
     * attempts to list a deleted mailbox must return that mailbox with the
     * \Noselect attribute.
     *
     * @param username String non-empty email address of requester
     * @param referenceName String non-empty name, including namespace, of a
     * mailbox or level of mailbox hierarchy, relative to user.
     * @param mailboxName String name of a mailbox possible including a
     * wildcard.
     * @param subscribedOnly only return mailboxes currently subscribed.
     * @returns Collection of strings representing a set of mailboxes.
     * @throws AccessControlException if the user does not have at least
     * lookup rights to at least one mailbox in the set requested.
     * @throws MailboxException if the referenceName is not local or if
     * referenceName and mailbox name resolve to a single mailbox which does
     * not exist locally.
     */
    public synchronized Collection listMailboxes(String username,
					       String referenceName,
					       String mailboxName,
					       boolean subscribedOnly)
	throws MailboxException, AccessControlException {
	logger.debug("Listing for user: " + username + " ref " + referenceName + " mailbox " + mailboxName);
	List responseList = new ArrayList();
	if (subscribedOnly == true ) {
	    return null;
	}
	if (mailboxName.equals("")) {
	    // means don't List but give root of hierarchy and separator
	    String response;
	    if (referenceName.startsWith(privateNamespace)) {
		response = "(\\Noselect) \"" + privateNamespaceSeparator
		    + "\" " + privateNamespace;
	    } else if (referenceName.startsWith(otherUsersNamespace)) {
		response = "(\\Noselect) \"" + otherUsersNamespaceSeparator
		    + "\" " + otherUsersNamespace;
	    } else if (referenceName.startsWith(sharedNamespace)) {
		response = "(\\Noselect) \"" + sharedNamespaceSeparator
		    + "\" " + sharedNamespace;
	    } else {
		logger.error("Weird arguments for LIST? referenceName was: " + referenceName + " and mailbox names was " + mailboxName);
		return null;
	    }
	    responseList.add(response);
	    return responseList;;
	}

	//short-circuit evaluation for namespaces
	String response = null;
	if (mailboxName.equals(privateNamespace + "%")) {
	    response = "(\\Noselect) \"" + privateNamespaceSeparator +  "\" " + privateNamespace;
	} else if (mailboxName.equals(otherUsersNamespace + "%")) {
	    response = "(\\Noselect) \"" + otherUsersNamespaceSeparator +  "\" " + otherUsersNamespace;
	} else if (mailboxName.equals(otherUsersNamespace + "%")) {
	    response = "(\\Noselect) \"" + sharedNamespaceSeparator +  "\" " + sharedNamespace;
	}
	if (response != null) {
	    responseList.add(response);
	    return responseList;
	}
	try { // for debugging purposes
	  
	    //Short-circuit for Netscape client calls - remove first % in, e.g. #mail%.%
	    // Eventually we need to handle % anywhere in mailboxname
	    if (mailboxName.startsWith(privateNamespace + "%")) {
		mailboxName = privateNamespace + mailboxName.substring(privateNamespace.length() + 1);
	    } else if (mailboxName.startsWith(otherUsersNamespace + "%")) {
		mailboxName = otherUsersNamespace + mailboxName.substring(otherUsersNamespace.length() + 1);
	    } else if (mailboxName.startsWith(sharedNamespace + "%")) {
		mailboxName = sharedNamespace + mailboxName.substring(sharedNamespace.length() + 1);
	    }
	    
	    //mailboxName = mailboxName.substring(0,mailboxName.length() -1);
	    logger.debug("Refined mailboxName to: " + mailboxName);
	    String userTarget;
	    if (mailboxName.startsWith("#")) {
		userTarget = mailboxName;
	    } else {
		if (referenceName.endsWith(".")) {
		    userTarget = referenceName + mailboxName;
		} else {
		    userTarget = referenceName + "." + mailboxName;
		}
	    }
	    String target = getAbsoluteName(username, userTarget);
	    logger.info("Target is: " + target);
	    if (target == null) { return new HashSet();}
	    int firstPercent = target.indexOf("%");
	    int firstStar = target.indexOf("*");
	    logger.info("First percent at index: " + firstPercent);
	    logger.info("First star at index: " + firstStar);
	    Iterator all = recordRep.getAbsoluteNames();
	    Set matches = new HashSet();

	    while (all.hasNext()) {
		boolean match = false;
		String test = (String)all.next();
		logger.info("Test is: " + test);
		if (firstPercent == -1 && firstStar == -1) {
		    // no wildcards so exact or nothing
		    match = test.equals(target);
		    logger.debug("match/ no match at test 1"); 
		} else if (firstStar == -1) {
		    // only % wildcards
		    if (!test.startsWith(target.substring(0, firstPercent))) {
			match = false;
			logger.debug("fail match at test 2");
		    } else if (firstPercent == target.length() -1) {
			// only one % and it is terminating char
			target = target.substring(0, firstPercent);
			logger.debug("Refined target to: " + target);
			if (test.equals(target)) {
			    match = true;
			    logger.debug("pass match at test 3");
			} else if ( (test.length() > target.length())
				    &&  (test.indexOf('.', target.length())
					 == -1)
				    ) {
			    match = true;
			    logger.debug("pass match at test 4");
			} else {
			    match = false;
			    logger.debug("fail match at test 5");
			}
		    } else {
			int secondPercent = target.indexOf("%", firstPercent + 1);
			match = false; // unfinished
			logger.debug("fail match at test 6");
		    }
		} else {
		    //at least one star
		    int firstWildcard = -1;
		    if (firstPercent != -1 && firstStar == -1) {
			firstWildcard = firstPercent;
		    } else if (firstStar != -1 && firstPercent == -1) {
			firstWildcard = firstStar;
		    } else if (firstPercent < firstStar) {
			firstWildcard = firstPercent;
		    } else {
			firstWildcard = firstStar;
		    }

		    if (!test.startsWith(target.substring(0, firstWildcard))) {
			match = false;
		    } else {
			match = false;
		    }
		}
		  

		if (match)  {
		    logger.info("Processing match for : " + test);
		    FolderRecord record = recordRep.retrieve(test);
		    ACLMailbox mailbox = null;
		    StringBuffer buf = new StringBuffer();
		    buf.append("(");
		    if (!record.isDeleted() && openMailboxes.containsKey(target)) {
			mailbox = (ACLMailbox) openMailboxes.get(target);
		    }
		    if (record.isDeleted()) {
			buf.append("\\Noselect");
		    } else if(openMailboxes.containsKey(target)) {
			mailbox = (ACLMailbox) openMailboxes.get(target);
			if (!mailbox.isSelectable(username)) {
			    buf.append("\\Noselect");
			}
			if (mailbox.isMarked()) {
			    buf.append("\\Marked");
			} else {
			    buf.append("\\Unmarked");
			}
		    } else {
			if (!record.isSelectable(username)) {
			    buf.append("\\Noselect");
			}
			if (record.isMarked()) {
			    buf.append("\\Marked");
			} else {
			    buf.append("\\Unmarked");
			}
		    }
		    buf.append(") \"");
		    if(userTarget.startsWith(privateNamespace)) {
			buf.append(privateNamespaceSeparator);
		    } else if(userTarget.startsWith(otherUsersNamespace)) {
			buf.append(otherUsersNamespaceSeparator);
		    } else {
			buf.append(sharedNamespaceSeparator);
		    }
		    buf.append("\" ");
		    if (test.toUpperCase().indexOf("INBOX") == -1) {
			buf.append(getFullMailboxName(username, test) );
		    } else {
			buf.append( "INBOX");
		    }
		    matches.add(buf.toString());
		}
	    }
	    return matches;
	} catch (Exception e) {
	    logger.error("Exception with list request for mailbox " + mailboxName);
	    e.printStackTrace();
	    return null;
	}
	
    }
    
    /**
     * Subscribes a user to a mailbox. The mailbox must exist locally and the
     * user must have at least lookup rights to it.
     *
     * @param username String representation of an email address
     * @param mailbox String representation of a mailbox name.
     * @returns true if subscribe completes successfully
     * @throws AccessControlException if the mailbox exists but the user does
     * not have lookup rights.
     * @throws MailboxException if the mailbox does not exist locally.
     */
    public boolean subscribe(String username, String mailbox)
	throws MailboxException, AccessControlException {
	return false;
    }

    /**
     * Unsubscribes from a given mailbox. 
     *
     * @param username String representation of an email address
     * @param mailbox String representation of a mailbox name.
     * @returns true if unsubscribe completes successfully
     */
    public boolean unsubscribe(String username, String mailbox)
	throws MailboxException, AccessControlException {
	return false;
    }


    /**
     * Returns a string giving the status of a mailbox on requested criteria.
     * Currently defined staus items are:
     * MESSAGES - Nummber of messages in mailbox
     * RECENT - Number of messages with \Recent flag set
     * UIDNEXT - The UID that will be assigned to the next message entering
     * the mailbox
     * UIDVALIDITY - The current UIDValidity value for the mailbox
     * UNSEEN - The number of messages which do not have the \Seen flag set.
     *
     * @param username String non-empty email address of requester
     * @param mailboxName String name of a mailbox (no wildcards allowed).
     * @param dataItems Vector of one or more Strings each of a single
     * status item.
     * @returns String consisting of space seperated pairs:
     * dataItem-space-number.
     * @throws AccessControlException if the user does not have at least
     * lookup rights to the mailbox requested.
     * @throws MailboxException if the mailboxName does not exist locally. 
     */
    public String getMailboxStatus(String username, String mailboxName,
				   List dataItems)
	throws MailboxException, AccessControlException {
	String absoluteName = getAbsoluteName(username, mailboxName);
	ACLMailbox mailbox = null;
	FolderRecord record = null;
	Iterator it = dataItems.iterator();
	String response = null;

	// Has a folder with this name ever been created?
	if(! recordRep.containsRecord(absoluteName)) {
	    throw new MailboxException("Mailbox: " + absoluteName + " has never been created.", MailboxException.NOT_LOCAL);
	} else {
	    record = recordRep.retrieve(absoluteName);
	    if (record.isDeleted()) {
		throw new MailboxException("Mailbox has been deleted", MailboxException.LOCAL_BUT_DELETED);
	    } else if (openMailboxes.containsKey(absoluteName)) {
		response = new String();
		mailbox = (ACLMailbox) openMailboxes.get(absoluteName);
		if (!mailbox.hasLookupRights(username)) {
		    throw new AccessControlException("No lookup rights.");
		} 
		while(it.hasNext()) {
		    String dataItem = (String) it.next();
		    if (dataItem.equalsIgnoreCase("MESSAGES")) {
			response += "MESSAGES " + mailbox.getExists();
		    } else if (dataItem.equalsIgnoreCase("RECENT")) {
			response += "RECENT " + mailbox.getRecent();
		    } else if (dataItem.equalsIgnoreCase("UIDNEXT")) {
			response += "UIDNEXT " + mailbox.getNextUID();
		    } else if (dataItem.equalsIgnoreCase("UIDVALIDITY")) {
			response += "UIDVALIDITY " + mailbox.getUIDValidity();
		    } else if (dataItem.equalsIgnoreCase("UNSEEN")) {
			response += "UNSEEN " + mailbox.getUnseen(username);
		    }
		    if (it.hasNext()) { response += " ";}
		}
		return response;
	    } else {
		if (!record.hasLookupRights(username)) {
		    throw new AccessControlException("No lookup rights.");
		} 
		response = new String();
		while(it.hasNext()) {
		    String dataItem = (String) it.next();
		    if (dataItem.equalsIgnoreCase("MESSAGES")) {
			response += "MESSAGES " + record.getExists();
		    } else if (dataItem.equalsIgnoreCase("RECENT")) {
			response += "RECENT " + record.getRecent();
		    } else if (dataItem.equalsIgnoreCase("UIDNEXT")) {
			response += "UIDNEXT " + (record.getHighestUid() + 1);
		    } else if (dataItem.equalsIgnoreCase("UIDVALIDITY")) {
			response += "UIDVALIDITY " + record.getUidValidity();
		    } else if (dataItem.equalsIgnoreCase("UNSEEN")) {
			response += "UNSEEN " + record.getUnseen(username);
		    }
		    if (it.hasNext()) { response += " ";}
		}
		return response;
	    }
	}
    }

    /**
     * Convert a user-based full mailbox name into a server absolute name.
     * Example:
     * <br> Convert from "#mail.INBOX" for user "Fred.Flinstone" into 
     * absolute name: "#mail.Fred.Flintstone.INBOX"
     *
     * @returns String of absoluteName, null if not valid selection
     */
    private String getAbsoluteName(String user, String fullMailboxName) {
	 
	if (fullMailboxName.equals(privateNamespace)) {
	    return fullMailboxName + user + privateNamespaceSeparator;
	} else if (fullMailboxName.equals(privateNamespace
				   + privateNamespaceSeparator)) {
	    return fullMailboxName + user + privateNamespaceSeparator;
	} else if (fullMailboxName.startsWith(privateNamespace)) {
	    return new String(privateNamespace + privateNamespaceSeparator
			      + user + privateNamespaceSeparator
			      + fullMailboxName.substring(privateNamespace.length()  + privateNamespaceSeparator.length()));
	} else if (fullMailboxName.equals(otherUsersNamespace)) {
	    return null;
	}else if (fullMailboxName.equals(otherUsersNamespace
				   + otherUsersNamespaceSeparator)) {
	    return null;
	} else if (fullMailboxName.startsWith(otherUsersNamespace)) {
	   
	    return new String(privateNamespace + privateNamespaceSeparator
			      + fullMailboxName.substring(otherUsersNamespace.length() + otherUsersNamespaceSeparator.length()));
	    
	} else if (fullMailboxName.startsWith(sharedNamespace)) {
	    return fullMailboxName;
	} else {
	    return null;
	}
    }


    private String getFullMailboxName(String user, String absoluteName) {

	if(absoluteName.startsWith(privateNamespace)) {
	    if (absoluteName.equals(privateNamespace + privateNamespaceSeparator  + user)) {
		return new String(privateNamespace );
	    } else if (absoluteName.startsWith(privateNamespace + privateNamespaceSeparator  + user)){
		return new String(privateNamespace 
				  + absoluteName.substring(privateNamespace.length()  + privateNamespaceSeparator.length()  + user.length()));
	    } else {
		// It's another users mailbox
		// Where is separator between name and mailboxes?
		int pos = absoluteName.substring(privateNamespace.length() + privateNamespaceSeparator.length()).indexOf(privateNamespaceSeparator);
		return new String(otherUsersNamespace
				  + otherUsersNamespaceSeparator
				  + absoluteName.substring(pos ));
	    }
	} else if (absoluteName.startsWith(sharedNamespace)) {
	    return absoluteName;
	} else {
	    return null;
	}
    }



    /**
     * Return the file-system path to a given absoluteName mailbox.
     *
     * @param absoluteName the user-independent name of the mailbox
     * @param owner string name of owner of mailbox
     */
    private String getPath(String absoluteName, String owner) {
	String path;
	if (absoluteName.startsWith(privateNamespace)) {
	    String path1 = rootPath + owner;
	    String path2
		= absoluteName.substring(privateNamespace.length()
					 + privateNamespaceSeparator.length()
					 + owner.length());
	    path = path1 + path2.replace(privateNamespaceSeparator.charAt(0), File.separatorChar);
	} else if (absoluteName.startsWith(sharedNamespace)) {
	    String path3 = absoluteName.substring(namespaceToken.length());
	    path = rootPath + File.separator + path3.replace(privateNamespaceSeparator.charAt(0), File.separatorChar);
	} else {
	    path = null;
	}
	return path;
    }


    public boolean createPrivateMailAccount(String user) {
	if (user == null || user.equals("")) {
	    throw new RuntimeException("Bad parameter for createPrivateMailAccount.");
	}
	
	String userRootAbsName
	    =  privateNamespace + privateNamespaceSeparator + user;
	String userInboxAbsName
	    = userRootAbsName + privateNamespaceSeparator + "INBOX";
	SimpleFolderRecord userRootRecord
	    = new SimpleFolderRecord(privateNamespace
				     + privateNamespaceSeparator, user,
				     userRootAbsName);
	SimpleFolderRecord userInboxRecord
	    = new SimpleFolderRecord(privateNamespace
				     + privateNamespaceSeparator + "INBOX",
				     user, userInboxAbsName);

	

	ACLMailbox userRootFolder = new FileMailbox();
	ACLMailbox userInbox = new FileMailbox();
	try{
	    userRootFolder.configure(conf);
	    userRootFolder.contextualize(context);
	    userRootFolder.compose(compMgr);
	    userRootFolder.prepareMailbox(user, userRootAbsName, user);
	    userInbox.configure(conf);
	    userInbox.contextualize(context);
	    userInbox.compose(compMgr);
	    userInbox.prepareMailbox(user, userInboxAbsName, user);
	    userRootFolder.init();
	    userRootFolder.setNotSelectableByAnyone(true);
	    userInbox.init();
	    userInbox.setRights(user, MailServer.MDA, "lrswi");
	} catch (Exception e) {
	    logger.error("Exception creating new account: " + e);
	    return false;
	}
	userInboxRecord.init();
	userInboxRecord.setUidValidity(userInbox.getUIDValidity());
	userInboxRecord.setHighestUid(userInbox.getNextUID() -1);
	userInboxRecord.setLookupRights(userInbox.getUsersWithLookupRights());
	userInboxRecord.setReadRights(userInbox.getUsersWithReadRights());
	userInboxRecord.setNotSelectableByAnyone(userInbox.isNotSelectableByAnyone());
	userRootRecord.init();
	userRootRecord.setLookupRights(userRootFolder.getUsersWithLookupRights());
	userRootRecord.setReadRights(userRootFolder.getUsersWithReadRights());
	userRootRecord.setNotSelectableByAnyone(userRootFolder.isNotSelectableByAnyone());
	recordRep.store(userRootRecord);
	recordRep.store(userInboxRecord);

	//No one is using these mailboxes
	//try {
	//      userRootFolder.destroy();
	//      userInbox.destroy();
	//} catch (Exception e) {
	//    logger.error("Exception closing new account mailbox: " + e);
	//    return false;
	//} 
	userRootFolder = null;
	userInbox = null;

	return true;
    }


}

