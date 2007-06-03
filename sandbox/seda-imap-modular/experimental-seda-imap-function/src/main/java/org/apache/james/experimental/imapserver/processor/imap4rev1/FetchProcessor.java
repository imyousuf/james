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

package org.apache.james.experimental.imapserver.processor.imap4rev1;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapConstants;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.ProtocolException;
import org.apache.james.api.imap.message.BodyFetchElement;
import org.apache.james.api.imap.message.FetchData;
import org.apache.james.api.imap.message.IdRange;
import org.apache.james.api.imap.message.MessageFlags;
import org.apache.james.core.MimeMessageWrapper;
import org.apache.james.experimental.imapserver.AuthorizationException;
import org.apache.james.experimental.imapserver.ImapSession;
import org.apache.james.experimental.imapserver.message.request.ImapRequest;
import org.apache.james.experimental.imapserver.message.request.imap4rev1.FetchRequest;
import org.apache.james.experimental.imapserver.message.response.ImapResponseMessage;
import org.apache.james.experimental.imapserver.message.response.imap4rev1.legacy.FetchResponse;
import org.apache.james.experimental.imapserver.processor.ImapProcessor;
import org.apache.james.experimental.imapserver.processor.base.AbstractImapRequestProcessor;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.imapserver.store.SimpleMessageAttributes;
import org.apache.james.mailboxmanager.GeneralMessageSet;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.impl.GeneralMessageSetImpl;
import org.apache.james.mailboxmanager.mailbox.ImapMailboxSession;
import org.apache.mailet.dates.RFC822DateFormat;

import com.sun.mail.util.CRLFOutputStream;


public class FetchProcessor extends AbstractImapRequestProcessor {
	
	public FetchProcessor(final ImapProcessor next) {
        super(next);
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof FetchRequest);
    }

    protected ImapResponseMessage doProcess(ImapRequest message, ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
        final FetchRequest request = (FetchRequest) message;
        final ImapResponseMessage result = doProcess(request, session, tag, command);
		return result;
	}

	private ImapResponseMessage doProcess(FetchRequest request, ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
        final boolean useUids = request.isUseUids();
        final IdRange[] idSet = request.getIdSet();
        final FetchData fetch = request.getFetch();
		final ImapResponseMessage result = doProcess(useUids, idSet, fetch, session, tag, command);
		return result;
	}
	
	private ImapResponseMessage doProcess(final boolean useUids, final IdRange[] idSet, final FetchData fetch,
			ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
        
        FetchResponse result = new FetchResponse(command, tag);
        boolean omitExpunged = (!useUids);
        
        // TODO only fetch needed results
        int resultToFetch = MessageResult.FLAGS | MessageResult.MIME_MESSAGE
                | MessageResult.INTERNAL_DATE | MessageResult.MSN
                | MessageResult.SIZE;
        ImapMailboxSession mailbox = session.getSelected().getMailbox();
        for (int i = 0; i < idSet.length; i++) {
            GeneralMessageSet messageSet=GeneralMessageSetImpl.range(idSet[i].getLowVal(),idSet[i].getHighVal(),useUids);
            MessageResult[] fetchResults;
            try {
                fetchResults = mailbox.getMessages(messageSet,resultToFetch);
            } catch (MailboxManagerException e) {
                throw new MailboxException(e);
            }
            for (int j = 0; j < fetchResults.length; j++) {
                String msgData = outputMessage( fetch, fetchResults[j], mailbox, useUids );
                // TODO: this is inefficient
                // TODO: stream output upon response
                result.addMessageData(fetchResults[j].getMsn(), msgData );
            }
        }
        List unsolicitedResponses = session.unsolicitedResponses(useUids);
        result.addUnsolicitedResponses(unsolicitedResponses);
        return result;
    }
    

    private String outputMessage(FetchData fetch, MessageResult result,
            ImapMailboxSession mailbox, boolean useUids)
            throws MailboxException, ProtocolException {
        // Check if this fetch will cause the "SEEN" flag to be set on this
        // message
        // If so, update the flags, and ensure that a flags response is included
        // in the response.
        try {
            boolean ensureFlagsResponse = false;
            if (fetch.isSetSeen()
                    && !result.getFlags().contains(Flags.Flag.SEEN)) {
                mailbox.setFlags(new Flags(Flags.Flag.SEEN), true, false,
                        GeneralMessageSetImpl.oneUid(result.getUid()), null);
                result.getFlags().add(Flags.Flag.SEEN);
                ensureFlagsResponse = true;
            }

            StringBuffer response = new StringBuffer();

            // FLAGS response
            if (fetch.isFlags() || ensureFlagsResponse) {
                response.append(" FLAGS ");
                response.append(MessageFlags.format(result.getFlags()));
            }

            // INTERNALDATE response
            if (fetch.isInternalDate()) {
                response.append(" INTERNALDATE \"");
                // TODO format properly
                response.append(RFC822DateFormat.toString(result
                        .getInternalDate())); // not right format
                response.append("\"");

            }

            // RFC822.SIZE response
            if (fetch.isSize()) {
                response.append(" RFC822.SIZE ");
                response.append(result.getSize());
            }

            SimpleMessageAttributes attrs = new SimpleMessageAttributes(result
                    .getMimeMessage(), getLogger());

            // ENVELOPE response
            if (fetch.isEnvelope()) {
                response.append(" ENVELOPE ");
                response.append(attrs.getEnvelope());
            }

            // BODY response
            if (fetch.isBody()) {
                response.append(" BODY ");
                response.append(attrs.getBodyStructure(false));
            }

            // BODYSTRUCTURE response
            if (fetch.isBodyStructure()) {
                response.append(" BODYSTRUCTURE ");
                response.append(attrs.getBodyStructure(true));
            }

            // UID response
            if (fetch.isUid()) {
                response.append(" UID ");
                response.append(result.getUid());
            }

            // BODY part responses.
            Collection elements = fetch.getBodyElements();
            for (Iterator iterator = elements.iterator(); iterator.hasNext();) {
                BodyFetchElement fetchElement = (BodyFetchElement) iterator
                        .next();
                response.append(ImapConstants.SP);
                response.append(fetchElement.getResponseName());
                response.append(ImapConstants.SP);

                // Various mechanisms for returning message body.
                String sectionSpecifier = fetchElement.getParameters();

                MimeMessage mimeMessage = result.getMimeMessage();
                try {
                    handleBodyFetch(mimeMessage, sectionSpecifier, response);
                } catch (MessagingException e) {
                    throw new MailboxException(e.getMessage(), e);
                }
            }

            if (response.length() > 0) {
                // Remove the leading " ".
                return response.substring(1);
            } else {
                return "";
            }
        } catch (MailboxManagerException mme) {
            throw new MailboxException(mme);
        } catch (MessagingException me) {
            throw new MailboxException(me);       
        }
    }
    
    private void handleBodyFetch( MimeMessage mimeMessage,
                                  String sectionSpecifier,
                                  StringBuffer response )
            throws ProtocolException, MessagingException
    {
        if ( sectionSpecifier.length() == 0 ) {
            // TODO - need to use an InputStream from the response here.
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            try {
                mimeMessage.writeTo(new CRLFOutputStream(bout));
            }
            catch ( IOException e ) {
                throw new ProtocolException( "Error reading message source", e);
            }
            byte[] bytes = bout.toByteArray();
            addLiteral( bytes, response );
            // TODO JD maybe we've to add CRLF here
            
        }
        else if ( sectionSpecifier.equalsIgnoreCase( "HEADER" ) ) {
            Enumeration e = mimeMessage.getAllHeaderLines();
            addHeaders( e, response );
        }
        else if ( sectionSpecifier.startsWith( "HEADER.FIELDS.NOT " ) ) {
            String[] excludeNames = extractHeaderList( sectionSpecifier, "HEADER.FIELDS.NOT ".length() );
            Enumeration e = mimeMessage.getNonMatchingHeaderLines( excludeNames );
            addHeaders( e, response );
        }
        else if ( sectionSpecifier.startsWith( "HEADER.FIELDS " ) ) {
            String[] includeNames = extractHeaderList( sectionSpecifier, "HEADER.FIELDS ".length() );
            Enumeration e = mimeMessage.getMatchingHeaderLines( includeNames );
            addHeaders( e, response );
        }
        else if ( sectionSpecifier.equalsIgnoreCase( "MIME" ) ) {
            // TODO implement
            throw new ProtocolException( "MIME not yet implemented." );
        }
        else if ( sectionSpecifier.equalsIgnoreCase( "TEXT" ) ) {
            // TODO - need to use an InputStream from the response here.
            // TODO - this is a hack. To get just the body content, I'm using a null
            // input stream to take the headers. Need to have a way of ignoring headers.
            ByteArrayOutputStream headerOut = new ByteArrayOutputStream();
            ByteArrayOutputStream bodyOut = new ByteArrayOutputStream();
            try {
                // TODO James Trunk : Is this okay?
                MimeMessageWrapper mmw=new MimeMessageWrapper(mimeMessage);
                
                mmw.writeTo(headerOut, bodyOut );
                byte[] bytes = bodyOut.toByteArray();

                addLiteral( bytes, response );

            }
            catch ( IOException e ) {
                throw new ProtocolException( "Error reading message source", e);
            }
        }
        else {
            // Should be a part specifier followed by a section specifier.
            // See if there's a leading part specifier.
            // If so, get the number, get the part, and call this recursively.
            int dotPos = sectionSpecifier.indexOf( '.' );
            if ( dotPos == -1 ) {
                throw new ProtocolException( "Malformed fetch attribute: " + sectionSpecifier );
            }
            int partNumber = Integer.parseInt( sectionSpecifier.substring( 0, dotPos ) );
            String partSectionSpecifier = sectionSpecifier.substring( dotPos + 1 );

            // TODO - get the MimePart of the mimeMessage, and call this method
            // with the new partSectionSpecifier.
//        MimeMessage part;
//        handleBodyFetch( part, partSectionSpecifier, response );
            throw new ProtocolException( "Mime parts not yet implemented for fetch." );
        }

    }

    private void addLiteral( byte[] bytes, StringBuffer response )
    {
        response.append('{' );
        response.append( bytes.length ); // TODO JD addLiteral: why was it  bytes.length +1 here?
        response.append( '}' );
        response.append( "\r\n" );

        for ( int i = 0; i < bytes.length; i++ ) {
            byte b = bytes[i];
            response.append((char)b);
        }
    }

    // TODO should do this at parse time.
    private String[] extractHeaderList( String headerList, int prefixLen )
    {
        // Remove the trailing and leading ')('
        String tmp = headerList.substring( prefixLen + 1, headerList.length() - 1 );
        String[] headerNames = split( tmp, " " );
        return headerNames;
    }
    
    private String[] split(String value, String delimiter) {
        ArrayList strings = new ArrayList();
        int startPos = 0;
        int delimPos;
        while ( (delimPos = value.indexOf(delimiter, startPos) ) != -1) {
            String sub = value.substring(startPos, delimPos);
            strings.add(sub);
            startPos = delimPos + 1;
        }
        String sub = value.substring(startPos);
        strings.add(sub);
        
        return (String[]) strings.toArray(new String[0]);
    }

    private void addHeaders( Enumeration e, StringBuffer response )
    {
        List lines = new ArrayList();
        int count = 0;
        while (e.hasMoreElements()) {
            String line = (String)e.nextElement();
            count += line.length() + 2;
            lines.add(line);
        }
        response.append( '{' );
        response.append( count + 2 );
        response.append( '}' );
        response.append("\r\n");

        Iterator lit = lines.iterator();
        while (lit.hasNext()) {
            String line = (String)lit.next();
            response.append( line );
            response.append( "\r\n" );
        }
        response.append("\r\n");
    }
}
