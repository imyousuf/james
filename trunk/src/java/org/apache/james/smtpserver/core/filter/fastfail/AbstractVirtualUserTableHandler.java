/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.smtpserver.core.filter.fastfail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;

import javax.mail.internet.ParseException;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.smtpserver.CommandHandler;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.util.VirtualUserTableUtil;
import org.apache.mailet.MailAddress;
import org.apache.oro.text.regex.MalformedPatternException;

/**
 * Provides an abstraction of common functionality needed for implementing
 * a Virtual User Table. Override the <code>mapRecipients</code> method to
 * map virtual recipients to real recipients.
 */
public abstract class AbstractVirtualUserTableHandler extends AbstractLogEnabled
    implements CommandHandler {
    
    public final static String VALID_USER = "VALID_USER";
    
    /**
     * @see org.apache.james.smtpserver.CommandHandler#onCommand(SMTPSession)
     */
    public void onCommand(SMTPSession session) {
        MailAddress rcpt = (MailAddress) session.getState().get(SMTPSession.CURRENT_RECIPIENT);

        // remove old state
        session.getState().remove(VALID_USER);
    
        String targetString = mapRecipients(rcpt);

        // Only non-null mappings are translated
        if (targetString != null) {
            if (targetString.startsWith("error:")) {
            // write back the error
            session.writeResponse(targetString.substring("error:".length()));
                session.setStopHandlerProcessing(true);
                return;
            } else {
                StringTokenizer tokenizer = new StringTokenizer(targetString,
                VirtualUserTableUtil.getSeparator(targetString));

                while (tokenizer.hasMoreTokens()) {
                    String targetAddress = tokenizer.nextToken().trim();


                    if (targetAddress.startsWith("regex:")) {   
                        try {
                            targetAddress = VirtualUserTableUtil.regexMap(rcpt, targetAddress);
                        } catch (MalformedPatternException e) {
                            getLogger().error("Exception during regexMap processing: ", e);
                        }


                        if (targetAddress == null)
                            continue;
                        }

                        try {
                            MailAddress target = (targetAddress.indexOf('@') < 0) ? new MailAddress(targetAddress, "localhost")
                                : new MailAddress(targetAddress);

                            session.getState().put(VALID_USER, target);

    
                            StringBuffer buf = new StringBuffer().append(
                                "Valid virtual user mapping ").append(rcpt)
                                .append(" to ").append(target);
                            getLogger().debug(buf.toString());
    

                        } catch (ParseException pe) {
                            //Don't map this address... there's an invalid address mapping here
                            StringBuffer exceptionBuffer = new StringBuffer(128)
                                .append("There is an invalid map from ")
                                .append(rcpt).append(" to ").append(
                                targetAddress);
                            getLogger().error(exceptionBuffer.toString());
                            continue;
                        }
                   }
               }
           }
      }

    /**
     * Override to map virtual recipients to real recipients, both local and non-local.
     * Each key in the provided map corresponds to a potential virtual recipient, stored as
     * a <code>MailAddress</code> object.
     * 
     * Translate virtual recipients to real recipients by mapping a string containing the
     * address of the real recipient as a value to a key. Leave the value <code>null<code>
     * if no mapping should be performed. Multiple recipients may be specified by delineating
     * the mapped string with commas, semi-colons or colons.
     * 
     * @param recipientsMap the mapping of virtual to real recipients, as 
     *    <code>MailAddress</code>es to <code>String</code>s.
     */
    protected abstract String mapRecipients(MailAddress recipient);
    
    public Collection getImplCommands() {
        Collection c = new ArrayList();
        c.add("RCPT");
    
        return c;
    }
}
