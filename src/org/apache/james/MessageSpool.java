package org.apache.james;

import java.io.*;
import java.util.*;

import javax.mail.*;
import javax.mail.internet.*;

import org.apache.james.server.*;
/**
 * This is open for server specific implementations, including raw file implementations, JDBC based
 * implementations, what have you.  The spool operates on an add/remove paradigm, combined with a
 * checkin/checkout paradigm.  The spool should provide a list of what messages are available for
 * processing, check these out, but keep track of them so as to provide robustness should the mail
 * server be restarted.  Also, the spool should take note to properly uncheck all messages upon start
 * up so that messages do not forever remain checked out even if the process is dead.  The spool is
 * free to set internal timelimits on message checkouts, but that is left open.
 *
 * The server will always call the <code>setProperties</code> method before asking the spool for
 * messages to process or before adding any new messages.  Since there is no destroy method and there
 * could be need to reset the properties, the spool should perform any shutdown steps before restarting
 * itself.
 * 
 * Note that it is the spool's responsibility to determine the order of messages to be checked out.
 * I recommend spool's keep track of a message's last activity, and always return the oldest modified
 * message.  Alternatively, the spool could implement the reverse concept where the most recent message
 * is the first to be processed, or messages received in the last 2 minutes are the first to be
 * checked out.  The server relies on the message spool to handle this kind of prioritization.  There are
 * implications with both strategy, and perhaps a message spool implementation could dynamically
 * modify this through property settings.
 * @author Serge Knystautas <sergek@lokitech.com>
 * @version 0.9
 */
public interface MessageSpool
{
/**
 * Add a message to the queue to be processed
 * @param message javax.mail.internet.MimeMessage
 * @param addresses javax.mail.Address[]
 */
public void addMessage (MimeMessage message, InternetAddress[] addresses) throws MessagingException;
/**
 * Add a message to the queue to be processed
 * @param message javax.mail.internet.MimeMessage
 * @param addresses javax.mail.Address[]
 */
public void addMessage (MimeMessage message, DeliveryState state) throws MessagingException;
/**
 * Return the message to the spool for processing, but cancel and ignore any changes we've made
 * @param message javax.mail.internet.MimeMessage
 */
public void cancelChanges (MimeMessage message);
/**
 * Return this message to the queue, and save the changes we've made
 * @param msg javax.mail.internet.MimeMessage
 * @param state org.apache.james.DeliveryState
 */
public void checkinMessage (MimeMessage msg, DeliveryState state);
/**
 * Checks out a message from the queue (sequence determined by queue)
 * @param stage Message stage
 */
public MimeMessage checkoutMessage ();
/**
 * Gets the DeliveryState object for a given message in the queue
 * @return org.apache.james.DeliveryState
 * @param messageID java.lang.String
 */
public DeliveryState getDeliveryState (MimeMessage message);
/**
 * Returns true if there are any messages in the spool
 * @return boolean
 */
public boolean hasMessages ();
/**
 * Establishes the server's configuration settings.  This should also initialize the queue
 * if needed and restore any previous spool settings.
 * @param server org.apache.james.server.JamesServ
 */
public void init (JamesServ server);
/**
 * Remove a message from the spool
 * @param message javax.mail.internet.MimeMessage
 */
public void removeMessage (MimeMessage message);
}