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

import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.james.util.XMLResources;
import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.oro.text.regex.*;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;
import java.io.IOException;


/**
 * CommandListservFooter is based on the AddFooter mailet.
 *
 * It is used by the {@link CommandListservProcessor} to inject a footer into mailing list.
 * <br />
 * <br />
 * @see XMLResources
 */
public class CommandListservFooter extends GenericMailet {

    protected String footerText;
    protected String footerHtml;

    /**
     * The list serv manager
     */
    protected ICommandListservManager commandListservManager;

    /**
     * For matching
     */
    protected Perl5Compiler perl5Compiler = new Perl5Compiler();
    protected Pattern insertPattern;
    protected Pattern newlinePattern;

    //For resources
    protected XMLResources[] xmlResources = new XMLResources[2];

    protected static final int TEXT_PLAIN = 0;
    protected static final int TEXT_HTML = 1;

    public CommandListservFooter(ICommandListservManager commandListservManager) {
        this.commandListservManager = commandListservManager;
        try {
            insertPattern = perl5Compiler.compile("</body>\\s*</html>", Perl5Compiler.CASE_INSENSITIVE_MASK);
            newlinePattern = perl5Compiler.compile("\r\n|\n", Perl5Compiler.CASE_INSENSITIVE_MASK);
        } catch (MalformedPatternException e) {
            throw new IllegalStateException("Unable to parse regexps: " + e.getMessage());
        }
    }

    /**
     * Initialize the mailet
     */
    public void init() throws MessagingException {
        try {
            xmlResources = commandListservManager.initXMLResources(new String[]{"footer", "footer_html"});
        } catch (ConfigurationException e) {
            throw new MessagingException(e.getMessage(), e);
        }
    }

    /**
     * Return a string describing this mailet.
     *
     * @return a string describing this mailet
     */
    public String getMailetInfo() {
        return "CommandListservFooter Mailet";
    }


    /**
     * Identify what type of mimeMessage it is, and attach the footer
     * @param mail
     * @throws MessagingException
     */
    public void service(Mail mail) throws MessagingException {
        try {
            MimeMessage message = mail.getMessage();

            //I want to modify the right message body
            if (message.isMimeType("text/plain")) {
                //This is a straight text message... just append the single part normally
                addToText(message);
            } else if (message.isMimeType("multipart/mixed")) {
                //Find the first body part, and determine what to do then.
                MimeMultipart multipart = (MimeMultipart) message.getContent();
                MimeBodyPart part = (MimeBodyPart) multipart.getBodyPart(0);
                attachFooter(part);
                //We have to do this because of a bug in JavaMail (ref id 4404733)
                message.setContent(multipart);
            } else {
                //Find the HTML and text message types and add to each
                MimeMultipart multipart = (MimeMultipart) message.getContent();
                int count = multipart.getCount();
                for (int index = 0; index < count; index++) {
                    MimeBodyPart part = (MimeBodyPart) multipart.getBodyPart(index);
                    attachFooter(part);
                }
                //We have to do this because of a bug in JavaMail (ref id 4404733)
                message.setContent(multipart);
            }
        } catch (IOException ioe) {
            throw new MessagingException("Could not read message", ioe);
        }
    }

    /**
     * Get and cache the footer text
     *
     * @return the footer text
     * @see XMLResources
     */
    protected String getFooterText() {
        if (footerText == null) {
            footerText = getFormattedText(TEXT_PLAIN);
        }
        return footerText;
    }

    /**
     * Get and cache the footer html text
     *
     * @return the footer text
     * @see XMLResources
     */
    protected String getFooterHTML() {
        if (footerHtml == null) {
            String footerText = getFormattedText(TEXT_HTML);
            footerHtml = Util.substitute(new Perl5Matcher(),
                    newlinePattern,
                    new StringSubstitution(" <br />"),
                    footerText,
                    Util.SUBSTITUTE_ALL);
        }
        return footerHtml;
    }

    /**
     * Prepends the content of the MimePart as HTML to the existing footer.
     * We use the regular expression to inject the footer inside of the body tag appropriately.
     *
     * @param part the MimePart to attach
     *
     * @throws MessagingException
     * @throws java.io.IOException
     */
    protected void addToHTML(MimePart part) throws MessagingException, IOException {
        String content = part.getContent().toString();
        StringSubstitution stringSubstitution = new StringSubstitution("<br />" + getFooterHTML() + "</body</html>");
        String result = Util.substitute(new Perl5Matcher(), insertPattern, stringSubstitution, content, 1);
        part.setContent(result, part.getContentType());
    }

    /**
     * Prepends the content of the MimePart as text to the existing footer
     *
     * @param part the MimePart to attach
     *
     * @throws MessagingException
     * @throws IOException
     */
    protected void addToText(MimePart part) throws MessagingException, IOException {
        String content = part.getContent().toString();
        if (!content.endsWith("\n")) {
            content += "\r\n";
        }
        content += getFooterText();
        part.setText(content);
    }

    /**
     * Attaches a MimePart as an appropriate footer
     *
     * @param part the MimePart to attach
     *
     * @throws MessagingException
     * @throws IOException
     */
    protected void attachFooter(MimePart part) throws MessagingException, IOException {
        if (part.isMimeType("text/plain")) {
            addToText(part);
        } else if (part.isMimeType("text/html")) {
            addToHTML(part);
        } else if (part.getContent() instanceof MimeMultipart) {
            MimeMultipart multipart = (MimeMultipart) part.getContent();
            int count = multipart.getCount();
            for (int index = 0; index < count; index++) {
                MimeBodyPart mimeBodyPart = (MimeBodyPart) multipart.getBodyPart(index);
                attachFooter(mimeBodyPart);
            }
            part.setContent(multipart);
        } else {
            //System.err.println(part.getContentType());
        }
    }

    /**
     * @see XMLResources#getString
     * @param index either {@link #TEXT_PLAIN} or {@link #TEXT_HTML}
     * @return a formatted text with the proper list and domain
     */
    protected String getFormattedText(int index) {
        return xmlResources[index].getString("text");
    }
}
