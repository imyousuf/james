/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.util;

import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * <p>This interface is designed to provide a simplified subset of the
 * methods provided by the <code>java.text.DateFormat</code> class.</p>
 *
 * <p>This interface is necessary because of the difficulty in writing
 * thread safe classes that inherit from <code>java.text.DateFormat</code>.
 * This difficulty leads us to approach the problem using composition
 * rather than inheritance.  In general classes that implement this
 * interface will delegate these calls to an internal DateFormat object.</p>
 *    
 * @author Peter M. Goldstein <farsight@alum.mit.edu>
 */
public interface SimplifiedDateFormat {

    /**
     * Formats a Date into a date/time string.
     * @param date the time value to be formatted into a time string.
     * @return the formatted time string.
     */
    public String format(Date d);

    /**
     * Parses text from the beginning of the given string to produce a date.
     * The method may not use the entire text of the given string.
     *
     * @param source A <code>String</code> whose beginning should be parsed.
     * @return A <code>Date</code> parsed from the string.
     * @throws ParseException if the beginning of the specified string
     *         cannot be parsed.
     */
    public Date parse(String source) throws ParseException;

    /**
     * Sets the time zone of this SimplifiedDateFormat object.
     * @param zone the given new time zone.
     */
    public void setTimeZone(TimeZone zone);

    /**
     * Gets the time zone.
     * @return the time zone associated with this SimplifiedDateFormat.
     */
    public TimeZone getTimeZone();

    /**
     * Specify whether or not date/time parsing is to be lenient.  With
     * lenient parsing, the parser may use heuristics to interpret inputs that
     * do not precisely match this object's format.  With strict parsing,
     * inputs must match this object's format.
     * @param lenient when true, parsing is lenient
     * @see java.util.Calendar#setLenient
     */
    public void setLenient(boolean lenient);

    /**
     * Tell whether date/time parsing is to be lenient.
     * @return whether this SimplifiedDateFormat is lenient.
     */
    public boolean isLenient();
}

