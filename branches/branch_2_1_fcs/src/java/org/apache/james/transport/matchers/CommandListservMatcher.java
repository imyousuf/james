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
package org.apache.james.transport.matchers;

import org.apache.mailet.GenericRecipientMatcher;
import org.apache.mailet.MailAddress;

import javax.mail.MessagingException;

/**
 * CommandListservMatcher is the matcher that pairs with the {@link org.apache.james.transport.mailets.CommandListservManager}
 * It checks to see if the request is intended for the ListservManager, but doesn't guarantee that it is a valid command.
 * <br />
 * To configure, insert this into the config.xml inside of the root processor block.
 * <pre>
 * &lt;mailet match="CommandListservMatcher=announce@localhost" class="CommandListservManager"&gt;
 * ...
 * &lt;/mailet&gt;
 * </pre>
 *
 * @version CVS $Revision: 1.1.2.2 $ $Date: 2003/07/06 11:53:56 $
 * @since 2.2.0
 * @see org.apache.james.transport.mailets.CommandListservManager
 */
public class CommandListservMatcher extends GenericRecipientMatcher {

    private MailAddress listservAddress;

    public void init() throws MessagingException {
        listservAddress = new MailAddress(getCondition());
    }

    /**
     * This doesn't perform an exact match, but checks to see if the request is at lesast
     * intended to go to the list serv manager.
     * @param recipient
     * @return true if matches, false otherwise
     */
    public boolean matchRecipient(MailAddress recipient) {
        if (recipient.getHost().equals(listservAddress.getHost())) {
            if (recipient.getUser().startsWith(listservAddress.getUser() + "-")) {
                return true;
            }
        }
        return false;
    }
}
