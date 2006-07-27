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



package org.apache.james.transport.mailets;

import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.james.util.XMLResources;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;
import org.apache.oro.text.regex.StringSubstitution;
import org.apache.oro.text.regex.Util;

import javax.mail.MessagingException;


/**
 * CommandListservFooter is based on the AddFooter mailet.
 *
 * It is used by the {@link CommandListservProcessor} to inject a footer into mailing list.
 * <br />
 * <br />
 *
 * @version CVS $Revision$ $Date$
 * @since 2.2.0
 * @see XMLResources
 */
public class CommandListservFooter extends AbstractAddFooter {

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
     * @see XMLResources#getString
     * @param index either {@link #TEXT_PLAIN} or {@link #TEXT_HTML}
     * @return a formatted text with the proper list and domain
     */
    protected String getFormattedText(int index) {
        return xmlResources[index].getString("text");
    }
}
