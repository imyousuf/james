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

package org.apache.james.transport.mailets;

import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;

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
 * @version This is $Revision: 1.15 $
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
        Collection reply = new ArrayList();
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
