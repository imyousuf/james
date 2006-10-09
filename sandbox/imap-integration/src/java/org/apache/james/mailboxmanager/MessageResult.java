package org.apache.james.mailboxmanager;

import java.util.Date;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;

import org.apache.mailet.Mail;

/**
 * <p>
 * Used to get specific informations about a Message without dealing with a
 * MimeMessage instance. Demanded information can be requested by binary
 * combining the constants.
 * </p>
 * 
 * <p>
 * I came to the Idea of the MessageResult because there are many possible
 * combinations of different requests (uid, msn, MimeMessage, Flags).
 * </p>
 * <p>
 * e.g. I want to have all uids, msns and flags of all messages. (a common IMAP
 * operation) Javamail would do it that way:
 * <ol>
 * <li>get all Message objects (Message[])</li>
 * <li>call Message.getMessageNumber() </li>
 * <li>call Message.getFlags() </li>
 * <li>call Folder.getUid(Message)</li>
 * </ol>
 * <p>
 * This means creating a lazy-loading MimeMessage instance. </br> So why don't
 * call getMessages(MessageResult.UID | MessageResult.MSN |
 * MessageResult.FLAGS)? This would leave a lot of room for the implementation
 * to optimize
 * </p>
 * 
 * 
 */

public interface MessageResult extends Comparable {

	/**
	 * For example: could have best performance when doing store and then
	 * forget.
	 */
	public static final int NOTHING = 0x00;

	/**
	 * 
	 */
	public static final int MIME_MESSAGE = 0x01;

	/**
	 * return a complete mail object
	 */
	public static final int MAIL = 0x02;

	public static final int UID = 0x04;

	public static final int MSN = 0x08;

	/**
	 * return a string baded key (used by James)
	 */
	public static final int KEY = 0x10;

	public static final int SIZE = 0x20;

	public static final int INTERNAL_DATE = 0x40;

	public static final int FLAGS = 0x80;

	int getIncludedResults();

	boolean contains(int result);

	MimeMessage getMimeMessage();

	long getUid();

	long getUidValidity();

	int getMsn();

	/**
	 * 
	 * <p>
	 * IMAP defines this as the time when the message has arrived to the server
	 * (by smtp). Clients are also allowed to set the internalDate on apppend.
	 * </p>
	 * <p>
	 * Is this Mail.getLastUpdates() for James delivery? Should we use
	 * MimeMessage.getReceivedDate()?
	 * </p>
	 * 
	 * @return
	 */

	Date getInternalDate();

	/**
	 * TODO optional, to be decided <br />
	 * maybe this is a good thing because IMAP often requests only the Flags and
	 * this way we don't need to create a lazy-loading MimeMessage instance just
	 * for the Flags.
	 * 
	 * @return
	 */
	Flags getFlags();

	Mail getMail();

	String getKey();
    
    int getSize();

}
