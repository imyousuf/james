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

package org.apache.james.imapserver.store;


/**
 * The set of flags associated with a message.
 * TODO - should store SEEN flag on a peruser basis (not required, but nice)
 * TODO - why not use javax.mail.Flags instead of having our own.
 *
 * <p>Reference: RFC 2060 - para 2.3
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.1 on 14 Dec 2000
 */
public class MessageFlags
{

    public static final int ANSWERED_INDEX = 0;
    public static final int DELETED_INDEX = 1;
    public static final int DRAFT_INDEX = 2;
    public static final int FLAGGED_INDEX = 3;
    public static final int RECENT_INDEX = 4;
    public static final int SEEN_INDEX = 5;

    // Array does not include seen flag
    private boolean[] flags = new boolean[6];


    public static final String ANSWERED = "\\ANSWERED";
    public static final String DELETED = "\\DELETED";
    public static final String DRAFT = "\\DRAFT";
    public static final String FLAGGED = "\\FLAGGED";
    public static final String SEEN = "\\SEEN";

    public MessageFlags()
    {
        resetAll();
    }

    private void resetAll()
    {
        setAll( false );
    }

    /**
     * Returns IMAP formatted String of MessageFlags for named user
     */
    public String format()
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "(" );
        if ( flags[ANSWERED_INDEX] ) {
            buf.append( "\\Answered " );
        }
        if ( flags[DELETED_INDEX] ) {
            buf.append( "\\Deleted " );
        }
        if ( flags[DRAFT_INDEX] ) {
            buf.append( "\\Draft " );
        }
        if ( flags[FLAGGED_INDEX] ) {
            buf.append( "\\Flagged " );
        }
        if ( flags[RECENT_INDEX] ) {
            buf.append( "\\Recent " );
        }
        if ( flags[SEEN_INDEX] ) {
            buf.append( "\\Seen " );
        }
        // Remove the trailing space, if necessary.
        if ( buf.length() > 1 )
        {
            buf.setLength( buf.length() - 1 );
        }
        buf.append( ")" );
        return buf.toString();
    }

    /**
     * Sets MessageFlags for message from IMAP-forammted string parameter.
     * <BR> The FLAGS<list> form overwrites existing flags, ie sets all other
     * flags to false.
     * <BR> The +FLAGS<list> form adds the flags in list to the existing flags
     * <BR> The -FLAGS<list> form removes the flags in list from the existing
     * flags
     * <BR> Note that the Recent flag cannot be set by user and is ignored by
     * this method.
     *
     * @param flagString a string formatted according to
     * RFC2060 store_att_flags
     * @return true if successful, false if not (including uninterpretable
     * argument)
     */
    public boolean setFlags( String flagString )
    {
        flagString = flagString.toUpperCase();

        boolean modValue;

        if ( flagString.startsWith( "FLAGS" ) ) {
            modValue = true;
            resetAll();
        }
        else if ( flagString.startsWith( "+FLAGS" ) ) {
            modValue = true;
        }
        else if ( flagString.startsWith( "-FLAGS" ) ) {
            modValue = false;
        }
        else {
            // Invalid flag string.
            return false;
        }

        if ( flagString.indexOf( ANSWERED ) != -1 ) {
            flags[ANSWERED_INDEX] = modValue;
        }
        if ( flagString.indexOf( DELETED ) != -1 ) {
            flags[DELETED_INDEX] = modValue;
        }
        if ( flagString.indexOf( DRAFT ) != -1 ) {
            flags[DRAFT_INDEX] = modValue;
        }
        if ( flagString.indexOf( FLAGGED ) != -1 ) {
            flags[FLAGGED_INDEX] = modValue;
        }
        if ( flagString.indexOf( SEEN ) != -1 ) {
            flags[SEEN_INDEX] = modValue;
        }
        return true;
    }

    public void setAnswered( boolean newState )
    {
        flags[ANSWERED_INDEX] = newState;
    }

    public boolean isAnswered()
    {
        return flags[ANSWERED_INDEX];
    }

    public void setDeleted( boolean newState )
    {
        flags[DELETED_INDEX] = newState;
    }

    public boolean isDeleted()
    {
        return flags[DELETED_INDEX];
    }

    public void setDraft( boolean newState )
    {
        flags[DRAFT_INDEX] = newState;
    }

    public boolean isDraft()
    {
        return flags[DRAFT_INDEX];
    }

    public void setFlagged( boolean newState )
    {
        flags[FLAGGED_INDEX] = newState;
    }

    public boolean isFlagged()
    {
        return flags[FLAGGED_INDEX];
    }

    public void setRecent( boolean newState )
    {
        flags[RECENT_INDEX] = newState;
    }

    public boolean isRecent()
    {
        return flags[RECENT_INDEX];
    }

    public void setSeen( boolean newState )
    {
        flags[SEEN_INDEX] = newState;
    }

    public boolean isSeen()
    {
        return flags[SEEN_INDEX];
    }

    public void setAll( boolean newState )
    {
        for ( int i = ANSWERED_INDEX; i <= SEEN_INDEX; i++ )
        {
            flags[i] = newState;
        }
    }

    public void setAll( MessageFlags newFlags ) {
        setAnswered( newFlags.isAnswered() );
        setDeleted( newFlags.isDeleted() );
        setDraft( newFlags.isDraft() );
        setFlagged( newFlags.isFlagged() );
        setSeen( newFlags.isSeen() );
    }

    public void addAll( MessageFlags toAdd ) {
        setFlagState( toAdd, true );
    }

    public void removeAll (MessageFlags toRemove) {
        setFlagState( toRemove, false );
    }

    private void setFlagState( MessageFlags changes, boolean setState )
    {
        if ( changes.isAnswered() ) {
            setAnswered( setState );
        }
        if ( changes.isDeleted() ) {
            setDeleted( setState );
        }
        if ( changes.isDraft() ) {
            setDraft( setState );
        }
        if ( changes.isFlagged() ) {
            setFlagged( setState );
        }
        if ( changes.isSeen() ) {
            setSeen( setState );
        }
    }
}

