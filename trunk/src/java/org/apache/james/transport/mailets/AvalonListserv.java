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

package org.apache.james.transport.mailets;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import javax.mail.MessagingException;
import javax.mail.internet.ParseException;

import org.apache.mailet.MailAddress;
import org.apache.mailet.UsersRepository;

/**
 * MailingListServer capability.
 *
 * <p>Requires a configuration element in the config.xml file of the form:
 * <br>  &lt;mailet match="RecipientIs=LIST-ADDRESS" class="AvalonListserv"&gt;
 * <br>    &lt;repositoryName&gt;LIST-NAME&lt;/repositoryName&gt;
 * <br>    &lt;membersonly&gt;[true|false]&lt;/membersonly&gt;
 * <br>    &lt;attachmentsallowed&gt;[true|false]&lt;/attachmentsallowed&gt;
 * <br>    &lt;replytolist&gt;[true|false]&lt;/replytolist&gt;
 * <br>    &lt;autobracket&gt;[true|false]&lt;/autobracket&gt;
 * <br>    &lt;subjectprefix [xml:space="preserve"]&gt;SUBJECT-PREFIX&lt;/subjectprefix&gt;
 * <br>  &lt;/mailet&gt;
 * <p>repositoryName - the name of a user repository configured in the
 * UsersStore block, e.g.,
 * <br>  &lt;repository name="list-name" class="org.apache.james.userrepository.ListUsersJdbcRepository" destinationURL="db://maildb/lists/list-name"&gt;
 * <br>    &lt;sqlFile&gt;file://conf/sqlResources.xml&lt;/sqlFile&gt;
 * <br>  &lt;/repository&gt;
 * <p>or
 * <br>  &lt;repository name="list-name" class="org.apache.james.userrepository.UsersFileRepository"&gt;
 * <br>    &lt;destination URL="file://var/lists/list-name/"/&gt;
 * <br>  &lt;/repository&gt;
 * <p>membersonly - if true only members can post to the list
 * <p>attachmentsallowed - if false attachments are not allowed
 * <p>replytolist - if true, replies go back to the list address; if
 * false they go to the sender.
 * <p>subjectprefix - a prefix that will be inserted at the front of
 * the subject.  If autobracketing is disabled (see below), the
 * xml:space="preserve" attribute can be used to precisely control the
 * prefix.
 * <p>autobracket - if true the subject prefix will be rendered as
 * "[PREFIX] ", if false, the prefix will be used literally.
 *
 * @version This is $Revision: 1.13 $
 */
public class AvalonListserv extends GenericListserv {

    /**
     * Whether only members can post to the list
     */
    protected boolean membersOnly = false;

    /**
     * Whether attachments can be sent to the list
     */
    protected boolean attachmentsAllowed = true;

    /**
     * Whether the reply-to header should be set to the list address
     */
    protected boolean replyToList = true;

    /**
     * A String to prepend to the subject of the message when it
     * is sent to the list
     */
    protected String subjectPrefix = null;

    /**
     * Whether the subject prefix should be bracketed with '[' and ']'
     */
    protected boolean autoBracket = true;

    /**
     * The repository containing the users on this list
     */
    private UsersRepository members;

    /**
     * Initialize the mailet
     */
    public void init() {
        try {
            membersOnly = new Boolean(getInitParameter("membersonly")).booleanValue();
        } catch (Exception e) {
            // Ignore any exceptions, default to false
        }
        try {
            attachmentsAllowed = new Boolean(getInitParameter("attachmentsallowed")).booleanValue();
        } catch (Exception e) {
            // Ignore any exceptions, default to true
        }
        try {
            replyToList = new Boolean(getInitParameter("replytolist")).booleanValue();
        } catch (Exception e) {
            // Ignore any exceptions, default to true
        }
        subjectPrefix = getInitParameter("subjectprefix");

        try {
            autoBracket = new Boolean(getInitParameter("autobracket")).booleanValue();
        } catch (Exception e) {
            // Ignore any exceptions, default to true
        }


            try {
                members = getMailetContext().getUserRepository(getInitParameter("repositoryName"));
            } catch (MessagingException e) {
                log("init failed cannot access users repository "+getInitParameter("repositoryName"), e);
            }

    }

    public Collection getMembers() throws ParseException {
        Collection reply = new Vector();
        for (Iterator it = members.list(); it.hasNext(); ) {
            String member = it.next().toString();
            try {
                reply.add(new MailAddress(member));
            }
            catch(Exception e) {
                // Handle an invalid subscriber address by logging it and
                // proceeding to the next member.
                StringBuffer logBuffer =
                    new StringBuffer(1024)
                            .append("Invalid subscriber address: ")
                            .append(member)
                            .append(" caused: ")
                            .append(e.getMessage());
                log(logBuffer.toString());
            }
        }
        return reply;
    }

    /**
     * Get whether posting to this list is restricted to list members
     *
     * @return whether posting to this list is restricted to list members
     */
    public boolean isMembersOnly() {
        return membersOnly;
    }

    /**
     * Get whether attachments can be sent to this list
     *
     * @return whether attachments can be sent to this list
     */
    public boolean isAttachmentsAllowed() {
        return attachmentsAllowed;
    }

    /**
     * Get whether the reply-to header for messages sent to this list
     * will be replaced with the list address
     *
     * @return whether replies to messages posted to this list will go to the entire list
     */
    public boolean isReplyToList() {
        return replyToList;
    }

    /**
     * Get the prefix prepended to the subject line
     *
     * @return whether the prefix for subjects on this list will be bracketed.
     */
    public String getSubjectPrefix() {
        return subjectPrefix;
    }

    /**
     * Return whether the prefix for subjects on this list will be bracketed.
     *
     * @return whether the prefix for subjects on this list will be bracketed.
     */
    public boolean isPrefixAutoBracketed() {
        return autoBracket;
    }

    /**
     * Return a string describing this mailet.
     *
     * @return a string describing this mailet
     */
    public String getMailetInfo() {
        return "AvalonListserv Mailet";
    }
}
