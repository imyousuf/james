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

package org.apache.james.transport;
import java.util.Vector;

import javax.mail.MessagingException;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.james.core.MatcherConfigImpl;
import org.apache.mailet.MailetContext;
import org.apache.mailet.MailetException;
import org.apache.mailet.Matcher;
/**
 * Loads Matchers for use inside James.
 *
 */
public class MatchLoader extends Loader implements Configurable {
        /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration conf) throws ConfigurationException {
           getPackages(conf,MATCHER_PACKAGE);
           configureMailetClassLoader();
    }


    /**
     * Get a new Matcher with the specified name acting
     * in the specified context.
     *
     * @param matchName the name of the matcher to be loaded
     * @param context the MailetContext to be passed to the new
     *                matcher
     * @throws MessagingException if an error occurs
     */
    public Matcher getMatcher(String matchName, MailetContext context) throws MessagingException {
        try {
            String condition = (String) null;
            int i = matchName.indexOf('=');
            if (i != -1) {
                condition = matchName.substring(i + 1);
                matchName = matchName.substring(0, i);
            }
            for (i = 0; i < packages.size(); i++) {
                String className = (String) packages.elementAt(i) + matchName;
                try {
                    MatcherConfigImpl configImpl = new MatcherConfigImpl();
                    configImpl.setMatcherName(matchName);
                    configImpl.setCondition(condition);
                    configImpl.setMailetContext(context);
                    Matcher matcher = (Matcher) mailetClassLoader.loadClass(className).newInstance();
                    matcher.init(configImpl);
                    return matcher;
                } catch (ClassNotFoundException cnfe) {
                    //do this so we loop through all the packages
                }
            }
            StringBuffer exceptionBuffer =
                new StringBuffer(128)
                    .append("Requested matcher not found: ")
                    .append(matchName)
                    .append(".  looked in ")
                    .append(packages.toString());
            throw new ClassNotFoundException(exceptionBuffer.toString());
        } catch (MessagingException me) {
            throw me;
        } catch (Exception e) {
            StringBuffer exceptionBuffer =
                new StringBuffer(128).append("Could not load matcher (").append(matchName).append(
                    ")");
            throw new MailetException(exceptionBuffer.toString(), e);
        }
    }

}
