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

import org.apache.avalon.framework.component.ComponentManager;
import org.apache.james.Constants;
import org.apache.james.services.UsersRepository;
import org.apache.james.services.UsersStore;
import org.apache.james.util.RFC2822Headers;
import org.apache.james.util.XMLResources;
import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.MailetException;
import org.apache.oro.text.regex.*;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.ParseException;
import java.io.IOException;
import java.util.*;


/**
 * CommandListservProcessor processes messages intended for the list serv mailing list.
 * For command handling, see {@link CommandListservManager} <br />
 *
 * This class is based on the existing list serv processor shipped with James.
 * <br />
 * <br />
 *
 * To configure the CommandListservProcessor place this configuratin in the root processor:
 * <pre>
 * &lt;mailet match="RecipientIs=announce@localhost" class="CommandListservProcessor"&gt;
 *  &lt;membersonly&gt;false&lt;/membersonly&gt;
 *  &lt;attachmentsallowed&gt;true&lt;/attachmentsallowed&gt;
 *  &lt;replytolist&gt;true&lt;/replytolist&gt;
 *  &lt;repositoryName&gt;list-announce&lt;/repositoryName&gt;
 *  &lt;subjectprefix&gt;Announce&lt;/subjectprefix&gt;
 *  &lt;autobracket&gt;true&lt;/autobracket&gt;
 *  &lt;listOwner&gt;owner@localhost&lt;/listOwner&gt;
 *  &lt;listName&gt;announce&lt;/listName&gt;
 * &lt;/mailet&gt;
 *
 * </pre>
 *
 * @version CVS $Revision: 1.1.2.4 $ $Date: 2003/10/20 06:03:15 $
 * @since 2.2.0
 */
public class CommandListservProcessor extends GenericMailet {

    /**
     * Whether only members can post to the list specified by the config param: 'membersonly'.
     * <br />
     * eg: <pre>&lt;membersonly&gt;false&lt;/membersonly&gt;</pre>
     *
     * Defaults to false
     */
    protected boolean membersOnly;

    /**
     * Whether attachments can be sent to the list specified by the config param: 'attachmentsallowed'.
     * <br />
     * eg: <pre>&lt;attachmentsallowed&gt;true&lt;/attachmentsallowed&gt;</pre>
     *
     * Defaults to true
     */
    protected boolean attachmentsAllowed;

    /**
     * Whether the reply-to header should be set to the list address
     * specified by the config param: 'replytolist'.
     * <br />
     * eg: <pre>&lt;replytolist&gt;true&lt;/replytolist&gt;</pre>
     *
     * Defaults to true
     */
    protected boolean replyToList;

    /**
     * A String to prepend to the subject of the message when it is sent to the list
     * specified by the config param: 'subjectPrefix'.
     * <br />
     * eg: <pre>&lt;subjectPrefix&gt;MyList&lt;/subjectPrefix&gt;</pre>
     *
     * For example: MyList
     */
    protected String subjectPrefix;

    /**
     * Whether the subject prefix should be bracketed with '[' and ']'
     * specified by the config param: 'autoBracket'.
     * <br />
     * eg: <pre>&lt;autoBracket&gt;true&lt;/autoBracket&gt;</pre>
     *
     * Defaults to true
     */
    protected boolean autoBracket;

    /**
     * The repository containing the users on this list
     * specified by the config param: 'repositoryName'.
     * <br />
     * eg: <pre>&lt;repositoryName&gt;list-announce&lt;/repositoryName&gt;</pre>
     */
    protected UsersRepository usersRepository;

    /**
     * The list owner
     * specified by the config param: 'listOwner'.
     * <br />
     * eg: <pre>&lt;listOwner&gt;owner@localhost&lt;/listOwner&gt;</pre>
     */
    protected MailAddress listOwner;

    /**
     * Name of the mailing list
     * specified by the config param: 'listName'.
     * <br />
     * eg: <pre>&lt;listName&gt;announce&lt;/listName&gt;</pre>
     *
     */
    protected String listName;

    /**
     * For matching
     */
    protected Pattern pattern;

    /**
     * The list serv manager
     */
    protected ICommandListservManager commandListservManager;

    /**
     * Mailet that will add the footer to the message
     */
    protected CommandListservFooter commandListservFooter;

    /**
     * @see XMLResources
     */
    protected XMLResources xmlResources;

    /**
     * Initialize the mailet
     */
    public void init() throws MessagingException {
        membersOnly = getBoolean("membersonly", false);
        attachmentsAllowed = getBoolean("attachmentsallowed", true);
        replyToList = getBoolean("replytolist", true);
        subjectPrefix = getString("subjectprefix", null);
        listName = getString("listName", null);
        autoBracket = getBoolean("autobracket", true);
        try {
            listOwner = new MailAddress(getString("listOwner", null));
            //initialize resources
            initializeResources();
            //init user repos
            initUsersRepository();
            //init regexp
            initRegExp();
        } catch (Exception e) {
            throw new MessagingException(e.getMessage(), e);
        }
    }

    /**
     * A message was sent to the list serv.  Broadcast if appropriate...
     * @param mail
     * @throws MessagingException
     */
    public void service(Mail mail) throws MessagingException {
        try {
            Collection members = new ArrayList();
            members.addAll(getMembers());
            MailAddress listservAddr = (MailAddress) mail.getRecipients().iterator().next();

            //Check for members only flag....
            if (!checkMembers(members, mail)) {
                return;
            }

            //Check for no attachments
            if (!checkAnnouncements(mail)) {
                return;
            }

            //check been there
            if (!checkBeenThere(listservAddr, mail)) {
                return;
            }

            //addfooter
            addFooter(mail);

            //prepare the new message
            MimeMessage message = prepareListMessage(mail, listservAddr);

            //Set the subject if set
            setSubject(message);

            //Send the message to the list members
            //We set the list owner as the sender for now so bounces go to him/her
            getMailetContext().sendMail(listOwner, members, message);
        } catch (IOException ioe) {
            throw new MailetException("Error creating listserv message", ioe);
        } finally {
            //Kill the old message
            mail.setState(Mail.GHOST);
        }
    }

    /**
     * Add the footer using {@link CommandListservFooter}
     * @param mail
     * @throws MessagingException
     */
    protected void addFooter(Mail mail) throws MessagingException {
        getCommandListservFooter().service(mail);
    }

    protected void setSubject(MimeMessage message) throws MessagingException {
        String prefix = subjectPrefix;
        if (prefix != null) {
            if (autoBracket) {
                StringBuffer prefixBuffer =
                        new StringBuffer(64)
                        .append("[")
                        .append(prefix)
                        .append("]");
                prefix = prefixBuffer.toString();
            }
            String subj = message.getSubject();
            if (subj == null) {
                subj = "";
            }
            subj = normalizeSubject(subj, prefix);
            message.setSubject(subj);
        }
    }

    /**
     * Create a new message with some set headers
     * @param mail
     * @param listservAddr
     * @return a prepared List Message
     * @throws MessagingException
     */
    protected MimeMessage prepareListMessage(Mail mail, MailAddress listservAddr) throws MessagingException {
        //Create a copy of this message to send out
        MimeMessage message = new MimeMessage(mail.getMessage());

        //We need tao remove this header from the copy we're sending around
        message.removeHeader(RFC2822Headers.RETURN_PATH);

        //We're going to set this special header to avoid bounces
        //  getting sent back out to the list
        message.setHeader("X-been-there", listservAddr.toString());

        //If replies should go to this list, we need to set the header
        if (replyToList) {
            message.setHeader(RFC2822Headers.REPLY_TO, listservAddr.toString());
        }

        return message;
    }

    /**
     * return true if this is ok, false otherwise
     * Check if the X-been-there header is set to the listserv's name
     * (the address).  If it has, this means it's a message from this
     * listserv that's getting bounced back, so we need to swallow it
     *
     * @param listservAddr
     * @param mail
     * @return true if this message has already bounced, false otherwse
     * @throws MessagingException
     */
    protected boolean checkBeenThere(MailAddress listservAddr, Mail mail) throws MessagingException {
        if (listservAddr.equals(mail.getMessage().getHeader("X-been-there"))) {
            return false;
        }
        return true;
    }

    /**
     * Returns true if this is ok to send to the list
     * @param mail
     * @return true if this message is ok, false otherwise
     * @throws IOException
     * @throws MessagingException
     */
    protected boolean checkAnnouncements(Mail mail) throws IOException, MessagingException {
        if (!attachmentsAllowed && mail.getMessage().getContent() instanceof MimeMultipart) {
            Properties standardProperties = getCommandListservManager().getStandardProperties();

            getCommandListservManager().onError(mail,
                    xmlResources.getString("invalid.mail.subject", standardProperties),
                    xmlResources.getString("error.attachments", standardProperties));
            return false;
        }
        return true;
    }

    /**
     * Returns true if this user is ok to send to the list
     *
     * @param members
     * @param mail
     * @return true if this message is ok, false otherwise
     * @throws MessagingException
     */
    protected boolean checkMembers(Collection members, Mail mail) throws MessagingException {
        if (membersOnly && !members.contains(mail.getSender())) {
            Properties standardProperties = getCommandListservManager().getStandardProperties();
            getCommandListservManager().onError(mail,
                    xmlResources.getString("invalid.mail.subject", standardProperties),
                    xmlResources.getString("error.membersonly", standardProperties));

            return false;
        }
        return true;
    }

    public Collection getMembers() throws ParseException {
        Collection reply = new Vector();
        for (Iterator it = usersRepository.list(); it.hasNext();) {
            String member = it.next().toString();
            try {
                reply.add(new MailAddress(member));
            } catch (Exception e) {
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
     * Get a configuration value
     * @param attrName
     * @param defValue
     * @return the value if found, defValue otherwise
     */
    protected boolean getBoolean(String attrName, boolean defValue) {
        boolean value = defValue;
        try {
            value = new Boolean(getInitParameter(attrName)).booleanValue();
        } catch (Exception e) {
            // Ignore any exceptions, default to false
        }
        return value;
    }

    /**
     * Get a configuration value
     * @param attrName
     * @param defValue
     * @return the attrValue if found, defValue otherwise
     */
    protected String getString(String attrName, String defValue) {
        String value = defValue;
        try {
            value = getInitParameter(attrName);
        } catch (Exception e) {
            // Ignore any exceptions, default to false
        }
        return value;
    }

    /**
     * initialize the resources
     * @throws Exception
     */
    protected void initializeResources() throws Exception {
        xmlResources = getCommandListservManager().initXMLResources(new String[]{"List Manager"})[0];
    }

    /**
     * Fetch the repository of users
     */
    protected void initUsersRepository() throws Exception {
        ComponentManager compMgr = (ComponentManager) getMailetContext().getAttribute(Constants.AVALON_COMPONENT_MANAGER);
        UsersStore usersStore = (UsersStore) compMgr.lookup("org.apache.james.services.UsersStore");
        String repName = getInitParameter("repositoryName");

        usersRepository = usersStore.getRepository(repName);
        if (usersRepository == null) throw new Exception("Invalid user repository: " + repName);
    }

    /**
     * init the regexp
     */
    protected void initRegExp() throws Exception {
        StringBuffer regExp = new StringBuffer();
        if (autoBracket) {
            regExp.append("\\[");
        }
        if (subjectPrefix != null) {
            regExp.append(subjectPrefix);
        }
        if (autoBracket) {
            regExp.append("\\]");
        }
        if (subjectPrefix != null) {
            regExp.append("|");
        }
        regExp.append("re:");

        pattern = new Perl5Compiler().compile(regExp.toString(), Perl5Compiler.CASE_INSENSITIVE_MASK);
    }

    protected String normalizeSubject(String subject, String prefix) {
        if (subject == null) {
            subject = "";
        }

        boolean hasMatch = (subject.indexOf(subjectPrefix) != -1);
        String result = Util.substitute(new Perl5Matcher(),
                pattern,
                new StringSubstitution(""),
                subject,
                Util.SUBSTITUTE_ALL).trim();

        if (hasMatch) {
            return prefix + " RE: " + result;
        } else {
            return prefix + " " + result;
        }
    }

    /**
     * lazy retrieval
     * @return ICommandListservManager
     */
    protected ICommandListservManager getCommandListservManager() {
        if (commandListservManager == null) {
            commandListservManager = (ICommandListservManager) getMailetContext().getAttribute(ICommandListservManager.ID + listName);
            if (commandListservManager == null) {
                throw new IllegalStateException("Unable to find command list manager named: " + listName);
            }
        }

        return commandListservManager;
    }

    /**
     * Lazy init
     * @throws MessagingException
     */
    protected CommandListservFooter getCommandListservFooter() throws MessagingException {
        if (commandListservFooter == null) {
            commandListservFooter = new CommandListservFooter(getCommandListservManager());
            commandListservFooter.init(getMailetConfig());
        }
        return commandListservFooter;
    }
}
