/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
package org.apache.james.transport;

import java.util.*;
import org.apache.arch.*;
import org.apache.james.transport.match.*;
/**
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Federico Barbieri <scoobie@systemy.it>
 */
public class MatchLoader implements Component, Composer, Contextualizable {

    private ComponentManager comp;
    private Context context;
    private Hashtable matchs;
    
    public MatchLoader() {
        matchs = new Hashtable();
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setComponentManager(ComponentManager comp) {
        this.comp = comp;
    }

    public Match getMatch(String matchName)
    throws Exception {
        Match res = (Match) matchs.get(matchName);
        if (res != null) return res;
        String className = "org.apache.james.transport.match." + matchName;
        res = (Match) Class.forName(className).newInstance();
        res.setContext(context);
        res.setComponentManager(comp);
        matchs.put(matchName, res);
        return res;
    }
}
