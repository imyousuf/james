/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver;


import java.io.BufferedReader;
import java.io.IOException;
import java.util.StringTokenizer;

/**
 * TODO: Study up parser design techniques and rewrite this
 * (I don't think it warrants a generated parser.)
 *
 * Encapsulates a single client request to the Imap server, and the server
 * response.
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.1 $
 */
public class ImapRequestParser
{
    private BufferedReader reader;

    // TODO: implement this without string tokenizer. I need to learn more about parser design...
    private StringTokenizer tokens;
    private String peek = null;
    private static final char[] EMPTY_CHAR_ARRAY = new char[0];
    private static final char[] WILDCARD_CHARS = new char[]{'*', '%'};

    private StringBuffer buffer = new StringBuffer();

    ImapRequestParser( BufferedReader reader )
    {
        this.reader = reader;
    }


    /**
     * @return An argument of type "atom"
     */
    public String atom() throws ProtocolException
    {
        return extendedAtom( EMPTY_CHAR_ARRAY );
    }

    private String extendedAtom( char[] extraChars ) throws ProtocolException
    {
        // TODO implement this.
        return nextWord();
    }

    /**
     * @return The "tag" from the client request.
     */
    public String tag() throws ProtocolException
    {
        String atom = atom();
        if ( atom.indexOf( '+' ) != -1 ) {
            throw new ProtocolException( "Tag values must not contain the '+' character." );
        }
        return atom;
    }

    /**
     * @return An argument of type "astring"
     */
    public String astring() throws ProtocolException
    {
        // TODO: do this properly - need to check for disallowed characters
        // in unquoted strings.

        String token = peek();

        if ( token.charAt( 0 ) == '\"' ) {
            return quoted();
        }

        if ( token.charAt( 0 ) == '{' ) {
            return literal();
        }

        return atom();
    }

    /**
     * @return An argument of type "nstring"
     */
    public String nstring() throws ProtocolException
    {
        String token = peek();

        if ( "NIL".equals( token ) ) {
            // Consume the "NIL".
            token = nextWord();
            return null;
        }

        if ( token.charAt( 0 ) == '\"' ) {
            return quoted();
        }

        if ( token.charAt( 0 ) == '{' ) {
            return literal();
        }

        throw new ProtocolException( "Invalid <nstring> argument: expected NIL, <literal>, or <quoted>" );
    }

    public String quoted() throws ProtocolException
    {
        // TODO: Handle escaped quotes.
        String firstToken = nextWord();
        StringBuffer astring = new StringBuffer( firstToken );
        while ( astring.length() == 1 ||
                astring.charAt( astring.length() - 1 ) != '\"' ) {
            if ( tokens.hasMoreTokens() ) {
                astring.append( " " );
                astring.append( tokens.nextToken() );
            }
            else {
                throw new ProtocolException( "Missing closing quote on <astring> argument." );
            }
        }
        astring.deleteCharAt( 0 );
        astring.deleteCharAt( astring.length() - 1 );
        return astring.toString();
    }

    private String literal()
    {
        // TODO - implement this.
        return null;
    }

    /**
     * Reads an argument of type "list_mailbox" from the request, which is
     * the second argument for a LIST or LSUB command. Valid values are a "string"
     * argument, an "atom" with wildcard characters.
     * @return An argument of type "list_mailbox"
     */
    public String listMailbox() throws ProtocolException
    {
        String token = peek();
        if ( token.charAt( 0 ) == '"' ) {
            return quoted();
        }
        if ( token.charAt( 0 ) == '{' ) {
            return literal();
        }

        return extendedAtom( WILDCARD_CHARS );
    }

    private String peek() throws ProtocolException
    {
        if ( peek == null ) {
            peek = nextWord();
        }

        return peek;
    }

    private String nextWord() throws ProtocolException
    {
        if ( peek != null ) {
            String tmp = peek;
            peek = null;
            return tmp;
        }

        if ( tokens.hasMoreTokens() ) {
            return tokens.nextToken();
        }

        // Word not found.
        throw new ProtocolException( "Missing argument." );
    }

    private boolean readNextLine() throws ProtocolException
    {
        String nextLine = null;
        try {
            peek = null;
            nextLine = reader.readLine();
        }
        catch ( IOException e ) {
            throw new ProtocolException( "Unexpected protocol exception: " +
                                         e.getMessage() );
        }

        if ( nextLine == null ) {
            tokens = null;
            return false;
        }
        else {
            tokens = new StringTokenizer( nextLine );
            return true;
        }
    }

    /**
     * Read a CRLF from the request, finishing the line.
     */
    public void endLine() throws ProtocolException
    {
        if ( peek != null || tokens.hasMoreTokens() ) {
            peek = null;
            tokens = null;
            throw new ProtocolException( "Extra argument found." );
        }
        peek = null;
        tokens = null;
    }

    public boolean nextRequest()
    {
        if ( tokens != null && tokens.hasMoreTokens() ) {
            return true;
        }
        try {
            if ( readNextLine() ) {
                return true;
            }
            else {
                return false;
            }
        }
        catch ( ProtocolException e ) {
            return false;
        }
    }

    private boolean isCHAR( char chr )
    {
        return ( chr >= 0x01 && chr <= 0x7f );
    }

    private boolean isCHAR8( char chr )
    {
        return ( chr >= 0x01 && chr <= 0xff );
    }

    private boolean isATOM_CHAR( char chr )
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

    private boolean isListWildcard( char chr )
    {
        return ( chr == '*' || chr == '%' );
    }

    private boolean isQuotedSpecial( char chr )
    {
        return ( chr == '"' || chr == '\\' );
    }

}
