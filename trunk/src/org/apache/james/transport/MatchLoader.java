/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
package org.apache.james.transport;

import java.util.*;
import org.apache.mailet.*;
import org.apache.james.core.*;
import org.apache.avalon.*;

/**
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Federico Barbieri <scoobie@systemy.it>
 */
public class MatchLoader implements Component {

    public Matcher getMatcher(String matchName, MailetContext context)
    throws MailetException {
        try {
            String condition = (String) null;
            int i = matchName.indexOf('=');
            if (i != -1) {
                condition = matchName.substring(i + 1);
                matchName = matchName.substring(0, i);
            }
            MatcherConfigImpl configImpl = new MatcherConfigImpl();
            configImpl.setCondition(condition);
            configImpl.setMailetContext(context);
            //Have to make this package search list configurable - SK
            String className = "org.apache.james.transport.matchers." + matchName;
            Matcher matcher = (Matcher) Class.forName(className).newInstance();
            matcher.init(configImpl);
            return matcher;
        } catch (MailetException me) {
            throw me;
        } catch (Throwable t) {
            throw new MailetException("Error loading matcher", t);
        }
    }
}
