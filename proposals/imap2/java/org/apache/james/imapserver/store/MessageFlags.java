/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver.store;


/**
 * The set of flags associated with a message.
 * TODO - should store SEEN flag on a peruser basis (not required, but nice)
 *
 * <p>Reference: RFC 2060 - para 2.3
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.1 on 14 Dec 2000
 */
public class MessageFlags
{

    public static final int ANSWERED = 0;
    public static final int DELETED = 1;
    public static final int DRAFT = 2;
    public static final int FLAGGED = 3;
    public static final int RECENT = 4;
    public static final int SEEN = 5;

    // Array does not include seen flag
    private boolean[] flags = new boolean[6];

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
        if ( flags[ANSWERED] ) {
            buf.append( "\\Answered " );
        }
        if ( flags[DELETED] ) {
            buf.append( "\\Deleted " );
        }
        if ( flags[DRAFT] ) {
            buf.append( "\\Draft " );
        }
        if ( flags[FLAGGED] ) {
            buf.append( "\\Flagged " );
        }
        if ( flags[RECENT] ) {
            buf.append( "\\Recent " );
        }
        if ( flags[SEEN] ) {
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

        if ( flagString.indexOf( "\\ANSWERED" ) != -1 ) {
            flags[ANSWERED] = modValue;
        }
        if ( flagString.indexOf( "\\DELETED" ) != -1 ) {
            flags[DELETED] = modValue;
        }
        if ( flagString.indexOf( "\\DRAFT" ) != -1 ) {
            flags[DRAFT] = modValue;
        }
        if ( flagString.indexOf( "\\FLAGGED" ) != -1 ) {
            flags[FLAGGED] = modValue;
        }
        if ( flagString.indexOf( "\\SEEN" ) != -1 ) {
            flags[SEEN] = modValue;
        }
        return true;
    }

    public void setAnswered( boolean newState )
    {
        flags[ANSWERED] = newState;
    }

    public boolean isAnswered()
    {
        return flags[ANSWERED];
    }

    public void setDeleted( boolean newState )
    {
        flags[DELETED] = newState;
    }

    public boolean isDeleted()
    {
        return flags[DELETED];
    }

    public void setDraft( boolean newState )
    {
        flags[DRAFT] = newState;
    }

    public boolean isDraft()
    {
        return flags[DRAFT];
    }

    public void setFlagged( boolean newState )
    {
        flags[FLAGGED] = newState;
    }

    public boolean isFlagged()
    {
        return flags[FLAGGED];
    }

    public void setRecent( boolean newState )
    {
        flags[RECENT] = newState;
    }

    public boolean isRecent()
    {
        return flags[RECENT];
    }

    public void setSeen( boolean newState )
    {
        flags[SEEN] = newState;
    }

    public boolean isSeen()
    {
        return flags[SEEN];
    }

    public void setAll( boolean newState )
    {
        for ( int i = ANSWERED; i <= SEEN; i++ )
        {
            flags[i] = newState;
        }
    }
}

