/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.mailet;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Date;
/**
 * Wrap a MimeMessage with routing information (from SMTP) such
 * as SMTP specified recipients, sender, and ip address and hostname
 * of sending server.  It also contains its state which represents
 * which processor in the mailet container it is currently running.
 * Special processor names are "root" and "error".
 *
 * @version 0.9
 */
public interface Mail extends Serializable, Cloneable {
    String GHOST = "ghost";
    String DEFAULT = "root";
    String ERROR = "error";
    String TRANSPORT = "transport";
    /**
     * Returns the MimeMessage stored in this message
     *
     * @return the MimeMessage that this Mail object wraps
     * @throws MessagingException - an error occured while loading this object
     */
    MimeMessage getMessage() throws MessagingException;
    /**
     * Returns a Collection of MailAddress objects that are recipients of this message
     *
     * @return a Collection of MailAddress objects that are recipients of this message
     */
    Collection getRecipients();
    /**
     * Method setRecipients.
     * @param recipients a Collection of MailAddress Objects representing the recipients of this message
     */
    void setRecipients(Collection recipients);
    /**
     * The sender of the message, as specified by the MAIL FROM header, or internally defined
     *
     * @return a MailAddress of the sender of this message
     */
    MailAddress getSender();
    /**
     * The current state of the message, such as GHOST, ERROR, or DEFAULT
     *
     * @return the state of this message
     */
    String getState();
    /**
     * The remote hostname of the server that connected to send this message
     *
     * @return a String of the hostname of the server that connected to send this message
     */
    String getRemoteHost();
    /**
     * The remote ip address of the server that connected to send this message
     *
     * @return a String of the ip address of the server that connected to send this message
     */
    String getRemoteAddr();
    /**
     * The error message, if any, associated with this message.  Not sure why this is needed.
     *
     * @return a String of a descriptive error message
     */
    String getErrorMessage();
    /**
     * Sets the error message associated with this message.  Not sure why this is needed.
     *
     * @param msg - a descriptive error message
     */
    void setErrorMessage(String msg);
    /**
     * Sets the MimeMessage associated with this message via the object.
     *
     * @param message - the new MimeMessage that this Mail object will wrap
     */
    void setMessage(MimeMessage message);
    /**
     * Sets the state of this message.
     *
     * @param state - the new state of this message
     */
    void setState(String state);
    /**
     * Method getUID.
     * Mainly used to log the progress of mails through processing the string returned need only be unique to this installation of the system.
     * @return a String Unique Identifier in this system.
     */
    String getName();
    /**
     * Method setName. Set the internal unique identifier of this Mail which need only be unique to this installation of the system.
     * @param name the UID
     */
    void setName(String name);
    /**
     * Method setLastUpdated. Used to set the internal timestamp of the Mail during processing.
     * @param date
     */
    void setLastUpdated(Date date);
    /**
     * Method getLastUpdated.
     * @return Date. timestamp of the last action which changed this Mail
     */
    Date getLastUpdated();
    /**
     * Returns the Mail session attribute with the given name, or null
     * if there is no attribute by that name.
     * An attribute allows a mailet to give this Mail instance additional information
     * not already provided by this interface.<p>
     * A list of currently set attributes can be retrieved using getAttributeNames.
     * <p>
     * The attribute is returned as a java.lang.Object or some subclass. Attribute
     * names should follow the same convention as package names. The Java Mailet API
     * specification reserves names matching java.*, javax.*, and sun.*
     *
     * @param name - a String specifying the name of the attribute
     * @return an Object containing the value of the attribute, or null if no attribute
     *      exists matching the given name
     */
    Serializable getAttribute(String name);
    /**
     * Returns an Iterator containing the attribute names currently available within
     * this Mail instance.  Use the getAttribute(java.lang.String) method with an
     * attribute name to get the value of an attribute.
     *
     * @return an Iterator of attribute names
     */
    Iterator getAttributeNames();
    /**
     * Removes the attribute with the given name from this Mail instance. After
     * removal, subsequent calls to getAttribute(java.lang.String) to retrieve
     * the attribute's value will return null.
     *
     * @param name - a String specifying the name of the attribute to be removed
     * @return previous attribute value associated with specified name, or null
     * if there was no mapping for name (null can also mean that null
     * was bound to the name)
     */
    Serializable removeAttribute(String name);
    /**
     * Binds an object to a given attribute name in this Mail instance. If the name
     * specified is already used for an attribute, this method will remove the old
     * attribute and bind the name to the new attribute.
     * As instances of Mail is Serializable, it is necessary that the attributes being
     * Serializable as well
     * <p>
     * Attribute names should follow the same convention as package names. The Java
     * Mailet API specification reserves names matching java.*, javax.*, and sun.*.
     *
     * @param name - a String specifying the name of the attribute
     * @param object - a Serializable Object representing the attribute to be bound
     * @return the object previously bound to the name, null if the name was
     * not bound (null can also mean that null was bound to the name)
     */
    Serializable setAttribute(String name, Serializable object);
}
