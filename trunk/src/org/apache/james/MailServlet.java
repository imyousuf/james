package org.apache.james;

import java.io.*;
import java.util.*;

import javax.mail.*;
import javax.mail.internet.*;

import org.apache.james.server.*;
/**
 * This is the mail servlet... do with it as you will.  A servlet can be placed at any stage in the
 * message filtering process and it is up to the servlet to either assume it was correctly place,
 * or to check the state object to verify correct placement.
 * @author Serge Knystautas <sergek@lokitech.com>
 * @version 0.9
 */
public abstract class MailServlet
{
	private MailServletContext context = null;
/**
 * Indicates whether this mail servlet explicitly handles this email address.  If a servlet
 * handles a particular email address, it should compare this to address and return true
 * if it is a match.  However, if a servlet processes all messages, it should not override
 * this method.
 * @return boolean
 * @param addr javax.mail.internet.InternetAddress
 */
public boolean acceptAddress (InternetAddress address)
{
	return false;
}
/**
 * This method was created in VisualAge.
 * @return org.apache.james.MailServletContext
 */
public MailServletContext getContext ()
{
	return context;
}
/**
 * Initialize the mail servlet
 * @param config MailServletConfig
 * @see MailServletConfig
 */
public void init (MailServletConfig config) throws MessagingException
{
	context = config.getContext ();
	log ("init");
}
/**
 * Logs to the MailServletContext
 * @param message java.lang.String
 */
public void log (String message)
{
	getContext ().log (getClass ().getName () + ": " + message);
}
/**
 * Applies itself as a filter to a single message.  The message object contains the actual message to be
 * processed.  Any changes should be made to this message.  The state object contains information
 * about what stage the message is in, it's state, it's recipients as specified to this mail server,
 * and other attributes not contained in the mail message.
 *
 * Service requests are not handled until servlet initialization has completed. Any requests for service
 * that are received during initialization block until it is complete. Note that servlets typically run
 * inside multi-threaded servers; servers can handle multiple service requests simultaneously. It is the
 * servlet writer's responsibility to synchronize access to any shared resources, such as network
 * connections or the servlet's class and instance variables. Information on multi-threaded programming
 * in Java can be found in the Java tutorial on multi-threaded programming.
 * @param message javax.mail.internet.MimeMessage
 * @param state org.apache.james.DeliveryState
 * @exception javax.mail.MessagingException
 * @exception java.io.IOException
 * @return javax.mail.MimeMessage
 */
public abstract void service (MimeMessage message, DeliveryState state) throws MessagingException, IOException;
}