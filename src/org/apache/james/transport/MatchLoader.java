/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
package org.apache.james.transport;

import java.util.*;
import org.apache.avalon.*;
import org.apache.mail.*;
/**
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Federico Barbieri <scoobie@systemy.it>
 */
public class MatchLoader implements Component {

    public Matcher getMatch(String matchName, MailetContext context)
    throws Exception {
        String condition = (String) null;
        int i = matchName.indexOf('=');
        if (i != -1) {
            condition = matchName.substring(i + 1);
            matchName = matchName.substring(0, i);
        }
        String className = "org.apache.james.transport.matchers." + matchName;
        AbstractMatcher res = (AbstractMatcher) Class.forName(className).newInstance();
        res.setMailetContext(context);
        res.init(condition);
        return res;
    }
}
