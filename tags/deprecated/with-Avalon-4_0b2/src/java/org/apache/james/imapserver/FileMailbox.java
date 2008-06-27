/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.context.Context;
import org.apache.james.AccessControlException;
import org.apache.james.AuthorizationException;
import org.apache.james.Constants;
import org.apache.james.core.EnhancedMimeMessage;
import org.apache.james.services.UsersRepository;
import org.apache.log.LogKit;
import org.apache.log.Logger;
import org.apache.mailet.Mail;

/**
 * Object representing an IMAP4rev1 mailbox (folder) on a local file system.
 * The mailbox contains messages, message attributes and an access control
 * list.
 *
 * <p> from Interface Mailbox
 *
 * <p>Mailbox Related Flags (rfc2060 name attributes)
 * <br>    \Noinferiors   It is not possible for any child levels of hierarchy
 * to exist under this name; no child levels exist now and none can be created
 * in the future.
 * <br>    \Noselect      It is not possible to use this name as a selectable
 * mailbox.
 * <br>    \Marked        The mailbox has been marked "interesting" by the
 * server; the mailbox probably contains messages that have been added since
 * the last time the mailbox was selected.
 * <br>      \Unmarked      The mailbox does not contain any additional
 * messages since the last time the mailbox was selected.
 *
 * <p>Message related flags.
 * <br>The flags allowed per message are specific to each mailbox.
 * <br>The minimum list (rfc2060 system flags) is:
 * <br> \Seen       Message has been read
 * <br> \Answered   Message has been answered
 * <br> \Flagged    Message is "flagged" for urgent/special attention
 * <br> \Deleted    Message is "deleted" for removal by later EXPUNGE
 * <br> \Draft      Message has not completed composition (marked as a draft).
 * <br> \Recent     Message is "recently" arrived in this mailbox.  This
 * session is the first session to have been notified about this message;
 * subsequent sessions will not see \Recent set for this message.  This flag
 * can not be altered by the client.
 *  <br>            If it is not possible to determine whether or not this
 * session is the first session to be notified about a message, then that
 * message SHOULD be considered recent.
 *
 * <p> From interface ACL </p>
 *
 * The standard rights in RFC2086 are:
 * <br>l - lookup (mailbox is visible to LIST/LSUB commands)
 * <br>r - read (SELECT the mailbox, perform CHECK, FETCH, PARTIAL, SEARCH,
 * COPY from mailbox)
 * <br>s - keep seen/unseen information across sessions (STORE SEEN flag)
 * <br>w - write (STORE flags other than SEEN and DELETED)
 * <br>i - insert (perform APPEND, COPY into mailbox)
 * <br>p - post (send mail to submission address for mailbox, not enforced by
 * IMAP4 itself)
 * <br>c - create (CREATE new sub-mailboxes in any implementation-defined
 * hierarchy)
 * <br>d - delete (STORE DELETED flag, perform EXPUNGE)
 * <br>a - administer (perform SETACL)
 *
 * <p> Serialization. Not all fields are serialized. Dispose() must be called to
 * write serialized fiels to disc before finishing with this object.
 *
 * <p> Deserialization. On recover from disc, configure, compose,
 * contextualize and reInit must be called before object is ready for use.
 *
 * Reference: RFC 2060
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.1 on 14 Dec 2000
 */
public class FileMailbox
    implements ACLMailbox, Serializable {

    public static final String MAILBOX_FILE_NAME = "mailbox.mbr";

    private static final String MESSAGE_EXTENSION = ".msg";
    private static final String ATTRIBUTES_EXTENSION = ".att";
    private static final String FLAGS_EXTENSION = ".flags";

    private static final int NUMBER_OF_RIGHTS = 9;
    // Map string identities to boolean[NUMBER_OF_RIGHTS] arrays of rights
    //The rights are, in order: l,r,s,w,i,p,c,d,a
    private static final int LOOKUP = 0;
    private static final int READ = 1;
    private static final int KEEP_SEEN = 2;
    private static final int WRITE = 3;
    private static final int INSERT = 4;
    private static final int POST = 5;
    private static final int CREATE = 6;
    private static final int DELETE = 7;
    private static final int ADMIN = 8;
    private static final boolean[] NO_RIGHTS
        = {false, false, false, false, false, false, false, false, false};
    private static final boolean[] ALL_RIGHTS
        = {true, true, true, true, true, true, true, true, true};
    private static final String DENY_ACCESS = "Access denied by ACL";
    private static final String DENY_AUTH = "Action not authorized for: ";
    private static final String OPTIONAL_RIGHTS = "l r s w i p c d a";
    private static final char[] DELETE_MODS
        = {'-', 'l', 'r', 's', 'w', 'i', 'p', 'c', 'd', 'a'};

    /* Transient fields - reInit must be called on recover from disc. */
    private transient Context context;
    private transient Configuration conf;
    private transient ComponentManager compMgr;
    private transient Logger logger = LogKit.getLoggerFor("james.MailRepository");
    private transient UsersRepository localUsers;
    private transient HashSet listeners;

    /* Serialized fileds */
    private String path; // does not end with File.separator
    private File directory ;
    private String owner;
    private String absoluteName;
    private Map acl;
    private String name;
    private int uidValidity;
    private int highestUID;
    private int mailboxSize; // octets
    private boolean inferiorsAllowed;
    private boolean marked;
    private boolean notSelectableByAnyone;

    // The message sequence number of a msg is its index in List sequence + 1
    private List sequence; //List of UIDs of messages in mailbox

    // Set of UIDs of messages with recent flag set
    private Set recentMessages;

    // Set of UIDs of messages with delete flag set
    private Set messagesForDeletion;

    //map of user String to Integer uid, 0 for no unseen messages
    private Map oldestUnseenMessage;

    public void configure(Configuration conf) throws ConfigurationException {
        this.conf = conf;
    }

    public void contextualize(Context context) {
        this.context = context;
    }

    public void compose(ComponentManager comp) {
        compMgr = comp;
    }

    public void prepareMailbox(String user, String absName,
                               String initialAdminUser) {
        if (user != null && (user.length() > 0)) {
            owner = user;
        } else {
            throw new RuntimeException("Incorrect user argument  for a"
                                       + " FileMailbox constructor.");
        }
        if (absName != null && (absName.length() > 0)) {
            absoluteName = absName;
        } else {
            throw new RuntimeException("Incorrect absoluteName argument for a"
                                       + " FileMailbox constructor.");
        }
        if (initialAdminUser != null && (initialAdminUser.length() > 0)) {
            acl = new HashMap(7);
            acl.put(initialAdminUser, ALL_RIGHTS);
            //acl = new SimpleACL(initialAdminUser);
        } else {
            throw new RuntimeException("Incorrect initialAdminUser argument"
                                       + " for a FileMailbox constructor.");
        }
    }

    public void initialize() throws Exception {
        uidValidity = 1;
        highestUID = 0;
        mailboxSize = 0;
        inferiorsAllowed = true;
        marked = false;
        notSelectableByAnyone = false;
        oldestUnseenMessage = new HashMap();
        listeners = new HashSet();
        sequence = new ArrayList();
        recentMessages = new HashSet();
        messagesForDeletion = new HashSet();
        logger.info("FileMailbox init for " + absoluteName);
        localUsers = (UsersRepository)compMgr.lookup("org.apache.james.services.UsersRepository");
        String rootPath
            = conf.getChild("mailboxRepository").getValue();
        if (!rootPath.endsWith(File.separator)) {
            rootPath = rootPath + File.separator;
        }
        Configuration namespaces = conf.getChild("namespaces");
        String namespaceToken = namespaces.getAttribute("token");
        String privateNamespace
            = namespaces.getChild("privateNamespace").getValue();
        String privateNamespaceSeparator
            = namespaces.getChild("privateNamespace").getAttribute("separator");
        String sharedNamespace
            = namespaces.getChild("sharedNamespace").getValue();
        String sharedNamespaceSeparator
            = namespaces.getChild("sharedNamespace").getAttribute("separator");
        if (absoluteName.startsWith(privateNamespace)) {
            String path1
                = absoluteName.substring(privateNamespace.length()
                                         + privateNamespaceSeparator.length()
                                         + owner.length());
            path = rootPath + owner
                + path1.replace(privateNamespaceSeparator.charAt(0),
                                File.separatorChar);
            name = absoluteName.substring(absoluteName.lastIndexOf(privateNamespaceSeparator) + 1);
            if (name.equals(owner)) {
                name = "";
            }
        } else if (absoluteName.startsWith(sharedNamespace)) {
            String path2 = absoluteName.substring(namespaceToken.length());
            path = rootPath + path2.replace(sharedNamespaceSeparator.charAt(0),
                                            File.separatorChar);
            name = absoluteName.substring(absoluteName.lastIndexOf(sharedNamespaceSeparator) + 1);
            if (name.equals(sharedNamespace)) {
                name = "";
            }
        } else {
            logger.error("FileMailbox init error: unknown namespace - "
                         + absoluteName);
            throw new RuntimeException("Unknown namespace for absoluteName"
                                       +" argument for a FileMailbox"
                                       +" constructor." + absoluteName);
        }
        //Check for writable directory
        File mailboxDir = new File(path);
        if (mailboxDir.exists()) {
            throw new RuntimeException("Error: Attempt to overwrite mailbox directory at " + path);
        } else if (! mailboxDir.mkdir()){
            throw new RuntimeException("Error: Cannot create mailbox directory at " + path);
        } else if (!mailboxDir.canWrite()) {
            throw new RuntimeException("Error: Cannot write to directory at " + path);
        }
        writeMailbox();
        logger.info("FileMailbox init complete: " + absoluteName);
    }

    /**
     * Re-initialises mailbox after reading from file-system. Cannot assume that this is the same instance of James that wrote it.
     *
     * <p> Contract is that re-init must be called after configure, contextualize, compose.
     */
    public void reinitialize() throws Exception {
        listeners = new HashSet();
        logger = LogKit.getLoggerFor("james.MailRepository");
        logger.info("FileMailbox reInit for " + absoluteName);
        localUsers = (UsersRepository)compMgr.lookup("org.apache.james.services.UsersRepository");
        String rootPath
            = conf.getChild("mailboxRepository").getValue();
        if (!rootPath.endsWith(File.separator)) {
            rootPath = rootPath + File.separator;
        }
    }

    /**
     * Call when host has finished with a mailbox.
     * This is really a stop rather than destroy.
     * Writes current mailbox object to disc.
     */
    public void dispose() {
        writeMailbox();
        logger.info("FileMailbox object destroyed: " + absoluteName);
    }

    /**
     * Returns true once this Mailbox has been checkpointed.
     * This implementation just writes its mailbox record to disc.  Unless something is
     * broken all messages added, amended or removed from this mailbox will have been
     * handled by this object.
     * <br> This implementation always returns true.
     *
     * @returns true
     */
    public synchronized  boolean checkpoint() {
        writeMailbox();
        logger.info("FileMailbox: " + absoluteName + " checkpointed.");
        return true;
    }

    /**
     * Remove \Recent flag from all messages in mailbox. Should be called
     * whenever a user session finishes.
     */
    public synchronized void unsetRecent() {
        Iterator it = recentMessages.iterator();
        while(it.hasNext()) {
            Integer uidObj =(Integer)it.next();
            int uid = uidObj.intValue();
            Flags flags = readFlags(uid);
            if (flags != null) {
                flags.setRecent(false);
                writeFlags(uid, flags);
            }
        }
        recentMessages.clear();
    }


    // Methods that don't involve the ACL ------------------

    /**
     * Returns name of this mailbox relative to its parent in the mailbox
     * hierarchy.
     * Example: 'NewIdeas'
     *
     * @returns String name of mailbox relative to its immeadiate parent in
     * the mailbox hierarchy.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns absolute, that is user-independent, hierarchical name of
     * mailbox (including namespace)
     * Example: '#mail.fred.flintstone.apache.James.NewIdeas'
     *
     * @returns String name of mailbox in absolute form
     */
    public String getAbsoluteName() {
        return absoluteName;
    }

    /** Returns namespace starting with namespace token.
     * Example: '#mail'
     *
     * @returns String containing user-independent namespace of this mailbox.
     */
    //  public String getNamespace();

    /** Returns true if the argument is the relative or absolute name of
     * this mailbox
     *
     * @param name possible name for this Mailbox
     * @returns true if name matches either getName() or getAbsoluteName()
     */
    public boolean matchesName(String testName) {
        return (name == testName || name == absoluteName);
    }

    /**
     * Returns the current unique id validity value of this mailbox.
     *
     * @returns int current 32 bit unique id validity value of this mailbox
     */
    public int getUIDValidity() {
        return uidValidity;
    }

    /**
     * Returns the 32 bit uid available for the next message.
     *
     * @returns int the next UID that would be used.
     */
    public int getNextUID() {
        return highestUID + 1;
    }

    /**
     * Returns mailbox size in octets. Should only include actual messages
     * and not any implementation-specific data, such as message attributes.
     *
     * @returns int mailbox size in octets
     */
    public synchronized int getMailboxSize() {
        return mailboxSize;
    }

    /**
     * Indicates if child folders may be created. It does not indicate which
     * users can create child folders.
     *
     * @returns boolean TRUE if inferiors aree allowed
     */
    public boolean getInferiorsAllowed() {
        return inferiorsAllowed;
    }

    /**
     * Indicates that messages have been added since this mailbox was last
     * selected by any user.
     *
     * @returns boolean TRUE if new messages since any user last selected
     * mailbox
     */
    public  synchronized  boolean isMarked() {
        return marked;
    }

    /**
     * Returns all flags supported by this mailbox.
     * e.g. \Answered \Deleted
     *
     * @returns String a space seperated list of message flags which are
     * supported by this mailbox.
     */
    public String getSupportedFlags() {
        return SYSTEM_FLAGS;
    }
    /**
     * Indicates no of messages with \Recent flag set
     *
     * @returns int no of messages with \Recent flag set
     */
    public  synchronized  int getRecent() {
        return recentMessages.size();
    }

    /**
     * Indicates the oldest unseen message for the specified user.
     *
     * @returns int Message Sequence Number of first message without \Seen
     * flag set for this User.  0 means no unseen messages in this mailbox.
     */
    public  synchronized  int getOldestUnseen(String user) {
        int response = 0;
        if (oldestUnseenMessage.containsKey(user)) {
            Integer uidObj = ((Integer)oldestUnseenMessage.get(user));
            if (! (uidObj.intValue() == 0)) {
                response = sequence.indexOf(uidObj);
            }
        } else {
            if (sequence.size() > 0) {
                response = 1;
                oldestUnseenMessage.put(user, (Integer)sequence.get(0));
            } else {
                oldestUnseenMessage.put(user, (new Integer(0)));
            }
        }
        return response;
    }

    /**
     * Indicates number of messages in folder
     *
     * @returns int number of messages
     */
    public  synchronized  int getExists() {
        return sequence.size();
    }

    /**
     * Indicates the number of  unseen messages for the specified user.
     *
     * @returns int number of messages without \Seen flag set for this User.
     */
    public int getUnseen(String user) {
        if (oldestUnseenMessage.containsKey(user)) {
            int response = 0; //indicates no unseen messages
            Integer uidObj = ((Integer)oldestUnseenMessage.get(user));
            int oldUID = uidObj.intValue();
            if (oldUID != 0) {
                ListIterator lit
                    = sequence.listIterator(sequence.indexOf(uidObj));
                while (lit.hasNext() ) {
                    int uid = ((Integer)lit.next()).intValue();
                    Flags flags = readFlags(uid);
                    if (!flags.isSeen(user)) {
                        response ++;
                    }
                }
            }
            return response;
        } else { // user has never selected mailbox
            return sequence.size();
        }
    }

    /** Mailbox Events are used to inform registered listeners of events in the Mailbox.
     * E.g. if mail is delivered to an Inbox or if another user appends/ deletes a message.
     */
    public synchronized void addMailboxEventListener(MailboxEventListener mel) {
        listeners.add(mel);
    }


    public synchronized void removeMailboxEventListener(MailboxEventListener mel) {
        listeners.remove(mel);
    }

    /**
     * Mark this mailbox as not selectable by anyone.
     * Example folders at the roots of hierarchies, e. #mail for each user.
     *
     * @param state true if folder is not selectable by anyone
     */
    public void setNotSelectableByAnyone(boolean state) {
        notSelectableByAnyone = state;
    }

    public boolean isNotSelectableByAnyone() {
        return notSelectableByAnyone;
    }

    // Methods for the embedded ACL ------------------------

    /**
     * Store access rights for a given identity.
     * The setter is the user setting the rights, the identifier is the user
     * whose rights are affected.
     * The setter and identifier arguments must be non-null and non-empty.
     * The modification argument must be non-null and follow the syntax of the
     * third argument to a SETACL command.
     * If the modification argument is an empty string, that identifier is
     * removed from the ACL, if currently present.
     *
     * @param setter String representing user attempting to set rights, must
     * be non-null and non-empty
     * @param identity String representing user whose rights are being set,
     * must be non-null and non-empty
     * @param modification String representing the change in rights, following
     * the syntax specified in rfc 2086. Note a blank string means delete all
     * rights for given identity.
     * @returns true if requested modification succeeded. A return value of
     * false means an error other than an AccessControlException or
     * AuthorizationException.
     * @throws AccessControlException if setter does not have lookup rights for
     * this mailbox (ie they should not know this mailbox exists).
     * @throws AuthorizationException if specified setter does not have the
     * administer right (ie the right to write ACL rights), or if the result
     * of this method would leave no identities with admin rights.
     */
    public boolean setRights(String setter, String identifier,
                             String modification)
        throws AccessControlException, AuthorizationException {

        boolean[] settersRights = (boolean[]) acl.get(setter);
        if (settersRights == null
            || (settersRights[LOOKUP] == false)) {
            throw new AccessControlException(DENY_ACCESS);
        } else if (settersRights[ADMIN] == false) {
            throw new AuthorizationException(DENY_AUTH + setter);
        }
        boolean[] existingRights = (boolean[]) acl.get(identifier);
        char[] mods = modification.toCharArray();
        if (mods.length == 0) { // means delete all
            mods = DELETE_MODS;
        }
        if(existingRights == null) {
            if ( mods[0] == REMOVE_RIGHTS ) {
                return false;
            } else {
                existingRights = new boolean[NUMBER_OF_RIGHTS];
                System.arraycopy(NO_RIGHTS, 0, existingRights, 0,
                                 NUMBER_OF_RIGHTS);
            }
        }

        boolean change;
        boolean[] rights = new boolean[NUMBER_OF_RIGHTS];

        if (mods[0] == ADD_RIGHTS) {
            change = true;
            System.arraycopy(existingRights, 0, rights, 0,
                             NUMBER_OF_RIGHTS);
        } else if (mods[0] == REMOVE_RIGHTS) {
            change = false;
            System.arraycopy(existingRights, 0, rights, 0,
                             NUMBER_OF_RIGHTS);
        } else {                                             // means replace
            System.arraycopy(NO_RIGHTS, 0, rights, 0,
                             NUMBER_OF_RIGHTS);
            char[] new_mods = new char[mods.length + 1];
            System.arraycopy(mods, 0, new_mods, 1, mods.length);
            mods = new_mods;
            change = true;
        }

        for (int i=1; i <mods.length; i++) {
            switch(mods[i]) {
            case LOOKUP_RIGHTS: rights[LOOKUP] = change;
                break;
            case READ_RIGHTS: rights[READ] = change;
                break;
            case KEEP_SEEN_RIGHTS: rights[KEEP_SEEN] = change;
                break;
            case WRITE_RIGHTS: rights[WRITE] = change;
                break;
            case INSERT_RIGHTS: rights[INSERT] = change;
                break;
            case POST_RIGHTS: rights[POST] = change;
                break;
            case CREATE_RIGHTS: rights[CREATE] = change;
                break;
            case DELETE_RIGHTS: rights[DELETE] = change;
                break;
            case ADMIN_RIGHTS: rights[ADMIN] = change;
                break;
            default: return false;
            }
        }

        //  All rights above lookup require lookup
        if(rights[LOOKUP] == false  &&  !Arrays.equals(rights, NO_RIGHTS)) {
            return false;
        }
        // Each right requires all the rights before it.
        int count = 0;
        for (int i=1; i< NUMBER_OF_RIGHTS; i++) {
            if(rights[i-1] ^ rights[i]) {
                count++;
            }
        }
        switch (count) {
        case 0:                              // now Admin or deleted
            if (rights[ADMIN]) {
                acl.put(identifier, rights);
                break;
            } else {
                if (otherAdmin(identifier)) {
                    acl.remove(identifier);
                    break;
                } else {
                    return false;
                }
            }
        case 2:              // not allowed
            return false;
        case 1:             // not Admin, check there remains an Admin
            // Iterator namesIt = acl.keySet().iterator();
            //boolean otherAdmin = false;
            //while(namesIt.hasNext() && !otherAdmin) {
            //String name = (String)namesIt.next();
            //if (name != identifier) {
            //    boolean[] otherRights = (boolean[]) acl.get(name);
            //        otherAdmin = otherRights[ADMIN];
            //}
            //}
            if (otherAdmin(identifier)) {
                acl.put(identifier, rights);
                break;
            } else {
                return false;
            }
        default:             // not allowed
            return false;
        }
        writeMailbox();
        return true;
    }

    /**
     * Check there is a person other than identifier who has Admin rights.
     */
    private boolean otherAdmin(String identifier) {
        Iterator namesIt = acl.keySet().iterator();
        boolean result = false;
        while(namesIt.hasNext() && !result) {
            String name = (String)namesIt.next();
            if (!name.equals(identifier)) {
                boolean[] otherRights = (boolean[]) acl.get(name);
                result = otherRights[ADMIN];
            }
        }
        return result;
    }

    /**
     * Retrieve access rights for a specific identity.
     *
     * @param getter String representing user attempting to get the rights,
     * must be non-null and non-empty
     * @param identity String representing user whose rights are being got,
     * must be non-null and non-empty
     * @returns String of rights usingrfc2086 syntax, empty if identity has no
     * rights in this mailbox.
     * @throws AccessControlException if getter does not have lookup rights for
     * this mailbox (ie they should not know this mailbox exists).
     * @throws AuthorizationException if implementation does not wish to expose
     * ACL for this identity to this getter.
     */
    public String getRights(String getter, String identity)
        throws AccessControlException, AuthorizationException {
        boolean[] gettersRights = (boolean[])  acl.get(getter);
        if (gettersRights == null
            || (gettersRights[LOOKUP] == false)) {
            throw new AccessControlException(DENY_ACCESS);
        } else if (!getter.equals(identity) && gettersRights[ADMIN] == false) {
            throw new AuthorizationException(DENY_AUTH + getter);
        }
        boolean[] rights = (boolean[]) acl.get(identity);
        if (rights == null) {
            return null;
        } else {
            StringBuffer buf = new StringBuffer(NUMBER_OF_RIGHTS);
            for (int i = 0; i<NUMBER_OF_RIGHTS; i++) {
                if (rights[i]) {
                    buf.append(RIGHTS[i]);
                }
            }
            return buf.toString();
        }
    }

    /**
     * Retrieves a String of one or more <identity space rights> who have
     * rights in this ACL
     *
     * @param getter String representing user attempting to get the rights,
     * must be non-null and non-empty
     * @returns String of rights sets usingrfc2086 syntax
     * @throws AccessControlException if getter does not have lookup rights for
     * this mailbox (ie they should not know this mailbox exists).
     * @throws AuthorizationException if implementation does not wish to expose
     * ACL to this getter.
     */
    public String getAllRights(String getter)
        throws AccessControlException, AuthorizationException {
        boolean[] gettersRights = (boolean[]) acl.get(getter);
        if (gettersRights == null
            || (gettersRights[LOOKUP] == false)) {
            throw new AccessControlException(DENY_ACCESS);
        } else if ( gettersRights[ADMIN] == false) {
            throw new AuthorizationException(DENY_AUTH + getter);
        }
        Iterator namesIt = acl.keySet().iterator();
        StringBuffer response = new StringBuffer(20*acl.size());
        while(namesIt.hasNext()) {
            String name = (String)namesIt.next();
            response.append("<" + name + " ");
            boolean[] rights = (boolean[]) acl.get(name);
            for (int i = 0; i<NUMBER_OF_RIGHTS; i++) {
                if (rights[i]) {
                    response.append(RIGHTS[i]);
                }
            }
            response.append("> ");
        }

        return response.toString();
    }

    /**
     * Retrieve rights which will always be granted to the specified identity.
     *
     * @param getter String representing user attempting to get the rights,
     * must be non-null and non-empty
     * @param identity String representing user whose rights are being got,
     * must be non-null and non-empty
     * @returns String of rights usingrfc2086 syntax, empty if identity has no
     * guaranteed rights in this mailbox.
     * @throws AccessControlException if getter does not have lookup rights for
     * this mailbox (ie they should not know this mailbox exists).
     * @throws AuthorizationException if implementation does not wish to expose
     * ACL for this identity to this getter.
     */
    public String getRequiredRights(String getter, String identity)
        throws AccessControlException, AuthorizationException {
        boolean[] gettersRights = (boolean[]) acl.get(getter);
        if (gettersRights == null
            || (gettersRights[LOOKUP] == false)) {
            throw new AccessControlException(DENY_ACCESS);
        } else if (!getter.equals(identity) && gettersRights[ADMIN] == false) {
            throw new AuthorizationException(DENY_AUTH + getter);
        }

        return "\"\"";
    }

    /**
     * Retrieve rights which may be granted to the specified identity.
     * @param getter String representing user attempting to get the rights,
     * must be non-null and non-empty
     * @param identity String representing user whose rights are being got,
     * must be non-null and non-empty
     * @returns String of rights usingrfc2086 syntax, empty if identity has no
     * guaranteed rights in this mailbox.
     * @throws AccessControlException if getter does not have lookup rights for
     * this mailbox (ie they should not know this mailbox exists).
     * @throws AuthorizationException if implementation does not wish to expose
     * ACL for this identity to this getter.
     */
    public String getOptionalRights(String getter, String identity)
        throws AccessControlException, AuthorizationException {
        boolean[] gettersRights = (boolean[]) acl.get(getter);
        if (gettersRights == null
            || (gettersRights[LOOKUP] == false)) {
            throw new AccessControlException(DENY_ACCESS);
        } else if (!getter.equals(identity) && gettersRights[ADMIN] == false) {
            throw new AuthorizationException(DENY_AUTH + getter);
        }

        return OPTIONAL_RIGHTS;
    }

    /**
     * Helper boolean methods.
     * Provided for cases where you need to check the ACL before selecting the
     * mailbox.
     *
     * @param username String representing user
     * @returns true if user has the requested right.
     * &throws AccessControlException if username does not have lookup rights.
     * (Except for hasLookupRights which just returns false.
     */
    public boolean hasLookupRights(String username) {
        boolean[] usersRights = (boolean[]) acl.get(username);
        return (( usersRights == null || (usersRights[LOOKUP] == false))
                ? false : true);
    }

    public boolean hasReadRights(String username)
        throws AccessControlException {
        boolean[] usersRights = (boolean[]) acl.get(username);
        if (usersRights == null  || (usersRights[LOOKUP] == false)) {
            throw new AccessControlException(DENY_ACCESS);
        }
        return usersRights[READ];
    }

    public boolean hasKeepSeenRights(String username)
        throws AccessControlException {
        boolean[] usersRights = (boolean[]) acl.get(username);
        if (usersRights == null  || (usersRights[LOOKUP] == false)) {
            throw new AccessControlException(DENY_ACCESS);
        }
        return usersRights[KEEP_SEEN];
    }

    public boolean hasWriteRights(String username)
        throws AccessControlException {
        boolean[] usersRights = (boolean[]) acl.get(username);
        if (usersRights == null  || (usersRights[LOOKUP] == false)) {
            throw new AccessControlException(DENY_ACCESS);
        }
        return usersRights[WRITE];
    }

    public boolean hasInsertRights(String username)
        throws AccessControlException {
        boolean[] usersRights = (boolean[]) acl.get(username);
        if (usersRights == null  || (usersRights[LOOKUP] == false)) {
            throw new AccessControlException(DENY_ACCESS);
        }
        return usersRights[INSERT];
    }

    public boolean hasCreateRights(String username)
        throws AccessControlException {
        boolean[] usersRights = (boolean[]) acl.get(username);
        if (usersRights == null  || (usersRights[LOOKUP] == false)) {
            throw new AccessControlException(DENY_ACCESS);
        }
        return usersRights[CREATE];
    }

    public boolean hasDeleteRights(String username)
        throws AccessControlException {
        boolean[] usersRights = (boolean[]) acl.get(username);
        if (usersRights == null  || (usersRights[LOOKUP] == false)) {
            throw new AccessControlException(DENY_ACCESS);
        }
        return usersRights[DELETE];
    }

    public boolean hasAdminRights(String username)
        throws AccessControlException {
        boolean[] usersRights = (boolean[]) acl.get(username);
        if (usersRights == null  || (usersRights[LOOKUP] == false)) {
            throw new AccessControlException(DENY_ACCESS);
        }
        return usersRights[ADMIN];
    }

    // Mailbox methods using the ACL ---------------------------

    /**
     * Indicates if this folder may be selected by the specified user. Requires
     * user to have at least read rights. It does not indicate whether user
     * can write to mailbox
     *
     * @param username String represnting user
     * @returns boolean TRUE if specified user can Select mailbox.
     * @throws AccessControlException if username does not have lookup rights
     */
    public  synchronized  boolean isSelectable(String username)
        throws AccessControlException {
        return (!notSelectableByAnyone && hasReadRights(username));
    }

    /**
     * Indicates if specified user can change any flag on a permanent basis,
     * except for \Recent which can never be changed by a user.
     *
     * @param username String represnting user
     * @returns true if specified user can change all flags permanently.
     */
    public synchronized boolean allFlags(String username)
        throws AccessControlException {
        // relies on implementation that each right implies those
        // before it in list:  l,r,s,w,i,p,c,d,a
        return hasDeleteRights(username);
    }

    /**
     * Indicates which flags this user can change permanently. If allFlags()
     * returns true for this user, then this method must have the same return
     * value as getSupportedFlags.
     *
     * @param username String represnting user
     * @returns String a space seperated list of message flags which this user
     * can set permanently
     */
    public  synchronized  String getPermanentFlags(String username)
        throws AccessControlException {
        if (hasDeleteRights(username)) {
            return SYSTEM_FLAGS;
        } else if (hasWriteRights(username)) {
            return "\\Seen \\Answered \\Flagged \\Draft";
        } else if (hasKeepSeenRights(username)) {
            return "\\Seen";
        } else {
            return "";
        }
    }

    /**
     * Provides a reference to the access control list for this mailbox.
     *
     * @returns the AccessControlList for this Mailbox
     */
    //   public ACL getACL();

    /**
     * Indicates state in which  the mailbox will be opened by specified user.
     * A return value of true indicates Read Only, false indicates Read-Write
     * and an AccessControlException is thrown if user does not have read
     * rights.
     * <p>Implementations decide if Read Only means only lookup and read
     * rights (lr) or lookup, read and keep seen rights (lrs). This may even
     * vary between mailboxes.
     *
     * @param username String represnting user
     * @returns true if specified user can only open the mailbox Read-Only.
     * @throws AccessControlException if the user can not open this mailbox
     * at least Read-Only.
     */
    public synchronized boolean isReadOnly(String username)
        throws AccessControlException {
        return (! hasWriteRights(username));
    }

    // Message handling methods ---------------------------

    /**
     * Stores a message in this mailbox. User must have insert rights.
     *
     * @param message the MimeMessage to be stored
     * @param username String represnting user
     * @returns boolean true if successful
     * @throws AccessControlException if username does not have lookup rights
     * for this mailbox.
     * @throws AuthorizationException if username has lookup rights but does
     * not have insert rights.
     */
    public synchronized boolean store(MimeMessage message, String username)
        throws AccessControlException, AuthorizationException,
               IllegalArgumentException {

        if (message == null || username == null) {
            logger.error("Null argument received in store.");
            throw new IllegalArgumentException("Null argument received in store.");
        }
        if (!hasInsertRights(username)) { //throws AccessControlException
            throw new AuthorizationException("Not authorized to insert.");
        }

        SimpleMessageAttributes attrs = new SimpleMessageAttributes();
        try {
            attrs.setAttributesFor(message);
        } catch (javax.mail.MessagingException me) {
            throw new RuntimeException("Exception creating SimpleMessageAttributes: " + me);
        }
        Flags flags = new Flags();
        flags.initialize();
        return store(message, username, attrs, flags);
    }

    /**
     * Stores a message in this mailbox, using passed MessageAttributes and
     * Flags. User must have insert rights.
     * <br>Current implementation requires MessageAttributs to be of
     * class SimpleMessageAttributes
     *
     * @param mail the message to be stored
     * @param username String represnting user
     * @param msgAttrs non-null MessageAttributes for use with this Message
     * @returns boolean true if successful
     * @throws AccessControlException if username does not have lookup
     * rights for this mailbox.
     * @throws AuthorizationException if username has lookup rights but does
     * not have insert rights.
     */
    public boolean store(MimeMessage message, String username,
                         MessageAttributes msgAttrs, Flags flags)
        throws AccessControlException, AuthorizationException,
               IllegalArgumentException {

        if (msgAttrs == null || message == null || username == null) {
            logger.error("Null argument received in store.");
            throw new IllegalArgumentException("Null argument received in store.");
        }
        if (! (msgAttrs instanceof SimpleMessageAttributes)) {
            logger.error("Wrong class for Attributes");
            throw new IllegalArgumentException("Wrong class for Attributes");
        }
        SimpleMessageAttributes attrs = (SimpleMessageAttributes)msgAttrs;

        int newUID = ++highestUID;
        attrs.setUID(newUID);
        sequence.add(new Integer(newUID));
        attrs.setMessageSequenceNumber(sequence.size());

        BufferedOutputStream outMsg = null;
        ObjectOutputStream outAttrs = null;

        try {
            outMsg = new BufferedOutputStream( new FileOutputStream(path + File.separator + newUID + MESSAGE_EXTENSION));
            message.writeTo(outMsg);
            outMsg.close();
            outAttrs = new ObjectOutputStream( new FileOutputStream(path + File.separator + newUID + ATTRIBUTES_EXTENSION));
            outAttrs.writeObject(attrs);
            outAttrs.close();
        } catch(Exception e) {
            logger.error("Error writing message to disc: " + e);
            e.printStackTrace();
            throw new
                RuntimeException("Exception caught while storing Mail: "
                                 + e);
        } finally {
            try {
                outMsg.close();
                outAttrs.close();
            } catch (IOException ie) {
                logger.error("Error closing streams: " + ie);
            }
        }
        marked = true;
        if (flags.isRecent()) {
            recentMessages.add(new Integer(newUID));
        }
        if (flags.isDeleted()) {
            messagesForDeletion.add(new Integer(newUID));
        }
        //if (!flags.isSeen(username)) {
        //If a user had no unseen messages, they do, now.
        Iterator it = oldestUnseenMessage.keySet().iterator();
        while (it.hasNext()) {
            String user = (String)it.next();
            if ( ((Integer)oldestUnseenMessage.get(user)).intValue() == -1) {
                oldestUnseenMessage.put(user, new Integer(newUID));
            }
        }
        //}
        writeFlags(newUID, flags);
        logger.info("Mail " + newUID + " written in " + absoluteName);

        return true;
    }

    /**
     * Retrieves a message given a message sequence number.
     *
     * @param msn the message sequence number
     * @param username String represnting user
     * @returns an  EnhancedMimeMessage object containing the message, null if no message with
     * the given msn.
     * @throws AccessControlException if user does not have read rights for
     * this mailbox.
     * @throws AuthorizationException if user has lookup rights but does not
     * have read rights.
     */
    public synchronized EnhancedMimeMessage retrieve(int msn, String user)
        throws AccessControlException, AuthorizationException {
        if (!hasReadRights(user)) { //throws AccessControlException
            throw new AuthorizationException("Not authorized to read.");
        }

        if (msn > sequence.size()) {
            return null;
        } else {
            int uid = ((Integer)sequence.get(msn - 1)).intValue();
            return retrieveUID(uid, user);
        }
    }


    /**
     * Retrieves a message given a unique identifier.
     *
     * @param uid the unique identifier of a message
     * @param username String represnting user
     * @returns an EnhancedMimeMessage object containing the message, null if no message with
     * the given msn.
     * @throws AccessControlException if user does not have read rights for
     * this mailbox.
     * @throws AuthorizationException if user has lookup rights but does not
     * have read rights.
     */
    public synchronized EnhancedMimeMessage retrieveUID(int uid, String user)
        throws AccessControlException, AuthorizationException {
        if (!hasReadRights(user)) { //throws AccessControlException
            throw new AuthorizationException("Not authorized to read.");
        }
        EnhancedMimeMessage response = null;
        if (sequence.contains(new Integer(uid))) {
            BufferedInputStream inMsg = null;
            try {
                inMsg = new BufferedInputStream( new FileInputStream(path + File.separator + uid + MESSAGE_EXTENSION));
                response = new EnhancedMimeMessage(Session.getDefaultInstance(System.getProperties(), null),inMsg);
                inMsg.close();
            } catch(Exception e) {
                logger.error("Error reading message from disc: " + e);
                e.printStackTrace();
                throw new
                    RuntimeException("Exception caught while retrieving Mail: "
                                     + e);
            } finally {
                try {
                    inMsg.close();
                } catch (IOException ie) {
                    logger.error("Error closing streams: " + ie);
                }
            }
            logger.info("EnhancedMimeMessage " + uid + " read from " + absoluteName);
            return response;
        } else {
            return null;
        }
    }

    /**
     * Marks a message for deletion given a message sequence number.
     *
     * @param msn the message sequence number
     * @param username String represnting user
     * @returns boolean true if successful.
     * @throws AccessControlException if user does not have read rights for
     * this mailbox.
     * @throws AuthorizationException if user has lookup rights but does not
     * have delete rights.
     */
    public synchronized boolean markDeleted(int msn, String user)
        throws AccessControlException, AuthorizationException {
        if (!hasDeleteRights(user)) { //throws AccessControlException
            throw new AuthorizationException("Not authorized to delete.");
        }

        //TBD
        return false;
    }

    /**
     * Marks a message for deletion given a unique identifier.
     *
     * @param uidunique identifier
     * @param username String represnting user
     * @returns boolean true if successful, false if failed including no
     * message with the given uid.
     * @throws AccessControlException if user does not have read rights for
     * this mailbox.
     * @throws AuthorizationException if user has lookup rights but does not
     * have delete rights.
     */
    public synchronized boolean markDeletedUID(int uid, String user)
        throws AccessControlException, AuthorizationException {
        if (!hasDeleteRights(user)) { //throws AccessControlException
            throw new AuthorizationException("Not authorized to delete.");
        }

        //TBD
        return false;
    }

    /**
     * Returns the message attributes for a message.
     *
     * @param msn message sequence number
     * @param username String represnting user
     * @returns MessageAttributes for message, null if no such message.
     * Changing the MessageAttributes object must not affect the actual
     * MessageAttributes.
     * @throws AccessControlException if user does not have read rights for
     * this mailbox.
     * @throws AuthorizationException if user has lookup rights but does not
     * have delete rights.
     */
    public synchronized MessageAttributes getMessageAttributes(int msn, String user)
        throws AccessControlException, AuthorizationException {
        if (!hasReadRights(user)) { //throws AccessControlException
            throw new AuthorizationException("Not authorized to read.");
        }
        if (msn > sequence.size()) {
            return null;
        } else {
            int uid = ((Integer)sequence.get(msn - 1)).intValue();
            return getMessageAttributesUID(uid, user);
        }
    }

    /**
     * Returns the message attributes for a message.
     *
     * @param uid unique identifier
     * @param username String represnting user
     * @returns MessageAttributes for message, null if no such message.
     * Changing the MessageAttributes object must not affect the actual
     * MessageAttributes.
     * @throws AccessControlException if user does not have read rights for
     * this mailbox.
     * @throws AuthorizationException if user has lookup rights but does not
     * have delete rights.
     */
    public synchronized MessageAttributes getMessageAttributesUID(int uid, String user)
        throws AccessControlException, AuthorizationException {
        if (!hasReadRights(user)) { //throws AccessControlException
            throw new AuthorizationException("Not authorized to read.");
        }
        SimpleMessageAttributes response = null;
        if (sequence.contains(new Integer(uid))) {
            ObjectInputStream inAttrs = null;
            try {
                inAttrs = new ObjectInputStream( new FileInputStream(path + File.separator + uid + ATTRIBUTES_EXTENSION));
                response = (SimpleMessageAttributes)inAttrs.readObject();
                response.reinitialize();
            } catch(Exception e) {
                logger.error("Error reading attributes from disc: " + e);
                e.printStackTrace();
                throw new
                    RuntimeException("Exception caught while retrieving Message attributes: "
                                     + e);
            } finally {
                try {
                    inAttrs.close();
                } catch (IOException ie) {
                    logger.error("Error closing streams: " + ie);
                }
            }
            logger.info("MessageAttributes for " + uid + " read from " + absoluteName);
            return response;
        } else {
            return null;
        }
    }

    /**
     * Updates the attributes of a message.This may be incorporated into setFlags().
     *
     * @param MessageAttributes of a message already in this Mailbox
     * @throws AccessControlException if user does not have read rights for
     * this mailbox.
     * @throws AuthorizationException if user has lookup rights but does not
     * have delete rights.
     */
    public boolean updateMessageAttributes(MessageAttributes attrs, String user)
        throws AccessControlException, AuthorizationException {
        if (!hasKeepSeenRights(user)) { //throws AccessControlException
            throw new AuthorizationException("Not authorized to store flags.");
        }
        int uid = attrs.getUID();
        if (sequence.contains(new Integer(uid))) {

            // Really, we should check whether the exact change is authorized.
            ObjectOutputStream outAttrs = null;
            try {
                outAttrs = new ObjectOutputStream( new FileOutputStream(path + File.separator + uid + ATTRIBUTES_EXTENSION));
                outAttrs.writeObject(attrs);
                outAttrs.close();
            } catch(Exception e) {
                logger.error("Error writing message to disc: " + e);
                e.printStackTrace();
                throw new
                    RuntimeException("Exception caught while storing Attributes: "
                                     + e);
            } finally {
                try {
                    outAttrs.close();
                } catch (IOException ie) {
                    logger.error("Error closing streams: " + ie);
                }
            }
            logger.info("MessageAttributes for " + uid + " written in " + absoluteName);

            return true;
        } else {
            return false;
        }
    }

    /**
     * Get the IMAP-formatted String of flags for specified message.
     *
     * @param msn message sequence number for a message in this mailbox
     * @param username String represnting user
     * @returns flags for this message and user, null if no such message.
     * @throws AccessControlException if user does not have lookup rights for
     * this mailbox.
     * @throws AuthorizationException if user has lookup rights but does not
     * have read rights.
     */
    public synchronized  String getFlags(int msn, String user)
        throws AccessControlException, AuthorizationException {
        if (!hasReadRights(user)) { //throws AccessControlException
            throw new AuthorizationException("Not authorized to read.");
        }
        if (msn > sequence.size()) {
            return null;
        } else {
            int uid = ((Integer)sequence.get(msn - 1)).intValue();
            return getFlagsUID(uid, user);
        }
    }

    /**
     * Get the IMAP-formatted String of flags for specified message.
     *
     * @param uid UniqueIdentifier for a message in this mailbox
     * @param username String represnting user
     * @returns flags for this message and user, null if no such message.
     * @throws AccessControlException if user does not have lookup rights for
     * this mailbox.
     * @throws AuthorizationException if user has lookup rights but does not
     * have read rights.
     */
    public synchronized  String getFlagsUID(int uid, String user)
        throws AccessControlException, AuthorizationException {
        if (!hasReadRights(user)) { //throws AccessControlException
            throw new AuthorizationException("Not authorized to read.");
        }
        if (!sequence.contains(new Integer(uid))) {
            return null;
        } else {
            Flags flags = readFlags(uid);
            return flags.getFlags(user);
        }
    }
    /**
     * Updates the flags for a message.
     *
     * @param msn MessageSequenceNumber of a message already in this Mailbox
     * @param username String represnting user
     * @param request IMAP-formatted String representing requested change to
     * flags.
     * @returns true if succeeded, false otherwise, including no such message
     * @throws AccessControlException if user does not have read rights for
     * this mailbox.
     * @throws AuthorizationException if user has lookup rights but does not
     * have delete rights.
     */
    public synchronized  boolean setFlags(int msn, String user, String request)
        throws AccessControlException, AuthorizationException,
               IllegalArgumentException {
        if (!hasKeepSeenRights(user)) { //throws AccessControlException
            throw new AuthorizationException("Not authorized to store any flags.");
        }
        if (msn > sequence.size()) {
            return false;
        } else {
            int uid = ((Integer)sequence.get(msn - 1)).intValue();
            return setFlagsUID(uid, user, request);
        }
    }

    /**
     * Updates the flags for a message.
     *
     * @param uid Unique Identifier of a message already in this Mailbox
     * @param username String represnting user
     * @param request IMAP-formatted String representing requested change to
     * flags.
     * @throws AccessControlException if user does not have read rights for
     * this mailbox.
     * @throws AuthorizationException if user has lookup rights but does not
     * have delete rights.
     */
    public synchronized boolean setFlagsUID(int uid, String user, String request)
        throws AccessControlException, AuthorizationException,
               IllegalArgumentException {
        if (!hasKeepSeenRights(user)) { //throws AccessControlException
            throw new AuthorizationException("Not authorized to store any flags.");
        }
        if ((request.toUpperCase().indexOf("DELETED") != -1) && (!hasDeleteRights(user))) { //throws AccessControlException
            throw new AuthorizationException("Not authorized to delete.");
        }
        if (sequence.contains(new Integer(uid))) {

            Flags flags = readFlags(uid);
            boolean wasRecent = flags.isRecent();
            boolean wasDeleted = flags.isDeleted();
            boolean wasSeen = flags.isSeen(user);

            if  (flags.setFlags(request, user)) {

                if (flags.isDeleted()) {
                    if (! wasDeleted) { messagesForDeletion.add(new Integer(uid)); }
                }
                if (flags.isSeen(user) != wasSeen) {
                    if (flags.isSeen(user)) {
                        int previousOld = ((Integer)oldestUnseenMessage.get(user)).intValue();
                        if (uid == previousOld) {
                            int newOld = findOldestUnseen(user, previousOld);
                            oldestUnseenMessage.put(user, (new Integer(newOld)));
                        }
                    } else { // seen flag unset
                        if (uid < ((Integer)oldestUnseenMessage.get(user)).intValue()) {
                            oldestUnseenMessage.put(user, (new Integer(uid)));
                        }
                    }
                }

                writeFlags(uid, flags);
                logger.debug("Flags for message uid " + uid + " in " + absoluteName + " updated.");
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }

    }

    private int findOldestUnseen(String user, int previousOld)
        throws AccessControlException, AuthorizationException {
        int response = 0; //indicates no unseen messages
        ListIterator lit = sequence.listIterator(previousOld);
        boolean found = false;
        while (!found && lit.hasNext() ) {
            int uid = ((Integer)lit.next()).intValue();
            Flags flags = readFlags(uid);
            if (!flags.isSeen(user)) {
                response = uid;
                found = true;
            }
        }
        return response;
    }

    private Flags readFlags(int uid) {
        Flags response = null;
        if (sequence.contains(new Integer(uid))) {
            ObjectInputStream inFlags = null;
            try {
                inFlags = new ObjectInputStream( new FileInputStream(path + File.separator + uid + FLAGS_EXTENSION));
                response = (Flags)inFlags.readObject();
            } catch(Exception e) {
                logger.error("Error reading flags from disc: " + e);
                e.printStackTrace();
                throw new
                    RuntimeException("Exception caught while retrieving Message flags: "
                                     + e);
            } finally {
                try {
                    inFlags.close();
                } catch (IOException ie) {
                    logger.error("Error closing streams: " + ie);
                }
            }
            logger.info("Flags for " + uid + " read from " + absoluteName);
        }
        return response;
    }

    private boolean writeFlags(int uid, Flags flags) {
        if (sequence.contains(new Integer(uid))) {
            ObjectOutputStream outFlags = null;
            try {
                outFlags = new ObjectOutputStream( new FileOutputStream(path + File.separator + uid + FLAGS_EXTENSION));
                outFlags.writeObject(flags);
                outFlags.close();
            } catch(Exception e) {
                logger.error("Error writing message to disc: " + e);
                e.printStackTrace();
                throw new
                    RuntimeException("Exception caught while storing Flags: "
                                     + e);
            } finally {
                try {
                    outFlags.close();
                } catch (IOException ie) {
                    logger.error("Error closing streams: " + ie);
                }
            }
            logger.info("Flags for " + uid + " written in " + absoluteName);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Removes all messages marked Deleted.  User must have delete rights.
     *
     * @param username String represnting user
     * @returns true if successful
     * @throws AccessControlException if user does not have read rights for
     * this mailbox.
     * @throws AuthorizationException if user has delete rights but does not
     * have delete rights.
     */
    public synchronized boolean expunge(String user)
        throws AccessControlException, AuthorizationException {
        if (!hasDeleteRights(user)) { //throws AccessControlException
            throw new AuthorizationException("Not authorized to delete.");
        }
        Iterator it = messagesForDeletion.iterator();
        while (it.hasNext()) {
            Integer uidObj = (Integer)it.next();
            int uid = uidObj.intValue();
            if (sequence.contains(uidObj)) {
                try  {
                    final File msgFile = new File(path + File.separator + uid + MESSAGE_EXTENSION );
                    msgFile.delete();
                    final File attrFile = new File(path + File.separator + uid + ATTRIBUTES_EXTENSION );
                    attrFile.delete();
                    sequence.remove(uidObj);
                    logger.debug( "Removed message uid " + uid );
                } catch ( final Exception e )  {
                    throw new RuntimeException( "Exception caught while removing" +
                                                " a message: " + e );
                }
            }
        }
        for (int i = 0; i < sequence.size(); i++) {
            System.err.println("Message with msn " + i + " has uid " + sequence.get(i));
        }
        return true;
    }

    private void writeMailbox() {
        String mailboxRecordFile = path + File.separator + MAILBOX_FILE_NAME;
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream( new FileOutputStream(mailboxRecordFile));
            out.writeObject(this);
            out.close();
        } catch(Exception e) {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception ignored) {
                }
            }
            e.printStackTrace();
            throw new
                RuntimeException("Exception caught while storing Mailbox: " + e);
        }
        logger.info("FileMailbox written: " + absoluteName);
    }


    /**
     * Lists uids of messages in mailbox indexed by MSN.
     *
     * @param username String represnting user
     * @returns List of Integers wrapping uids of message
     * @throws AccessControlException if user does not have lookup rights for
     * this mailbox.
     */
    public List listUIDs(String user) {
        return new ArrayList(Collections.unmodifiableList(sequence));
    }

    public Set getUsersWithLookupRights() {
        Set response = new  HashSet();
        Iterator it = acl.keySet().iterator();
        while (it.hasNext()) {
            String user = (String) it.next();
            boolean[] rights = (boolean[]) acl.get(user);
            if (rights[LOOKUP] == true) {
                response.add(user);
            }
        }
        return response;
    }

    public Set getUsersWithReadRights() {
        Set response = new  HashSet();
        Iterator it = acl.keySet().iterator();
        while (it.hasNext()) {
            String user = (String) it.next();
            boolean[] rights = (boolean[]) acl.get(user);
            if (rights[READ] == true) {
                response.add(user);
            }
        }
        return response;
    }

    public Map getUnseenByUser() {
        Map response = new HashMap();
        Iterator it = oldestUnseenMessage.keySet().iterator();
        while (it.hasNext()) {
            String user = (String) it.next();
            Integer uidObj = ((Integer)oldestUnseenMessage.get(user));
            int oldUID = uidObj.intValue();
            if (oldUID == 0) {
                response.put(user, uidObj);
            } else {
                int count = 0;
                ListIterator lit
                    = sequence.listIterator(sequence.indexOf(uidObj));
                while (lit.hasNext() ) {
                    int uid = ((Integer)lit.next()).intValue();
                    Flags flags = readFlags(uid);
                    if (!flags.isSeen(user)) {
                        count ++;
                    }
                }
                response.put(user, new Integer(count));
            }
        }
        return response;
    }


    public InternetHeaders getInternetHeaders(int msn, String user)
        throws AccessControlException, AuthorizationException {
        if (!hasReadRights(user)) { //throws AccessControlException
            throw new AuthorizationException("Not authorized to read.");
        }
        if (msn > sequence.size()) {
            return null;
        } else {
            int uid = ((Integer)sequence.get(msn - 1)).intValue();
            return getInternetHeadersUID(uid, user);
        }
    }

    public InternetHeaders getInternetHeadersUID(int uid, String user)
        throws AccessControlException, AuthorizationException {
        InternetHeaders response = null;
        if (sequence.contains(new Integer(uid))) {
            BufferedInputStream inMsg = null;
            try {
                inMsg = new BufferedInputStream( new FileInputStream(path + File.separator + uid + MESSAGE_EXTENSION));
                response = new InternetHeaders(inMsg);
                inMsg.close();
            } catch(Exception e) {
                logger.error("Error reading headers of message from disc: " + e);
                e.printStackTrace();
                throw new
                    RuntimeException("Exception caughtt while retrieving InternetHeaders: "    + e);
            } finally {
                try {
                    inMsg.close();
                } catch (IOException ie) {
                    logger.error("Error closing streams: " + ie);
                }
            }
            logger.info("InternetHeaders for message " + uid + " read from "
                        + absoluteName);
            return response;
        } else {
            return null;
        }
    }
}



