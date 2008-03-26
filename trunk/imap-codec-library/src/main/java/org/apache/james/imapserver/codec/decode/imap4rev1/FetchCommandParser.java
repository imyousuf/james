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
package org.apache.james.imapserver.codec.decode.imap4rev1;

import java.util.List;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.ProtocolException;
import org.apache.james.api.imap.imap4rev1.Imap4Rev1CommandFactory;
import org.apache.james.api.imap.imap4rev1.Imap4Rev1MessageFactory;
import org.apache.james.api.imap.message.BodyFetchElement;
import org.apache.james.api.imap.message.FetchData;
import org.apache.james.api.imap.message.IdRange;
import org.apache.james.imapserver.codec.decode.FetchPartPathDecoder;
import org.apache.james.imapserver.codec.decode.ImapRequestLineReader;
import org.apache.james.imapserver.codec.decode.InitialisableCommandFactory;

class FetchCommandParser extends AbstractUidCommandParser  implements InitialisableCommandFactory
{
    public FetchCommandParser() {
    }

    /**
     * @see org.apache.james.imapserver.codec.decode.InitialisableCommandFactory#init(org.apache.james.api.imap.imap4rev1.Imap4Rev1CommandFactory)
     */
    public void init(Imap4Rev1CommandFactory factory)
    {
        final ImapCommand command = factory.getFetch();
        setCommand(command);
    }

    public FetchData fetchRequest( ImapRequestLineReader request )
    throws ProtocolException
    {
        FetchData fetch = new FetchData();

        char next = nextNonSpaceChar( request );
        if (request.nextChar() == '(') {
            consumeChar( request, '(' );

            next = nextNonSpaceChar( request );
            while ( next != ')' ) {
                addNextElement( request, fetch );
                next = nextNonSpaceChar( request );
            }
            consumeChar(request, ')');
        } else {
            addNextElement( request, fetch );

        }

        return fetch;
    }

    private void addNextElement( ImapRequestLineReader reader, FetchData fetch)
    throws ProtocolException
    {   
        //String name = element.toString();
        String name = readWord(reader, " [)\r\n");
        char next = reader.nextChar();
        // Simple elements with no '[]' parameters.
        if (next != '[') {
            if ( "FAST".equalsIgnoreCase( name ) ) {
                fetch.setFlags(true);
                fetch.setInternalDate(true);
                fetch.setSize(true);
            } else if ("FULL".equalsIgnoreCase(name)) {
                fetch.setFlags(true);
                fetch.setInternalDate(true);
                fetch.setSize(true);
                fetch.setEnvelope(true);
                fetch.setBody(true);
            } else if ("ALL".equalsIgnoreCase(name)) {
                fetch.setFlags(true);
                fetch.setInternalDate(true);
                fetch.setSize(true);
                fetch.setEnvelope(true);
            } else if ("FLAGS".equalsIgnoreCase(name)) {
                fetch.setFlags(true);
            } else if ("RFC822.SIZE".equalsIgnoreCase(name)) {
                fetch.setSize(true);
            } else if ("ENVELOPE".equalsIgnoreCase(name)) {
                fetch.setEnvelope(true);
            } else if ("INTERNALDATE".equalsIgnoreCase(name)) {
                fetch.setInternalDate(true);
            } else if ("BODY".equalsIgnoreCase(name)) {
                fetch.setBody(true);
            } else if ("BODYSTRUCTURE".equalsIgnoreCase(name)) {
                fetch.setBodyStructure(true);
            } else if ("UID".equalsIgnoreCase(name)) {
                fetch.setUid(true);
            } else if ("RFC822".equalsIgnoreCase(name)) {
                fetch.add(BodyFetchElement.createRFC822(), false);
            } else if ("RFC822.HEADER".equalsIgnoreCase(name)) {
                fetch.add(BodyFetchElement.createRFC822Header(), true);
            } else if ("RFC822.TEXT".equalsIgnoreCase(name)) {
                fetch.add(BodyFetchElement.createRFC822Text(), false);
            } else {
                throw new ProtocolException( "Invalid fetch attribute: " + name );
            }
        }
        else {
            consumeChar( reader, '[' );

            String parameter = readWord(reader, "]");

            consumeChar( reader, ']');

            final Long firstOctet;
            final Long numberOfOctets;
            if(reader.nextChar() == '<') {
                consumeChar(reader, '<');
                firstOctet = new Long(number(reader));
                if (reader.nextChar() == '.') {
                    consumeChar(reader, '.');
                    numberOfOctets = new Long(nzNumber(reader));
                } else {
                    numberOfOctets = null;
                }
                consumeChar(reader, '>');
            } else {
                firstOctet = null;
                numberOfOctets = null;
            }
            
            
            final BodyFetchElement bodyFetchElement 
                = createBodyElement(parameter, firstOctet, numberOfOctets);
            final boolean isPeek = isPeek(name);
            fetch.add(bodyFetchElement, isPeek);
        }
    }

    private boolean isPeek(String name) throws ProtocolException {
        final boolean isPeek;
        if ( "BODY".equalsIgnoreCase( name ) ) {
            isPeek = false;
        } else if ( "BODY.PEEK".equalsIgnoreCase( name ) ) {
            isPeek = true;
        } else {
            throw new ProtocolException( "Invalid fetch attibute: " + name + "[]" );
        }
        return isPeek;
    }

    private BodyFetchElement createBodyElement(String parameter, Long firstOctet, Long numberOfOctets) throws ProtocolException {
        final String responseName = "BODY[" + parameter + "]";
        FetchPartPathDecoder decoder = new FetchPartPathDecoder();
        decoder.decode(parameter);
        final int sectionType = getSectionType(decoder);

        final List names = decoder.getNames();
        final int[] path = decoder.getPath();
        final BodyFetchElement bodyFetchElement = new BodyFetchElement(responseName, sectionType, path, names, firstOctet, numberOfOctets);
        return bodyFetchElement;
    }

    private int getSectionType(FetchPartPathDecoder decoder) throws ProtocolException {
        final int specifier = decoder.getSpecifier();
        final int sectionType;
        switch (specifier) {
            case FetchPartPathDecoder.CONTENT:
                sectionType = BodyFetchElement.CONTENT;
                break;
            case FetchPartPathDecoder.HEADER:
                sectionType = BodyFetchElement.HEADER;
                break;
            case FetchPartPathDecoder.HEADER_FIELDS:
                sectionType = BodyFetchElement.HEADER_FIELDS;
                break;
            case FetchPartPathDecoder.HEADER_NOT_FIELDS:
                sectionType = BodyFetchElement.HEADER_NOT_FIELDS;
                break;    
            case FetchPartPathDecoder.MIME:
                sectionType = BodyFetchElement.MIME;
                break;
            case FetchPartPathDecoder.TEXT:
                sectionType = BodyFetchElement.TEXT;
                break;
            default:
                throw new ProtocolException("Section type is unsupported.");
        }
        return sectionType;
    }

    private String readWord(ImapRequestLineReader request, String terminator) throws ProtocolException {
        StringBuffer buf = new StringBuffer();
        char next = request.nextChar(); 
        while(terminator.indexOf(next)==-1) {
            buf.append(next);
            request.consume();
            next = request.nextChar();
        }
        return buf.toString();
    }

    private char nextNonSpaceChar( ImapRequestLineReader request )
    throws ProtocolException
    {
        char next = request.nextChar();
        while ( next == ' ' ) {
            request.consume();
            next = request.nextChar();
        }
        return next;
    }

    protected ImapMessage decode(ImapCommand command, ImapRequestLineReader request, 
            String tag, boolean useUids) throws ProtocolException {
        IdRange[] idSet = parseIdRange( request );
        FetchData fetch = fetchRequest( request );
        endLine( request );

        final Imap4Rev1MessageFactory factory = getMessageFactory();
        final ImapMessage result  = factory.createFetchMessage(command, useUids, idSet, fetch, tag);
        return result;
    }

}
