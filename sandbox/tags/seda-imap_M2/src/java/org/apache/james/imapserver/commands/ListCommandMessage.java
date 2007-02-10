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
package org.apache.james.imapserver.commands;

import org.apache.james.imapserver.AuthorizationException;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.ProtocolException;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.mailboxmanager.ListResult;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.impl.ListResultImpl;

class ListCommandMessage extends AbstractImapCommandMessage {
    private final String baseReferenceName;
    private final String mailboxPattern;

    public ListCommandMessage(final ImapCommand command, final String referenceName, final String mailboxPattern,
            final String tag) {
        super(tag, command);
        this.baseReferenceName = referenceName;
        this.mailboxPattern = mailboxPattern;
    }
    
    protected ImapResponseMessage doProcess(ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {

        final ListResponseMessage result = new ListResponseMessage(command, tag);
        String referenceName = this.baseReferenceName;
        // Should the #user.userName section be removed from names returned?
        boolean removeUserPrefix;

        ListResult[] listResults;

        String personalNamespace = ListCommand.USER_NAMESPACE + ListCommand.HIERARCHY_DELIMITER_CHAR +
        session.getUser().getUserName();

        if ( mailboxPattern.length() == 0 ) {
            // An empty mailboxPattern signifies a request for the hierarchy delimiter
            // and root name of the referenceName argument

            String referenceRoot;
            if ( referenceName.startsWith( ListCommand.NAMESPACE_PREFIX ) )
            {
                // A qualified reference name - get the first element,
                // and don't remove the user prefix
                removeUserPrefix = false;
                int firstDelimiter = referenceName.indexOf( ListCommand.HIERARCHY_DELIMITER_CHAR );
                if ( firstDelimiter == -1 ) {
                    referenceRoot = referenceName;
                }
                else {
                    referenceRoot = referenceName.substring(0, firstDelimiter );
                }
            }
            else {
                // A relative reference name - need to remove user prefix from results.
                referenceRoot = "";
                removeUserPrefix = true;

            }

            // Get the mailbox for the reference name.
            listResults = new ListResult[1];
            listResults[0]=new ListResultImpl(referenceRoot,ListCommand.HIERARCHY_DELIMITER);
        }
        else {

            // If the mailboxPattern is fully qualified, ignore the
            // reference name.
            if ( mailboxPattern.charAt( 0 ) == ListCommand.NAMESPACE_PREFIX_CHAR ) {
                referenceName="";
            }

            // If the search pattern is relative, need to remove user prefix from results.
            removeUserPrefix = ( (referenceName+mailboxPattern).charAt(0) != ListCommand.NAMESPACE_PREFIX_CHAR );

            if (removeUserPrefix) {
                referenceName=personalNamespace+"."+referenceName;
            }

            listResults = doList( session, referenceName, mailboxPattern );
        }


        int prefixLength = personalNamespace.length();

        for (int i = 0; i < listResults.length; i++) {
            StringBuffer message = new StringBuffer( "(" );
            String[] attrs=listResults[i].getAttributes();
            for (int j = 0; j < attrs.length; j++) {
                if (j > 0) {
                    message.append(' ');
                }
                message.append( attrs[j] );
            }
            message.append( ") \"" );
            message.append( listResults[i].getHierarchyDelimiter() );
            message.append( "\" " );

            String mailboxName = listResults[i].getName();
            if ( removeUserPrefix ) {
                if ( mailboxName.length() <= prefixLength ) {
                    mailboxName = "";
                }
                else {
                    mailboxName = mailboxName.substring( prefixLength + 1 );
                }
            }

            // TODO: need to check if the mailbox name needs quoting.
            if ( mailboxName.length() == 0 ) {
                message.append("\"\"");
            }
            else {
                message.append( mailboxName );
            }

            result.addMessageData( message.toString() );
        }
        return result;
    }
    
    protected ListResult[] doList( ImapSession session, String base, String pattern ) throws MailboxException {
        return doList(  session,  base,  pattern, false);
    }
    
    protected ListResult[] doList( ImapSession session, String base, String pattern, boolean subscribed ) throws MailboxException
    {
        try {
            return session.getMailboxManager().list(base,pattern,false);
        } catch (MailboxManagerException e) {
            throw new MailboxException(e);  
        }
    }
}
