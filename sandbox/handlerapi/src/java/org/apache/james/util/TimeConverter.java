/***********************************************************************
 * Copyright (c) 2006 The Apache Software Foundation.                  *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.util;

import java.util.HashMap;

import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;

public class TimeConverter {

    private static HashMap multipliers = new HashMap(10);

    private static final String PATTERN_STRING = "\\s*([0-9]+)\\s*([a-z,A-Z]+)\\s*";

    private static Pattern PATTERN = null;

    static {
        // add allowed units and their respective multiplier
        multipliers.put("msec", new Integer(1));
        multipliers.put("msecs", new Integer(1));
        multipliers.put("sec", new Integer(1000));
        multipliers.put("secs", new Integer(1000));
        multipliers.put("minute", new Integer(1000 * 60));
        multipliers.put("minutes", new Integer(1000 * 60));
        multipliers.put("hour", new Integer(1000 * 60 * 60));
        multipliers.put("hours", new Integer(1000 * 60 * 60));
        multipliers.put("day", new Integer(1000 * 60 * 60 * 24));
        multipliers.put("days", new Integer(1000 * 60 * 60 * 24));

        try {
            Perl5Compiler compiler = new Perl5Compiler();
            PATTERN = compiler.compile(PATTERN_STRING,
                    Perl5Compiler.READ_ONLY_MASK);
        } catch (MalformedPatternException mpe) {
            // Will never happen cause its hardcoded
        }

    }

    // Get sure it can not be instanciated
    private TimeConverter(String rawString) {
    }

    /**
     * Helper method to get the milliseconds for the given amount and unit
     * 
     * @param amount
     *            The amount for use with the unit
     * @param unit
     *            The unit
     * @return The time in milliseconds
     * @throws NumberFormatException
     *             Get thrown if an illegal unit was used
     */
    public static long getMilliSeconds(long amount, String unit)
            throws NumberFormatException {
        Object multiplierObject = multipliers.get(unit);
        if (multiplierObject == null) {
            throw new NumberFormatException("Unknown unit: " + unit);
        }
        int multiplier = ((Integer) multiplierObject).intValue();
        return (amount * multiplier);
    }

    /**
     * Helper method to get the milliseconds for the given rawstring. Allowed
     * rawstrings must mach pattern: "\\s*([0-9]+)\\s*([a-z,A-Z]+)\\s*"
     * 
     * @param rawString
     *            The rawstring which we use to extract the amount and unit
     * @return The time in milliseconds
     * @throws NumberFormatException
     *             Get thrown if an illegal rawString was used
     */
    public static long getMilliSeconds(String rawString)
            throws NumberFormatException {
        Perl5Matcher matcher = new Perl5Matcher();

        try {
            Perl5Compiler compiler = new Perl5Compiler();
            PATTERN = compiler.compile(PATTERN_STRING,
                    Perl5Compiler.READ_ONLY_MASK);
        } catch (MalformedPatternException mpe) {
            // Will never happen
        }

        if (matcher.matches(rawString, PATTERN)) {
            MatchResult res = matcher.getMatch();

            if (res.group(1) != null && res.group(2) != null) {
                long time = Integer.parseInt(res.group(1).trim());
                String unit = res.group(2);
                return getMilliSeconds(time, unit);
            } else {

                // This should never Happen anyway throw an exception
                throw new NumberFormatException(
                        "The supplied String is not a supported format "
                                + rawString);
            }
        } else {
            // The rawString not match our pattern. So its not supported
            throw new NumberFormatException(
                    "The supplied String is not a supported format "
                            + rawString);
        }
    }

}
