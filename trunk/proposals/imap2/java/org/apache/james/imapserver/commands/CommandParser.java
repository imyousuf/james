/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver.commands;

import org.apache.james.imapserver.ProtocolException;
import org.apache.james.imapserver.ImapRequestLineReader;
import org.apache.james.imapserver.ImapConstants;
import org.apache.james.imapserver.store.MessageFlags;
import org.apache.james.util.Assert;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;

/**
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.2 $
 */
public class CommandParser
{
    private static final char[] EMPTY_CHAR_ARRAY = new char[0];

    /**
     * Reads an argument of type "atom" from the request.
     */
    public String atom( ImapRequestLineReader request ) throws ProtocolException
    {
        return consumeWord( request, new ATOM_CHARValidator() );
    }

    /**
     * Reads a command "tag" from the request.
     */
    public String tag(ImapRequestLineReader request) throws ProtocolException
    {
        CharacterValidator validator = new TagCharValidator();
        return consumeWord( request, validator );
    }

    /**
     * Reads an argument of type "astring" from the request.
     */
    public String astring(ImapRequestLineReader request) throws ProtocolException
    {
        char next = request.nextWordChar();
        switch ( next ) {
            case '"':
                return consumeQuoted( request );
            case '{':
                return consumeLiteral( request );
            default:
                return atom( request );
        }
    }

    /**
     * Reads an argument of type "nstring" from the request.
     */
    public String nstring( ImapRequestLineReader request ) throws ProtocolException
    {
        char next = request.nextWordChar();
        switch ( next ) {
            case '"':
                return consumeQuoted( request );
            case '{':
                return consumeLiteral( request );
            default:
                String value = atom( request );
                if ( "NIL".equals( value ) ) {
                    return null;
                }
                else {
                    throw new ProtocolException( "Invalid nstring value: valid values are '\"...\"', '{12} CRLF *CHAR8', and 'NIL'." );
                }
        }
    }

    /**
     * Reads a "mailbox" argument from the request. Not implemented *exactly* as per spec,
     * since a quoted or literal "inbox" still yeilds "INBOX"
     * (ie still case-insensitive if quoted or literal). I think this makes sense.
     *
     * mailbox         ::= "INBOX" / astring
     *              ;; INBOX is case-insensitive.  All case variants of
     *              ;; INBOX (e.g. "iNbOx") MUST be interpreted as INBOX
     *              ;; not as an astring.
     */
    public String mailbox( ImapRequestLineReader request ) throws ProtocolException
    {
        String mailbox = astring( request );
        if ( mailbox.equalsIgnoreCase( ImapConstants.INBOX_NAME ) ) {
            return ImapConstants.INBOX_NAME;
        }
        else {
            return mailbox;
        }
    }

    /**
     * Reads a "date-time" argument from the request.
     */
    public Date dateTime( ImapRequestLineReader request ) throws ProtocolException
    {
        char next = request.nextWordChar();
        String dateString;
        if ( next == '"' ) {
            dateString = consumeQuoted( request );
        }
        else {
            throw new ProtocolException( "DateTime values must be quoted." );
        }

        DateFormat dateFormat = new SimpleDateFormat( "dd-MMM-yyyy HH:mm:ss ZZ" );
        try {
            return dateFormat.parse( dateString );
        }
        catch ( ParseException e ) {
            throw new ProtocolException( "Invalid date format." );
        }
    }

    /**
     * Reads a "date" argument from the request.
     */
    public Date date( ImapRequestLineReader request ) throws ProtocolException
    {
        char next = request.nextWordChar();
        String dateString;
        if ( next == '"' ) {
            dateString = consumeQuoted( request );
        }
        else {
            dateString = atom( request );
        }

        DateFormat dateFormat = new SimpleDateFormat( "dd-MMM-yyyy" );
        try {
            return dateFormat.parse( dateString );
        }
        catch ( ParseException e ) {
            throw new ProtocolException( "Invalid date format." );
        }
    }

    /**
     * Reads the next "word from the request, comprising all characters up to the next SPACE.
     * Characters are tested by the supplied CharacterValidator, and an exception is thrown
     * if invalid characters are encountered.
     */
    protected String consumeWord( ImapRequestLineReader request,
                                  CharacterValidator validator )
            throws ProtocolException
    {
        StringBuffer atom = new StringBuffer();

        char next = request.nextWordChar();
        while( ! isWhitespace( next ) ) {
            if ( validator.isValid( next ) )
            {
                atom.append( next );
                request.consume();
            }
            else {
                throw new ProtocolException( "Invalid character: '" + next + "'" );
            }
            next = request.nextChar();
        }
        return atom.toString();
    }

    private boolean isWhitespace( char next )
    {
        return ( next == ' ' || next == '\n' || next == '\r' || next == '\t' );
    }

    /**
     * Reads an argument of type "literal" from the request, in the format:
     *      "{" charCount "}" CRLF *CHAR8
     */
    protected String consumeLiteral( ImapRequestLineReader request )
            throws ProtocolException
    {
        // The 1st character must be '{'
        consumeChar( request, '{' );

        StringBuffer digits = new StringBuffer();
        char next = request.nextChar();
        while ( next != '}' && next != '+' )
        {
            digits.append( next );
            request.consume();
            next = request.nextChar();
        }

        // If the number is *not* suffixed with a '+', we *are* using a synchronized literal,
        // and we need to send command continuation request before reading data.
        boolean synchronizedLiteral = true;
        // '+' indicates a non-synchronized literal (no command continuation request)
        if ( next == '+' ) {
            synchronizedLiteral = false;
            consumeChar(request, '+' );
        }

        // Consume the '}' and the newline
        consumeChar( request, '}' );
        consumeChar( request, '\n' );

        if ( synchronizedLiteral ) {
            request.commandContinuationRequest();
        }

        int size = Integer.parseInt( digits.toString() );
        char[] buffer = new char[size];
        request.read( buffer );

        return new String( buffer );
    }

    /**
     * Consumes the next character in the request, checking that it matches the
     * expected one. This method should be used when the
     */
    protected void consumeChar( ImapRequestLineReader request, char expected )
            throws ProtocolException
    {
        char consumed = request.consume();
        if ( consumed != expected ) {
            throw new ProtocolException( "Expected:'" + expected + "' found:'" + consumed + "'" );
        }
    }

    /**
     * Reads a quoted string value from the request.
     */
    protected String consumeQuoted( ImapRequestLineReader request )
            throws ProtocolException
    {
        // The 1st character must be '"'
        consumeChar(request, '"' );

        StringBuffer quoted = new StringBuffer();
        char next = request.nextChar();
        while( next != '"' ) {
            if ( next == '\\' ) {
                request.consume();
                next = request.nextChar();
                if ( ! isQuotedSpecial( next ) ) {
                    throw new ProtocolException( "Invalid escaped character in quote: '" +
                                                 next + "'" );
                }
            }
            quoted.append( next );
            request.consume();
            next = request.nextChar();
        }

        consumeChar( request, '"' );

        return quoted.toString();
    }

    /**
     * Reads a base64 argument from the request.
     */
    public byte[] base64( ImapRequestLineReader request ) throws ProtocolException
    {
        return null;
    }

    /**
     * Reads a "flags" argument from the request.
     */
    public MessageFlags flagList( ImapRequestLineReader request )
    {
        // TODO implement
        return null;
    }

    /**
     * Reads an argument of type "number" from the request.
     */
    public long number( ImapRequestLineReader request ) throws ProtocolException
    {
        String digits = consumeWord( request, new DigitCharValidator() );
        return Long.parseLong( digits );
    }

    /**
     * Reads an argument of type "nznumber" (a non-zero number)
     * (NOTE this isn't strictly as per the spec, since the spec disallows
     * numbers such as "0123" as nzNumbers (although it's ok as a "number".
     * I think the spec is a bit shonky.)
     */
    public long nzNumber( ImapRequestLineReader request ) throws ProtocolException
    {
        long number = number( request );
        if ( number == 0 ) {
            throw new ProtocolException( "Zero value not permitted." );
        }
        return number;
    }

    private boolean isCHAR( char chr )
    {
        return ( chr >= 0x01 && chr <= 0x7f );
    }

    private boolean isCHAR8( char chr )
    {
        return ( chr >= 0x01 && chr <= 0xff );
    }

    protected boolean isListWildcard( char chr )
    {
        return ( chr == '*' || chr == '%' );
    }

    private boolean isQuotedSpecial( char chr )
    {
        return ( chr == '"' || chr == '\\' );
    }

    /**
     * Consumes the request up to and including the eno-of-line.
     * @param request The request
     * @throws ProtocolException If characters are encountered before the endLine.
     */
    public void endLine( ImapRequestLineReader request ) throws ProtocolException
    {
        request.eol();
    }

    /**
     * Provides the ability to ensure characters are part of a permitted set.
     */
    protected interface CharacterValidator
    {
        /**
         * Validates the supplied character.
         * @param chr The character to validate.
         * @return <code>true</code> if chr is valid, <code>false</code> if not.
         */
        boolean isValid( char chr );
    }

    protected class NoopCharValidator implements CharacterValidator
    {
        public boolean isValid( char chr )
        {
            return true;
        }
    }

    protected class ATOM_CHARValidator implements CharacterValidator
    {
        public boolean isValid( char chr )
        {
            return ( isCHAR( chr ) && !isAtomSpecial( chr ) &&
                     !isListWildcard( chr ) && !isQuotedSpecial( chr ) );
        }

        private boolean isAtomSpecial( char chr )
        {
            return ( chr == '(' ||
                    chr == ')' ||
                    chr == '{' ||
                    chr == ' ' ||
                    chr == Character.CONTROL );
        }
    }

    protected class DigitCharValidator implements CharacterValidator
    {
        public boolean isValid( char chr )
        {
            return ( chr >= '0' && chr <= '9' );
        }
    }

    private class TagCharValidator extends ATOM_CHARValidator
    {
        public boolean isValid( char chr )
        {
            if ( chr == '+' ) return false;
            return super.isValid( chr );
        }
    }


}
