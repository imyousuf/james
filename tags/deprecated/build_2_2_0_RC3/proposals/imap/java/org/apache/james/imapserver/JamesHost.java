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

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.imapserver.AccessControlException;
import org.apache.james.imapserver.AuthorizationException;
import org.apache.james.services.MailServer;
import org.apache.james.services.UsersRepository;
import org.apache.james.services.UsersStore;
import org.apache.james.util.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.*;


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
 * References: rfc 2060
 * @version 0.2 on 04 Aug 2002
 * @see FolderRecord
 * @see RecordRepository
 */
public class JamesHost
        extends AbstractLogEnabled
        implements Host, Component, Composable, 
                   Configurable, Contextualizable, Initializable {

    private Context context;
    private Configuration conf;
    private ComponentManager compMgr;
    private String rootPath; // ends with File.separator
    private IMAPSystem imapSystem;
    private UsersRepository localUsers;
    private RecordRepository recordRep;
    private OpenMailboxes openMailboxes; //maps absoluteName to ACLMailbox

    public static final String HIERARCHY_SEPARATOR = ".";
    public static final char HIERARCHY_SEPARATOR_CHAR = '.';
    public static final String NAMESPACE_TOKEN = "#";
    public static final String PRIVATE_NAMESPACE_PREFIX = "";
    private static final String USER_NAMESPACE = "user";
    private static final String SHARE_NAMESPACE = "share";
    public static final String USER_NAMESPACE_PREFIX = NAMESPACE_TOKEN + USER_NAMESPACE;
    public static final String SHARE_NAMESPACE_PREFIX = NAMESPACE_TOKEN + SHARE_NAMESPACE;
    
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

    /**
     * @see org.apache.avalon.framework.context.Contextualizable#contextualize(Context)
     */
    public void contextualize( Context context ) {
        this.context = context;
    }

    /**
     * @see org.apache.avalon.framework.component.Composable#compose(ComponentManager)
     */
    public void compose( ComponentManager comp ) {
        compMgr = comp;
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure( Configuration conf ) throws ConfigurationException {
        this.conf = conf;
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception {
        getLogger().info( "JamesHost init..." );

        imapSystem = (IMAPSystem) compMgr.lookup( IMAPSystem.ROLE );

        UsersStore usersStore = (UsersStore) compMgr.lookup( "org.apache.james.services.UsersStore" );
        localUsers = usersStore.getRepository( "LocalUsers" );

        String recordRepDest
                = conf.getChild( "recordRepository" ).getValue();
        recordRep = new DefaultRecordRepository();
        setupLogger( recordRep, "recordRep" );
        recordRep.setPath( recordRepDest );
        getLogger().info( "AvalonRecordRepository opened at " + recordRepDest );
        rootPath = conf.getChild( "mailboxRepository" ).getValue();
        if ( !rootPath.endsWith( File.separator ) ) {
            rootPath = rootPath + File.separator;
        }
        prepareDir( rootPath );

        // Create directories for user and shared mailboxes.
        String usersPath = getPath( USER_NAMESPACE_PREFIX );
        prepareDir( usersPath );
        String sharePath = getPath( SHARE_NAMESPACE_PREFIX );
        prepareDir( sharePath );
        getLogger().info( "IMAP Mailbox Repository opened at " + rootPath );
        openMailboxes = new OpenMailboxes(); // how big should this start?
        getLogger().info( "JamesHost ...init end" );
    }

    /**
     *  Checks that the Directory provided exists and is writeable, creating it if it is not.
     */
    private void prepareDir( String dir ) {
        File newFolder = new File( dir );
        if ( !newFolder.isDirectory() ) {
            if ( !newFolder.mkdir() ) {
                throw new RuntimeException( "Error: Cannot create directory: " + dir );
            }
        } else if ( !newFolder.canWrite() ) {
            throw new RuntimeException( "Error: Cannot write to directory: " + dir );
        }
    }

    /**
     * Establishes whether this host is the Home Server for the specified user.
     * Used during login to decide whether a LOGIN_REFERRAL has to be sent to
     * the client.
     *
     * @param username an email address
     * @return true if inbox (and private mailfolders) are accessible through
     * this host.
     */
    public boolean isHomeServer( String username ) {
        return localUsers.contains( username );
    }

    /**
     * Establishes if the specified user can access any mailboxes on this host.
     * Used during login process to decide what sort of LOGIN-REFERRAL must be
     * sent to client.
     *
     * @param username an email address
     * @return true if the specified user has at least read access to any
     * mailboxes on this host.
     */
    public boolean hasLocalAccess( String username ) {
        return localUsers.contains( username );
    }

    /**
     * Returns a reference to an existing Mailbox. The requested mailbox
     * must already exists on this server and the requesting user must have at
     * least lookup rights.
     *
     * @param user email address on whose behalf the request is made.
     * @param mailboxName String name of the target.
     * @return an Mailbox reference.
     * @throws AccessControlException if the user does not have at least
     * lookup rights.
     * @throws MailboxException if mailbox does not exist locally.
     */
    public synchronized ACLMailbox getMailbox( String user, String mailboxName )
            throws AccessControlException, MailboxException {
        Assert.isTrue( Assert.ON &&
                       user != null &&
                       user.length() > 0 &&
                       mailboxName != null );

        getLogger().debug( "Getting mailbox " + mailboxName + " for " + user );
        String absoluteName = getAbsoluteMailboxName( user, mailboxName );
        return getAbsoluteMailbox( user, absoluteName );
    }

    private synchronized ACLMailbox getAbsoluteMailbox( String user, String absoluteName )
            throws AccessControlException, MailboxException {
        Assert.isTrue( Assert.ON &&
                       user != null &&
                       absoluteName.startsWith( NAMESPACE_TOKEN ) );

        ACLMailbox mailbox = null;
        FolderRecord record = null;

        // Has a folder with this name ever been created?
        System.out.println("THISISTHE ABSOLUTENAME IN getAbsoluteMailbox " + absoluteName);
        if ( !recordRep.containsRecord( absoluteName ) ) {
            throw new MailboxException( "Mailbox: " + absoluteName + " has never been created.", MailboxException.NOT_LOCAL );
        } else {
            record = recordRep.retrieve( absoluteName );
            if ( record.isDeleted() ) {
                throw new MailboxException( "Mailbox has been deleted", MailboxException.LOCAL_BUT_DELETED );
            } else if ( openMailboxes.contains( absoluteName ) ) {
                mailbox = openMailboxes.getMailbox( absoluteName );
                if ( ! mailbox.hasLookupRights( user ) ) {
                    throw new AccessControlException( "No lookup rights." );
                }
                openMailboxes.addReference( absoluteName );
                return mailbox;
            } else {
                String owner = record.getUser();
                String key = getPath( absoluteName );
                ObjectInputStream in = null;
                try {
                    // SK:UPDATE
                    in = new ObjectInputStream( new FileInputStream( key + File.separator + FileMailbox.MAILBOX_FILE_NAME ) );
                    mailbox = (FileMailbox) in.readObject();
                    setupLogger( mailbox );
                    mailbox.configure( conf );
                    mailbox.contextualize( context );
                    mailbox.compose( compMgr );
                    mailbox.reinitialize();
                } catch ( Exception e ) {
                    e.printStackTrace();
                    throw new RuntimeException( "Exception caught while reading FileMailbox: " + e );
                } finally {
                    if ( in != null ) {
                        try {
                            in.close();
                        }
                        catch ( Exception ignored ) {
                        }
                    }
                    notifyAll();
                }
                if ( !mailbox.hasLookupRights( user ) ) {
                    throw new AccessControlException( "No lookup rights." );
                }
                openMailboxes.addMailbox( absoluteName, mailbox );
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
     * @return an Mailbox reference.
     * @throws AccessControlException if the user does not have lookup rights
     * for parent or any needed ancestor folder
     * lookup rights.
     * @throws AuthorizationException if mailbox could be created locally but
     * user does not have create rights.
     * @throws MailboxException if mailbox already exists, locally or remotely,
     * or if mailbox cannot be created locally.
     * @see FolderRecord
     */
    public synchronized ACLMailbox createMailbox( String user, String mailboxName )
            throws AccessControlException, AuthorizationException, MailboxException {
        Assert.isTrue( Assert.ON &&
                       user != null &&
                       user.length() > 0 &&
                       mailboxName != null );

        String absoluteName = getAbsoluteMailboxName( user, mailboxName );
        Assert.isTrue( Assert.ON &&
                       absoluteName != null );

        getLogger().debug( "JamesHost createMailbox() for:  " + absoluteName );

        return createAbsoluteMailbox( user, absoluteName );
    }

    private synchronized ACLMailbox createAbsoluteMailbox( String user, String absoluteName )
            throws AccessControlException, AuthorizationException, MailboxException {

        Assert.isTrue( Assert.ON &&
                       absoluteName.startsWith( NAMESPACE_TOKEN ) &&
                       absoluteName.indexOf( HIERARCHY_SEPARATOR ) != -1 );

        ACLMailbox mailbox = null;
        FolderRecord record = null;
        ACLMailbox parentMailbox = null;

        // Has a folder with this name ever been created?
        if ( recordRep.containsRecord( absoluteName ) ) {
                getLogger().error( "Attempt to create an existing Mailbox." );
                throw new MailboxException( "Mailbox already exists", MailboxException.ALREADY_EXISTS_LOCALLY );
        } else {
            // Get the directory holding the new mailbox.
            String parent = absoluteName.substring( 0, absoluteName.lastIndexOf( HIERARCHY_SEPARATOR ) );
            //Recurse to a created and not deleted mailbox
            try {
                parentMailbox = getAbsoluteMailbox( user, parent );
            } catch ( MailboxException mbe ) {
                if ( mbe.getStatus().equals( MailboxException.NOT_LOCAL )
                        || mbe.getStatus().equals( MailboxException.LOCAL_BUT_DELETED ) ) {
                    parentMailbox = createAbsoluteMailbox( user, parent );
                } else {
                    throw new MailboxException( mbe.getMessage(), mbe.getStatus() );
                }
            }
            // Does user have create rights in parent mailbox?
            boolean hasCreateRights = parentMailbox.hasCreateRights( user );
            releaseMailbox( user, parentMailbox );
            if ( ! hasCreateRights ) {
                releaseMailbox( user, parentMailbox );
                throw new AuthorizationException( "User does not have create rights." );
            }
            try {
                //TODO: BUG! Here MUST be used a Factory for using FILE / JDBC !!!
                mailbox = new FileMailbox();
                setupLogger( mailbox );
                mailbox.configure( conf );
                mailbox.contextualize( context );
                mailbox.compose( compMgr );
                mailbox.prepareMailbox( user, absoluteName, user, recordRep.nextUIDValidity() );
                mailbox.initialize();
            } catch ( Exception e ) {
                getLogger().error( "Exception creating mailbox: " + e );
                throw new MailboxException( "Exception creating mailbox: " + e );
            }
            SimpleFolderRecord fr
                    = new SimpleFolderRecord( user, absoluteName );
            fr.initialize();
            recordRep.store( fr );
            openMailboxes.addMailbox( absoluteName, mailbox );
        }

        return mailbox;
    }

    /**
     * Releases a reference to a mailbox, allowing Host to do any housekeeping.
     *
     * @param mailbox a non-null reference to an ACL Mailbox.
     */
    public void releaseMailbox( String user, ACLMailbox mailbox )
    {
        if ( mailbox == null ) {
            getLogger().debug( "Attempt to release mailbox with null reference" );
            return;
        }
        if ( user != MailServer.MDA ) {
            mailbox.unsetRecent();
        }
        String absoluteName = mailbox.getAbsoluteName();
        int count = openMailboxes.removeReference( absoluteName );
        if ( count == 0 ) {
            openMailboxes.removeMailbox( absoluteName );
            try {
                FolderRecord fr = recordRep.retrieve( absoluteName );
                fr.setUidValidity( mailbox.getUIDValidity() );
                fr.setHighestUid( mailbox.getNextUID() - 1 );
                fr.setLookupRights( mailbox.getUsersWithLookupRights() );
                fr.setReadRights( mailbox.getUsersWithReadRights() );
                fr.setMarked( mailbox.isMarked() );
                fr.setNotSelectableByAnyone( mailbox.isNotSelectableByAnyone() );
                fr.setExists( mailbox.getExists() );
                fr.setRecent( mailbox.getRecent() );
                fr.setUnseenbyUser( mailbox.getUnseenByUser() );
                recordRep.store( fr );
                mailbox.dispose();
                mailbox = null;
                getLogger().info( "Mailbox object destroyed: " + absoluteName );
            } catch ( Exception e ) {
                getLogger().error( "Exception destroying mailbox object: " + e );
                e.printStackTrace();
            }
        } else {
            getLogger().info( "Mailbox " + absoluteName + " now has " + count + "live references" );
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
     * @return true if mailbox deleted successfully
     * @throws MailboxException if mailbox does not exist locally or is any
     * identities INBOX.
     * @throws AuthorizationException if mailbox exists locally but user does
     * not have rights to delete it.
     * @see FolderRecord
     */
    public boolean deleteMailbox( String user, String mailboxName )
            throws MailboxException, AuthorizationException, AccessControlException {
        Assert.isTrue( Assert.ON &&
                       user != null &&
                       mailboxName != null &&
                       user.length() > 0 &&
                       mailboxName.length() > 0 );

        String absoluteName = getAbsoluteMailboxName( user, mailboxName );
        getLogger().debug( "JamesHost deleteMailbox() called for:  " + absoluteName );
        return deleteAbsoluteMailbox( user, absoluteName );
    }

    private boolean deleteAbsoluteMailbox( String user, String absoluteName )
            throws MailboxException, AuthorizationException, AccessControlException {
        if ( ! recordRep.containsRecord( absoluteName ) ) {
            throw new MailboxException( "Mailbox doesn't exist" );
        }

        int count = openMailboxes.getReferenceCount( absoluteName );
        if ( count > 0 ) {
            throw new MailboxException( "Mailbox is currently selected by another user" );
        }

        ACLMailbox mailbox = getAbsoluteMailbox( user, absoluteName );
        if ( ! mailbox.hasDeleteRights( user ) ) {
            throw new AuthorizationException( "No delete rights" );
        }

        // Get child folders of this mailbox
        Collection childList = listMailboxes( MailServer.MDA, absoluteName, "%", false );
        if ( ! childList.isEmpty() ) {
            if ( mailbox.isNotSelectableByAnyone() ) {
                throw new MailboxException( "Mailbox with \\Noselect AND subfolders cannot be deleted" );
            } else {
                // Delete and expunge all messages, and set NotSelectableByAnyone.
                deleteAllMessages( mailbox, user );
                mailbox.setNotSelectableByAnyone( true );
                releaseMailbox( user, mailbox );
            }
        } else {
            deleteAllMessages( mailbox, user );
            Assert.isTrue( Assert.ON &&
                           mailbox.getExists() == 0 );

            openMailboxes.removeMailbox( absoluteName );
            recordRep.deleteRecord( recordRep.retrieve( absoluteName ) );
            mailbox.removeMailbox();
        }
        return true;
    }

    private void deleteAllMessages( ACLMailbox mailbox, String user )
            throws AccessControlException, AuthorizationException {
        // Delete all messages in this box
        int messageCount = mailbox.getExists();
        for ( int i = 0; i < messageCount; i++ ) {
            mailbox.markDeleted( i + 1, user );
        }
        mailbox.expunge( user );
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
     * @return true if rename completed successfully
     * @throws MailboxException if mailbox does not exist locally, or there
     * is an existing mailbox with the new name.
     * @throws AuthorizationException if user does not have rights to delete
     * the existing mailbox or create the new mailbox.
     * @see FolderRecord
     */
    public boolean renameMailbox( String user, String oldMailboxName, String newMailboxName )
            throws MailboxException, AuthorizationException {
        // At first we MUST ensure, that the User has the rights for renaming not only the Directory 
        // that he wanted to rename, but all subdiretories too.
        // Also we have to "rename" the subdirectories as well.
        try {
            ACLMailbox mailbox = this.getMailbox(user,oldMailboxName);
            String oldAbsoluteName = getAbsoluteMailboxName(user, oldMailboxName);
            
            //trying to get children Mailboxes
            Collection coll = this.listMailboxes(user,"",oldMailboxName+"*",false);
            java.util.TreeMap tr = new java.util.TreeMap();
            Iterator it = coll.iterator();
            
            int mindepth=9999;
            int maxdepth=0;
            while(it.hasNext()) {
                // BUG: Whats about Mailbox Names with an Whitespace ?
                StringTokenizer strl = new StringTokenizer((String) it.next());
                String mailboxname = "";
                boolean mbxfound=false;
                while(strl.hasMoreTokens()) {
                    String token = strl.nextToken();
                    if(token.equals("\""+this.HIERARCHY_SEPARATOR+"\"") || mbxfound) {
                        // This must be the FileSeparator Token. After that comes the name of the Mailbox
                        if(mbxfound){
                            mailboxname += " "+token;
                        } else {
                            mailboxname += strl.nextToken();
                        }
                        mbxfound=true;
                    }
                }
                // StringTokenizer is only used to get the depth of the Mailboxname
                strl = new StringTokenizer(mailboxname, this.HIERARCHY_SEPARATOR);
                mailboxname = mailboxname.substring(1,mailboxname.length()-1);
                System.out.println("RENAME: MAILBOXNAME FOUND: "+mailboxname);
                // Create Collection (or first try to get one from the Sorted Tree) and then
                // save this to the TreeMap. The TreeMap is used to order the found Mailbox Names after their
                // depth. It's needed to first change the Mailbox Properties for all Subdirectories, and update the
                // FolderRecords. 
                Object oldcoll = tr.get(new Integer(strl.countTokens()));
                if (oldcoll==null) {
                    HashSet hs = new HashSet();
                    hs.add(mailboxname);
                    tr.put(new Integer(strl.countTokens()), hs);
                } else {
                    HashSet hs = (HashSet) oldcoll;
                    hs.add(mailboxname);
                    tr.put(new Integer(strl.countTokens()), hs);
                }
                if(maxdepth<strl.countTokens()) {
                    maxdepth=strl.countTokens();
                }
                if(mindepth>strl.countTokens()) {
                    mindepth=strl.countTokens();
                }
            }
            System.out.println("RENAME: POSSIBLE MAXDEPTH FOUND: "+maxdepth+" THE MINDEPTH IS "+mindepth);
            for(int i=maxdepth;i>=0;i--) {
                Collection cole = (Collection) tr.get(new Integer(i));
                if (cole!=null) {
                    Iterator ite = cole.iterator();
                    while(ite.hasNext()) {
                        String mbxhere = (String) ite.next();
                        String absoluteName = getAbsoluteMailboxName(user, mbxhere);
                        System.out.println("RENAME: MAILBOXES FOR INTEGER "+i+ " NAME "+mbxhere+" ABSOLUTE "+absoluteName);
                        ACLMailbox mmm = this.getAbsoluteMailbox(user,absoluteName);
                        if (i!=mindepth) {
                            String suboldabsolutename = mmm.getAbsoluteName();
                            mmm.renameSubMailbox(user, oldAbsoluteName, newMailboxName);
                           // THis is the new AbsoluteName with # and so on
                            String subnewabsolutename = mmm.getAbsoluteName();
                            // Delete the old FolderRecord
                            FolderRecord fr = recordRep.retrieve(suboldabsolutename);
                            recordRep.deleteRecord(fr);
                            // Create a new FolderRecord and store this in the recordRepository
                            // Can anybody explain me the use of this repository ???
                            // I think OpenMailbox and Repository is relatively redundant
                            fr = new SimpleFolderRecord(user, subnewabsolutename);
                            fr.setUidValidity( mmm.getUIDValidity() );
                            fr.setLookupRights( mmm.getUsersWithLookupRights() );
                            fr.setReadRights( mmm.getUsersWithReadRights() );
                            fr.setMarked( mmm.isMarked() );
                            fr.setNotSelectableByAnyone( mmm.isNotSelectableByAnyone() );
                            fr.setExists( mmm.getExists() );
                            fr.setRecent( mmm.getRecent() );
                            fr.setUnseenbyUser( mmm.getUnseenByUser() );
                            recordRep.store(fr);
                            // Remove old entry from openMailboxes and create a new one
                            this.openMailboxes.removeMailbox(suboldabsolutename);
                            this.openMailboxes.addMailbox(subnewabsolutename,mmm);
                        } else {
                            // RENAME MAILBOX MAIN
                            System.out.println("NEWMAILBOXNAME WILL BE "+newMailboxName);
                            if (mmm.renameMailbox(user, newMailboxName)) {
                                // THis is the new AbsoluteName with # and so on
                                String absolutenewName = getAbsoluteMailboxName(user, newMailboxName);
                                // Delete the old FolderRecord
                                FolderRecord fr = recordRep.retrieve(oldAbsoluteName);
                                recordRep.deleteRecord(fr);
                                // Create a new FolderRecord and store this in the recordRepository
                                // Can anybody explain me the use of this repository ???
                                // I think OpenMailbox and Repository is relatively redundant
                                fr = new SimpleFolderRecord(user, absolutenewName);
                                fr.setUidValidity( mmm.getUIDValidity() );
                                fr.setLookupRights( mmm.getUsersWithLookupRights() );
                                fr.setReadRights( mmm.getUsersWithReadRights() );
                                fr.setMarked( mmm.isMarked() );
                                fr.setNotSelectableByAnyone( mmm.isNotSelectableByAnyone() );
                                fr.setExists( mmm.getExists() );
                                fr.setRecent( mmm.getRecent() );
                                fr.setUnseenbyUser( mmm.getUnseenByUser() );
                                recordRep.store(fr);
                                // Remove old entry from openMailboxes and create a new one
                                this.openMailboxes.removeMailbox(oldAbsoluteName);
                                mailbox = getAbsoluteMailbox( user, absolutenewName );
                                this.openMailboxes.addMailbox(absolutenewName,mailbox);
                                return true;
                            } else {
                                return false;
                            }
                        }
                    }
                }
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            throw new AuthorizationException("You have insufficient permission to change the Mailbox Name");
        }
    }

    /**
     * Returns the namespace which should be used for this user unless they
     * expicitly request another.
     *
     * @param username String an email address
     * @return a String of a namespace
     */
    public String getDefaultNamespace( String username ) {
        return PRIVATE_NAMESPACE_PREFIX;
    }

    /**
     * Return UIDValidity for named mailbox. Implementations should track
     * existing and deleted folders.
     *
     * @param mailbox String name of the existing mailbox
     * @return  an integer containing the current UID Validity value.
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
     * @return Collection of strings representing a set of mailboxes.
     * @throws AccessControlException if the user does not have at least
     * lookup rights to at least one mailbox in the set requested.
     * @throws MailboxException if the referenceName is not local or if
     * referenceName and mailbox name resolve to a single mailbox which does
     * not exist locally.
     */
    public synchronized Collection listMailboxes( String username,
                                                  String referenceName,
                                                  String mailboxName,
                                                  boolean subscribedOnly )
            throws MailboxException, AccessControlException {
        getLogger().debug( "Listing for user: " + username + " ref " + referenceName + " mailbox " + mailboxName );
        System.out.println( "Listing for user: '" + username + "' ref '" + referenceName + "' mailbox '" + mailboxName + "'");
        List responseList = new ArrayList();

        // For mailboxName == ""; return <"."> <namespace-of-reference>
        if ( mailboxName.equals( "" ) ) {
            String referenceNamespace = getNamespacePrefix( referenceName );
            if ( referenceNamespace == null ) {
                getLogger().error( "Weird arguments for LIST? referenceName was: " + referenceName + " and mailbox names was " + mailboxName );
                return null;
            }
            if ( referenceNamespace.length() == 0 ) {
                referenceNamespace = "\"\"";
            }

            String response = "(\\Noselect) \"" + HIERARCHY_SEPARATOR
                    + "\" " + referenceNamespace;
            responseList.add( response );
            return responseList;
        }
        try { 
            getLogger().debug( "Refined mailboxName to: " + mailboxName );
            String userTarget;
            if ( mailboxName.startsWith( NAMESPACE_TOKEN ) ) {
                userTarget = mailboxName;
            } else {
                if ( referenceName.length() == 0 ||
                        referenceName.endsWith( HIERARCHY_SEPARATOR ) ) {
                    userTarget = referenceName + mailboxName;
                } else {
                    userTarget = referenceName + HIERARCHY_SEPARATOR + mailboxName;
                }
            }
            String target = getAbsoluteMailboxName( username, userTarget );
            getLogger().info( "Target is: " + target );
            if ( target == null ) {
                return new HashSet();
            }
            int firstPercent = target.indexOf( "%" );
            int firstStar = target.indexOf( "*" );
            getLogger().info( "First percent at index: " + firstPercent );
            getLogger().info( "First star at index: " + firstStar );
            System.out.println( "First percent at index: " + firstPercent );
            System.out.println( "First star at index: " + firstStar );

            // For now, only handle wildcards as last character of target.
            String targetMatch = target;
            boolean starWildcard = false;
            if ( firstStar > -1 ) {
                if ( firstStar != (target.length() - 1) ) {
                    getLogger().debug( "Non-terminal * in LIST search." );
                    return null;
                }
                starWildcard = true;
                targetMatch = target.substring( 0, target.length() - 1 );
            }
            boolean percentWildcard = false;
            if ( firstPercent > -1 ) {
                if ( firstPercent != (target.length() - 1) ) {
                    getLogger().debug( "Non-terminal % in LIST search." );
                    return null;
                }
                percentWildcard = true;
                targetMatch = target.substring( 0, target.length() - 1 );
            }

            Iterator all = recordRep.getAbsoluteNames();
            Set matches = new HashSet();

            while ( all.hasNext() ) {
                boolean match = false;
                String testMailboxName = (String) all.next();
                getLogger().info( "Test is: " + testMailboxName );

                if ( starWildcard ) {
                    match = testMailboxName.startsWith( targetMatch );
                } else if ( percentWildcard ) {
                    match = (testMailboxName.startsWith( targetMatch )
                            && testMailboxName.lastIndexOf( HIERARCHY_SEPARATOR ) < targetMatch.length());
                } else {
                    // no wildcards so exact or nothing
                    match = testMailboxName.equals( target );
                    getLogger().debug( "match/ no match at testMailboxName 1" );
                }

                if ( match && subscribedOnly ) {
                    ACLMailbox mailbox = getAbsoluteMailbox( username, testMailboxName );
                    if (! mailbox.isSubscribed( username ) ) {
                        match = false;
                    }
                    releaseMailbox( username, mailbox );
                }

                if ( match ) {
                    getLogger().info( "Processing match for : " + testMailboxName );
                    FolderRecord record = recordRep.retrieve( testMailboxName );
                    ACLMailbox mailbox = null;
                    StringBuffer buf = new StringBuffer();
                    buf.append( "(" );
                    if ( !record.isDeleted() && openMailboxes.contains( target ) ) {
                        mailbox = openMailboxes.getMailbox( target );
                    }
                    if ( record.isDeleted() ) {
                        buf.append( "\\Noselect" );
                    } else if ( openMailboxes.contains( target ) ) {
                        mailbox = openMailboxes.getMailbox( target );
                        if ( !mailbox.isSelectable( username ) ) {
                            buf.append( "\\Noselect" );
                        }
                        if ( mailbox.isMarked() ) {
                            buf.append( "\\Marked" );
                        } else {
                            buf.append( "\\Unmarked" );
                        }
                    } else {
                        if ( !record.isSelectable( username ) ) {
                            buf.append( "\\Noselect" );
                        }
                        if ( record.isMarked() ) {
                            buf.append( "\\Marked" );
                        } else {
                            buf.append( "\\Unmarked" );
                        }
                    }
                    buf.append( ") \"" );
                    buf.append(  HIERARCHY_SEPARATOR );
                    buf.append( "\" \"" );
                    buf.append( getUserAwareMailboxName( username, testMailboxName ) );
                    buf.append( "\"" );
                    matches.add( buf.toString() );
                }
            }
            return matches;
        }
        catch ( Exception e ) {
            getLogger().error( "Exception with list request for mailbox " + mailboxName );
            e.printStackTrace();
            return null;
        }
    }

    private String getNamespacePrefix( String mailbox )
    {
        if ( mailbox.startsWith( USER_NAMESPACE_PREFIX ) ) {
            return USER_NAMESPACE_PREFIX;
        } else if ( mailbox.startsWith( SHARE_NAMESPACE_PREFIX ) ) {
            return SHARE_NAMESPACE_PREFIX;
        } else {
            return PRIVATE_NAMESPACE_PREFIX;
        }
    }

    /**
     * Subscribes a userName to a mailbox. The mailbox must exist locally and the
     * userName must have at least lookup rights to it.
     *
     * @param username String representation of an email address
     * @param mailbox String representation of a mailbox name.
     * @return true if subscribe completes successfully
     * @throws AccessControlException if the mailbox exists but the userName does
     * not have lookup rights.
     * @throws MailboxException if the mailbox does not exist locally.
     */
    public boolean subscribe( String userName, String mailboxName )
            throws MailboxException, AccessControlException
    {
        Assert.isTrue( Assert.ON &&
                       userName != null &&
                       mailboxName != null &&
                       userName.length() > 0 &&
                       mailboxName.length() > 0 );

        String absoluteName = getAbsoluteMailboxName( userName, mailboxName );
        ACLMailbox mailbox = getAbsoluteMailbox( userName, absoluteName );

        mailbox.subscribe( userName );
        releaseMailbox( userName, mailbox );

        return true;
    }

    /**
     * Unsubscribes from a given mailbox.
     *
     * @param username String representation of an email address
     * @param mailbox String representation of a mailbox name.
     * @return true if unsubscribe completes successfully
     */
    public boolean unsubscribe( String userName, String mailboxName )
            throws MailboxException, AccessControlException
    {
        Assert.isTrue( Assert.ON &&
                       userName != null &&
                       mailboxName != null &&
                       userName.length() > 0 &&
                       mailboxName.length() > 0 );

        String absoluteName = getAbsoluteMailboxName( userName, mailboxName );
        ACLMailbox mailbox = getAbsoluteMailbox( userName, absoluteName );

        mailbox.unsubscribe( userName );
        releaseMailbox( userName, mailbox );

        return true;
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
     * @return String consisting of space seperated pairs:
     * dataItem-space-number.
     * @throws AccessControlException if the user does not have at least
     * lookup rights to the mailbox requested.
     * @throws MailboxException if the mailboxName does not exist locally.
     */
    public String getMailboxStatus( String username, String mailboxName,
                                    List dataItems )
            throws MailboxException, AccessControlException
    {
        String absoluteName = getAbsoluteMailboxName( username, mailboxName );
        ACLMailbox mailbox = null;
        FolderRecord record = null;
        Iterator it = dataItems.iterator();
        String response = null;

        // Has a folder with this name ever been created?
        if ( !recordRep.containsRecord( absoluteName ) ) {
            throw new MailboxException( "Mailbox: " + absoluteName + " has never been created.", MailboxException.NOT_LOCAL );
        } else {
            record = recordRep.retrieve( absoluteName );
            if ( record.isDeleted() ) {
                throw new MailboxException( "Mailbox has been deleted", MailboxException.LOCAL_BUT_DELETED );
            } else if ( openMailboxes.contains( absoluteName ) ) {
                response = new String();
                mailbox = openMailboxes.getMailbox( absoluteName );
                if ( !mailbox.hasLookupRights( username ) ) {
                    throw new AccessControlException( "No lookup rights." );
                }
                while ( it.hasNext() ) {
                    String dataItem = (String) it.next();
                    if ( dataItem.equalsIgnoreCase( "MESSAGES" ) ) {
                        response += "MESSAGES " + mailbox.getExists();
                    } else if ( dataItem.equalsIgnoreCase( "RECENT" ) ) {
                        response += "RECENT " + mailbox.getRecent();
                    } else if ( dataItem.equalsIgnoreCase( "UIDNEXT" ) ) {
                        response += "UIDNEXT " + mailbox.getNextUID();
                    } else if ( dataItem.equalsIgnoreCase( "UIDVALIDITY" ) ) {
                        response += "UIDVALIDITY " + mailbox.getUIDValidity();
                    } else if ( dataItem.equalsIgnoreCase( "UNSEEN" ) ) {
                        response += "UNSEEN " + mailbox.getUnseen( username );
                    }
                    if ( it.hasNext() ) {
                        response += " ";
                    }
                }
                return response;
            }
            else {
                if ( !record.hasLookupRights( username ) ) {
                    throw new AccessControlException( "No lookup rights." );
                }
                response = new String();
                while ( it.hasNext() ) {
                    String dataItem = (String) it.next();
                    if ( dataItem.equalsIgnoreCase( "MESSAGES" ) ) {
                        response += "MESSAGES" + " " + record.getExists();
                    } else if ( dataItem.equalsIgnoreCase( "RECENT" ) ) {
                        response += "RECENT" + " " + record.getRecent();
                    } else if ( dataItem.equalsIgnoreCase( "UIDNEXT" ) ) {
                        response += "UIDNEXT" + " " + (record.getHighestUid() + 1);
                    } else if ( dataItem.equalsIgnoreCase( "UIDVALIDITY" ) ) {
                        response += "UIDVALIDITY" + " " + record.getUidValidity();
                    } else if ( dataItem.equalsIgnoreCase( "UNSEEN" ) ) {
                        response += "UNSEEN" + " " + record.getUnseen( username );
                    }
                    if ( it.hasNext() ) {
                        response += " ";
                    }
                }
                return response;
            }
        }
    }

    /**
     * Convert a user-based full mailbox name into a server absolute name.
     * If the fullMailboxName begins with the namespace token,
     * return as-is.
     * If not, need to resolve the Mailbox name for this user.
     * Example:
     * <br> Convert "INBOX" for user "Fred.Flinstone" into
     * absolute name: "#user.Fred.Flintstone.INBOX"
     *
     * @return String of absoluteName, null if not valid selection
     */
    private String getAbsoluteMailboxName( String user, String fullMailboxName )
    {
        // First decode the mailbox name as an Atom / String.

        if ( fullMailboxName.startsWith( NAMESPACE_TOKEN ) ) {
            return fullMailboxName;
        }
        else {
            if ( fullMailboxName.length() == 0 ) {
                return USER_NAMESPACE_PREFIX + HIERARCHY_SEPARATOR + user;
            }
            else {
                return USER_NAMESPACE_PREFIX + HIERARCHY_SEPARATOR + user + HIERARCHY_SEPARATOR + fullMailboxName;
            }
        }
    }

    /**
     * Convert a server absolute name into a user-aware mailbox name.
     * If the absolute name starts with #user.<username>.
     * remove this section.
     * Otherwise, return as-is.
     *
     * @return user-aware mailbox name
     */
    private String getUserAwareMailboxName( String user, String absoluteName )
    {
        String userPrefix = USER_NAMESPACE_PREFIX + HIERARCHY_SEPARATOR + user + HIERARCHY_SEPARATOR;
        String response;
        if ( absoluteName.startsWith( userPrefix ) ) {
            response = absoluteName.substring( userPrefix.length() );
        } else {
            response = absoluteName;
        }
        return response;
    }

    /**
     * Return the file-system path to a given absoluteName mailbox.
     *
     * @param absoluteName the user-independent name of the mailbox
     * @param owner string name of owner of mailbox
     */
    String getPath( String absoluteName ) {
        Assert.isTrue( Assert.ON &&
                       absoluteName.startsWith( NAMESPACE_TOKEN ) );

        // Remove the leading '#' and replace Hierarchy separators with file separators.
        String filePath = absoluteName.substring( NAMESPACE_TOKEN.length() );
        filePath = filePath.replace( HIERARCHY_SEPARATOR_CHAR, File.separatorChar );
        return rootPath + filePath;
    }

    public boolean createPrivateMailAccount( String user ) {
        Assert.isTrue( Assert.ON &&
                       user != null &&
                       user.length() > 0 );

        String userRootName
                = getAbsoluteMailboxName( user, "" );
        String userInboxName
                = getAbsoluteMailboxName( user, "INBOX" );
        SimpleFolderRecord userRootRecord
                = new SimpleFolderRecord( user,
                                          userRootName );
        SimpleFolderRecord userInboxRecord
                = new SimpleFolderRecord( user, userInboxName );

        ACLMailbox userRootFolder = new FileMailbox();
        ACLMailbox userInbox = new FileMailbox();
        try {
            setupLogger( userRootFolder );
            userRootFolder.configure( conf );
            userRootFolder.contextualize( context );
            userRootFolder.compose( compMgr );
            userRootFolder.prepareMailbox( user, userRootName, user, recordRep.nextUIDValidity() );
            setupLogger( userInbox );
            userInbox.configure( conf );
            userInbox.contextualize( context );
            userInbox.compose( compMgr );
            userInbox.prepareMailbox( user, userInboxName, user, recordRep.nextUIDValidity() );
            userRootFolder.initialize();
            userRootFolder.setNotSelectableByAnyone( true );
            userInbox.initialize();
            userInbox.setRights( user, MailServer.MDA, "lrswi" );
        }
        catch ( Exception e ) {
            getLogger().error( "Exception creating new account ", e );
            return false;
        }
        userInboxRecord.initialize();
        userInboxRecord.setUidValidity( userInbox.getUIDValidity() );
        userInboxRecord.setHighestUid( userInbox.getNextUID() - 1 );
        userInboxRecord.setLookupRights( userInbox.getUsersWithLookupRights() );
        userInboxRecord.setReadRights( userInbox.getUsersWithReadRights() );
        userInboxRecord.setNotSelectableByAnyone( userInbox.isNotSelectableByAnyone() );
        userRootRecord.initialize();
        userRootRecord.setLookupRights( userRootFolder.getUsersWithLookupRights() );
        userRootRecord.setReadRights( userRootFolder.getUsersWithReadRights() );
        userRootRecord.setNotSelectableByAnyone( userRootFolder.isNotSelectableByAnyone() );
        recordRep.store( userRootRecord );
        recordRep.store( userInboxRecord );
        
        userRootFolder = null;
        userInbox = null;

        return true;
    }

    private static final class OpenMailboxes {

        private Map _mailboxes = new HashMap();

        boolean contains( String absoluteName ) {
            return _mailboxes.containsKey( absoluteName );
        }

        private OpenMailbox getOpen( String absoluteName ) {
            return (OpenMailbox)_mailboxes.get( absoluteName );
        }

        void addMailbox( String absoluteName, ACLMailbox mailbox ) {
            OpenMailbox openMailbox = new OpenMailbox( mailbox );
            _mailboxes.put( absoluteName, openMailbox );
        }

        void removeMailbox( String absoluteName ) {
            _mailboxes.remove( absoluteName );
        }

        ACLMailbox getMailbox( String absoluteName ) {
            return getOpen( absoluteName ).getMailbox();
        }

        int addReference( String absoluteName ) {
            return getOpen( absoluteName ).addReference();
        }

        int removeReference( String absoluteName ) {
            return getOpen( absoluteName ).removeReference();
        }

        int getReferenceCount( String absoluteName ) {
            OpenMailbox openMailbox = getOpen( absoluteName );
            if ( openMailbox == null ) {
                return 0;
            } else {
                return openMailbox.getReferenceCount();
            }
        }
    }

    private static final class OpenMailbox {
        private ACLMailbox _mailbox;
        private int _referenceCount;

        OpenMailbox( ACLMailbox mailbox ) {
            _mailbox = mailbox;
            _referenceCount = 1;
        }

        ACLMailbox getMailbox() {
            return _mailbox;
        }

        int getReferenceCount() {
            return _referenceCount;
        }

        int addReference() {
            return ++_referenceCount;
        }

        int removeReference() {
            return --_referenceCount;
        }
    }
}
