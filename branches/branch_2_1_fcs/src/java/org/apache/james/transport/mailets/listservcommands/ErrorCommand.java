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
package org.apache.james.transport.mailets.listservcommands;

import org.apache.james.transport.mailets.ICommandListservManager;
import org.apache.james.util.XMLResources;
import org.apache.mailet.Mail;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;

import javax.mail.MessagingException;
import java.util.Properties;

/**
 * Error handles the error command.
 * It is configured by:
 * <pre>&lt;command name="error" class="ErrorCommand"/&gt;</pre>
 *
 * <br />
 * <br />
 *
 * It uses the formatted text-based resources for its return mail body:
 * <ul>
 *  <li>header
 *  <li>error
 *  <li>admincommands
 * </ul>
 *
 * <br />
 * <br />
 * After formatting the text, the message is delivered with {@link #sendStandardReply}
 *
 * @version CVS $Revision: 1.1.2.2 $ $Date: 2003/07/06 11:53:56 $
 * @since 2.2.0
 */
public class ErrorCommand extends BaseCommand {

    //For resources
    protected XMLResources[] xmlResources;

    protected static final int HEADER = 0;
    protected static final int ERROR = 1;
    protected static final int ADMIN_COMMANDS = 2;

    public void init(ICommandListservManager commandListservManager, Configuration configuration) throws ConfigurationException {
        super.init(commandListservManager, configuration);
        xmlResources = initXMLResources(new String[]{"header", "error", "admincommands"});
    }

    /**
     * Delegate to {@link #onError}
     * @param mail
     * @throws MessagingException
     */
    public void onCommand(Mail mail) throws MessagingException {
        onError(mail, "an unkown error occurred", "an unkown error occurred");
    }

    /**
     * An error occurred, send a message with the following text resources:
     * <ul>
     *  <li>{@link #HEADER}
     *  <li>{@link #ERROR}
     *  <li>{@link #ADMIN_COMMANDS}
     * </ul>
     *
     * @param subject the subject of the message to send
     * @param mail
     * @param errorMessage
     */
    public void onError(Mail mail, String subject, String errorMessage) throws MessagingException {

        Properties props = getStandardProperties();
        props.put("ERROR_MESSAGE", errorMessage);

        StringBuffer plainTextMessage = new StringBuffer();
        String header = xmlResources[HEADER].getString("text", props);
        plainTextMessage.append(header);

        String errorText = xmlResources[ERROR].getString("text", props);
        plainTextMessage.append(errorText);

        String adminCommands = xmlResources[ADMIN_COMMANDS].getString("text", props);
        plainTextMessage.append(adminCommands);

        sendStandardReply(mail, subject, plainTextMessage.toString(), null);
    }
}
