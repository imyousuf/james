package org.apache.james.util;

import java.io.*;
import java.util.*;

import javax.mail.*;
import javax.mail.internet.*;

/**
 * I need this class to handle some issues in the JavaMail API that are more "client-friendly".
 * Specifically:
 * <ul><li>Overriding the Message-ID
 * <li>Headers being added when saving a message
 * <li>Inability to order headers if a message is created from an InputStream
 * </ul>
 * I also add the ability to clone the object (since MimeMessage is not ordinarily cloneable)
 * @author Serge Knystautas <sergek@lokitech.com>
 * @version 0.9
 */
public class ServerMimeMessage extends MimeMessage
{
/**
 * ServerMimeMessage constructor comment.
 * @param arg1 javax.mail.Folder
 * @param arg2 int
 */
protected ServerMimeMessage(javax.mail.Folder arg1, int arg2) {
	super(arg1, arg2);
}
/**
 * ServerMimeMessage constructor comment.
 * @param arg1 javax.mail.Folder
 * @param arg2 java.io.InputStream
 * @param arg3 int
 * @exception javax.mail.MessagingException The exception description.
 */
protected ServerMimeMessage(javax.mail.Folder arg1, java.io.InputStream arg2, int arg3) throws javax.mail.MessagingException {
	super(arg1, arg2, arg3);
	rebuildHeaders ();
}
/**
 * ServerMimeMessage constructor comment.
 * @param arg1 javax.mail.Folder
 * @param arg2 javax.mail.internet.InternetHeaders
 * @param arg3 byte[]
 * @param arg4 int
 * @exception javax.mail.MessagingException The exception description.
 */
protected ServerMimeMessage(javax.mail.Folder arg1, javax.mail.internet.InternetHeaders arg2, byte[] arg3, int arg4) throws javax.mail.MessagingException {
	super(arg1, arg2, arg3, arg4);
}
/**
 * ServerMimeMessage constructor comment.
 * @param arg1 javax.mail.Session
 */
public ServerMimeMessage(javax.mail.Session arg1) {
	super(arg1);
}
/**
 * ServerMimeMessage constructor comment.
 * @param arg1 javax.mail.Session
 * @param arg2 java.io.InputStream
 * @exception javax.mail.MessagingException The exception description.
 */
public ServerMimeMessage(javax.mail.Session arg1, java.io.InputStream arg2) throws javax.mail.MessagingException {
	super(arg1, arg2);
	rebuildHeaders ();
}
/**
 * This method was created in VisualAge.
 * @return javax.mail.internet.MimeMessage
 * @param message javax.mail.internet.MimeMessage
 */
public static MimeMessage cloneMessage (MimeMessage message)
{
	try
	{
		ByteArrayOutputStream bout = new ByteArrayOutputStream ();
		message.writeTo (bout);

		Session session = Session.getDefaultInstance(new Properties (), null);
		session.setDebug (false);

		ByteArrayInputStream bin = new ByteArrayInputStream (bout.toByteArray ());
		return new ServerMimeMessage (session, bin);
	} catch (IOException ioe)
	{
		System.out.println ("Exception @ " + new Date ());
		ioe.printStackTrace (System.out);
	} catch (MessagingException me)
	{
		System.out.println ("Exception @ " + new Date ());
		me.printStackTrace (System.out);
	}
	return null;
}
/**
 * When creating a MimeMessage from an InputStream, the MimeMessage (actually the InternetHeader object)
 * abandons all hope of ordering the headers (for subsequent additions).
 * What this does is rebuild all the headers by converting them from arrays of strings into single
 * strings, and re-adding each header tag/section to a new InternetHeader object created sans
 * InputStream
 */
private void rebuildHeaders () throws MessagingException
{
	int count = 1;
	InternetHeaders newHeader = new InternetHeaders ();
	
	for (Enumeration e = getAllHeaders (); e.hasMoreElements ();)
	{
		Header header = (Header)e.nextElement ();
		newHeader.addHeader (header.getName (), header.getValue ());
	}
	headers = newHeader;
}
	protected void updateHeaders() throws MessagingException
	{
		//Let's leave everything untouched... we've just a server passing along instructions

		//Normally, JavaMail overrides the Message-ID tag, and creates several MIME related
		//headers, including Mime-Version, Content-Type, etc...
	}
}